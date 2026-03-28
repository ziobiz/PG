package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgNotifyInbound;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgNotifyInboundRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * 전사 PG 노티 수신 (NOTI 전산노티대상 URL 연동용). MID + 루트번호로 가맹점 매핑.
 */
@Service
public class PgNotifyReceiveService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HqNotifyEnvService hqNotifyEnvService;
    private final PgNotifyInboundRepository inboundRepository;
    private final MerchantPgBindingRepository bindingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final PgNotifyIngressGuard notifyIngressGuard;

    public PgNotifyReceiveService(HqNotifyEnvService hqNotifyEnvService,
                                PgNotifyInboundRepository inboundRepository,
                                MerchantPgBindingRepository bindingRepository,
                                OrgUnitRepository orgUnitRepository,
                                OrgServiceUseService orgServiceUseService,
                                PgNotifyIngressGuard notifyIngressGuard) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.inboundRepository = inboundRepository;
        this.bindingRepository = bindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.notifyIngressGuard = notifyIngressGuard;
    }

    @Transactional
    public String receiveAndRespond(String pathToken, String rawBody, String contentType, String clientIp, HttpServletRequest request) {
        notifyIngressGuard.assertAllowed(clientIp, rawBody != null ? rawBody : "", request);
        HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
        if (!env.getIngressToken().equals(pathToken)) {
            throw new SecurityException("invalid notify token");
        }
        String body = rawBody != null ? rawBody : "";
        MidRoot parsed = parsePayload(body, contentType);
        PgNotifyInbound in = new PgNotifyInbound();
        in.setMid(parsed.mid);
        in.setRootNo(parsed.rootNo);
        in.setRawBody(body.length() > 500_000 ? body.substring(0, 500_000) + "...(truncated)" : body);
        in.setContentType(contentType);
        in.setClientIp(clientIp);

        Optional<MerchantPgBinding> bindingOpt = resolveBinding(parsed.mid, parsed.rootNo);
        if (bindingOpt.isPresent()) {
            MerchantPgBinding b = bindingOpt.get();
            in.setOrgUnitId(b.getOrgUnitId());
            Optional<OrgUnit> ou = orgUnitRepository.findById(b.getOrgUnitId());
            ou.ifPresent(o -> in.setMerchantId(o.getCode()));
            if (!orgServiceUseService.isOrgServiceActive(b.getOrgUnitId())) {
                in.setProcessStatus("MERCHANT_DISABLED");
                in.setErrorMessage("업체 미사용(서비스 중지) — 노티 미처리");
            } else {
                in.setProcessStatus("PARSED");
            }
        } else {
            in.setProcessStatus("MERCHANT_UNRESOLVED");
            in.setErrorMessage(parsed.mid == null || parsed.mid.isBlank() ? "mid missing" : "no binding for mid/root");
        }
        inboundRepository.save(in);
        return env.getNotifyOkResponse() != null ? env.getNotifyOkResponse() : "{\"result\":\"OK\"}";
    }

    private Optional<MerchantPgBinding> resolveBinding(String mid, String rootNo) {
        if (mid == null || mid.isBlank()) {
            return Optional.empty();
        }
        String m = mid.trim();
        List<MerchantPgBinding> list = bindingRepository.findByMidOrderByOperationalYnDescIdAsc(m);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        if (rootNo == null || rootNo.isBlank()) {
            return Optional.of(list.get(0));
        }
        String r = rootNo.trim();
        Optional<MerchantPgBinding> exact = list.stream()
                .filter(b -> b.getRootNo() != null && r.equals(b.getRootNo().trim()))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return list.stream()
                .filter(b -> b.getRootNo() == null || b.getRootNo().isBlank())
                .findFirst()
                .or(() -> Optional.of(list.get(0)));
    }

    private MidRoot parsePayload(String raw, String contentType) {
        MidRoot out = new MidRoot();
        String body = raw != null ? raw.trim() : "";
        if (body.isEmpty()) {
            return out;
        }
        String ct = contentType != null ? contentType.toLowerCase() : "";
        if (ct.contains("application/x-www-form-urlencoded")) {
            parseFormUrlEncoded(body, out);
        }
        if (out.mid == null && (ct.contains("json") || body.startsWith("{") || body.startsWith("["))) {
            parseJson(body, out);
        }
        if (out.mid == null && body.contains("=")) {
            parseFormUrlEncoded(body, out);
        }
        return out;
    }

    private void parseFormUrlEncoded(String body, MidRoot out) {
        try {
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                int i = pair.indexOf('=');
                if (i <= 0) continue;
                String k = URLDecoder.decode(pair.substring(0, i).trim(), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(i + 1).trim(), StandardCharsets.UTF_8);
                applyKeyValue(k, v, out);
            }
        } catch (Exception ignored) {
        }
    }

    private void parseJson(String body, MidRoot out) {
        try {
            JsonNode n = MAPPER.readTree(body);
            if (n.isObject()) {
                Iterator<String> it = n.fieldNames();
                while (it.hasNext()) {
                    String k = it.next();
                    JsonNode v = n.get(k);
                    if (v != null && !v.isNull()) {
                        applyKeyValue(k, v.isTextual() ? v.asText() : v.toString(), out);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void applyKeyValue(String key, String val, MidRoot out) {
        if (key == null || val == null) return;
        String k = key.trim();
        String v = val.trim();
        if (v.isEmpty()) return;
        switch (k.toLowerCase()) {
            case "mid":
            case "merchantid":
            case "merchant_code":
            case "merchantcode":
            case "mchtid":
                if (out.mid == null) out.mid = v;
                break;
            case "rootno":
            case "root_no":
            case "routeno":
            case "route_no":
            case "root":
                if (out.rootNo == null) out.rootNo = v;
                break;
            case "midroot":
            case "mid_root":
                if (!v.contains("_")) break;
                int u = v.lastIndexOf('_');
                if (out.mid == null) out.mid = v.substring(0, u).trim();
                if (out.rootNo == null) out.rootNo = v.substring(u + 1).trim();
                break;
            default:
                break;
        }
    }

    private static class MidRoot {
        String mid;
        String rootNo;
    }
}
