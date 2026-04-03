package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgNotifyInbound;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgNotifyInboundRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 전사 PG 노티 수신 (NOTI 전산노티대상 URL 연동용).
 * <ul>
 *   <li><b>노티 전용 연동</b>: 본문의 MID + 루트(옵션)으로 {@code tb_merchant_pg_binding} 매칭.</li>
 *   <li><b>URL 결제(1:N)</b>: API연동설정에서 연동용도가 <b>URL 결제만</b>인 PG({@code integ_url_pay_yn=Y} 단독)는 공통 MID이므로
 *       동일 MID로 바인딩이 여러 건이면 본문에 <b>업체코드(compId)</b> 또는 {@code icopayCompId=} 가 있어야 합니다.</li>
 *   <li>노티 본문에 업체코드가 있어도, 동일 MID가 전부 MID+루트 분기 가능한 PG(노티/API 등)이면 <b>MID+루트가 우선</b>합니다.</li>
 * </ul>
 */
@Service
public class PgNotifyReceiveService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** ChillPay Description 등에 부가하는 토큰 — 노티 본문 전체에서 재추출 */
    private static final Pattern ICOPAY_COMP_ID = Pattern.compile("icopayCompId=([A-Za-z0-9_.-]+)", Pattern.CASE_INSENSITIVE);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final PgNotifyInboundRepository inboundRepository;
    private final MerchantPgBindingRepository bindingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final PgNotifyIngressGuard notifyIngressGuard;

    public PgNotifyReceiveService(HqNotifyEnvService hqNotifyEnvService,
                                PgNotifyInboundRepository inboundRepository,
                                MerchantPgBindingRepository bindingRepository,
                                OrgUnitRepository orgUnitRepository,
                                PgAgencyRepository pgAgencyRepository,
                                OrgServiceUseService orgServiceUseService,
                                PgNotifyIngressGuard notifyIngressGuard) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.inboundRepository = inboundRepository;
        this.bindingRepository = bindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
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
        ParsedNotify parsed = parsePayload(body, contentType);
        PgNotifyInbound in = new PgNotifyInbound();
        in.setMid(parsed.mid);
        in.setRootNo(parsed.rootNo);
        in.setRawBody(body.length() > 500_000 ? body.substring(0, 500_000) + "...(truncated)" : body);
        in.setContentType(contentType);
        in.setClientIp(clientIp);

        resolveAndFillInbound(in, parsed);
        inboundRepository.save(in);
        return env.getNotifyOkResponse() != null ? env.getNotifyOkResponse() : "{\"result\":\"OK\"}";
    }

    private void resolveAndFillInbound(PgNotifyInbound in, ParsedNotify p) {
        boolean hasComp = p.compId != null && !p.compId.trim().isEmpty();
        String compStore = hasComp ? (p.compId.trim().length() > 64 ? p.compId.trim().substring(0, 64) : p.compId.trim()) : null;

        // 1) 노티/API 등: 동일 MID에 대해 전부 MID+루트 분기 가능한 PG이면 compId 없이(또는 있어도) MID+루트 우선
        if (p.mid != null && !p.mid.isBlank()) {
            String m = p.mid.trim();
            List<MerchantPgBinding> sameMid = bindingRepository.findByMidOrderByOperationalYnDescIdAsc(m);
            if (!sameMid.isEmpty()) {
                boolean allPreferMidRoot = sameMid.stream()
                        .allMatch(b -> prefersMidRootRouting(loadAgency(b.getPgCd())));
                if (allPreferMidRoot) {
                    Optional<MerchantPgBinding> bMid = resolveBinding(p.mid, p.rootNo);
                    if (bMid.isPresent()) {
                        if (hasComp) {
                            in.setPayloadCompId(compStore);
                        } else {
                            in.setPayloadCompId(null);
                        }
                        applyBindingResolved(in, bMid.get());
                        return;
                    }
                }
            }
        }

        // 2) URL 결제용 PG(공통 MID 1:N): 업체코드 필수 (동일 MID 다건일 때)
        if (hasComp) {
            in.setPayloadCompId(compStore);
            resolveUrlPayByCompId(in, p);
            return;
        }

        // 3) compId 없음 — MID+루트만
        in.setPayloadCompId(null);
        if (p.mid == null || p.mid.isBlank()) {
            in.setProcessStatus("MERCHANT_UNRESOLVED");
            in.setErrorMessage("mid missing");
            return;
        }
        List<MerchantPgBinding> sameMidOnly = bindingRepository.findByMidOrderByOperationalYnDescIdAsc(p.mid.trim());
        Optional<MerchantPgBinding> bindingOpt = resolveBinding(p.mid, p.rootNo);
        if (bindingOpt.isEmpty()) {
            in.setProcessStatus("MERCHANT_UNRESOLVED");
            in.setErrorMessage("no binding for mid/root");
            return;
        }
        MerchantPgBinding chosen = bindingOpt.get();
        boolean urlChannelOnThisMid = sameMidOnly.stream()
                .anyMatch(b -> hasUrlPayChannel(loadAgency(b.getPgCd())));
        if (urlChannelOnThisMid && sameMidOnly.size() > 1) {
            in.setProcessStatus("URL_PAY_NEEDS_COMP_ID");
            in.setErrorMessage("URL 결제용 PG(동일 MID 다가맹점) 노티에는 업체코드(compId) 또는 icopayCompId= 가 필요합니다.");
            return;
        }
        applyBindingResolved(in, chosen);
    }

    private void resolveUrlPayByCompId(PgNotifyInbound in, ParsedNotify p) {
        String compRaw = p.compId.trim();
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(compRaw);
        if (ouOpt.isEmpty()) {
            in.setProcessStatus("UNKNOWN_COMP");
            in.setErrorMessage("알 수 없는 업체코드: " + (compRaw.length() > 64 ? compRaw.substring(0, 64) : compRaw));
            return;
        }
        OrgUnit ou = ouOpt.get();
        List<MerchantPgBinding> binds = bindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ou.getId());
        if (binds.isEmpty()) {
            in.setProcessStatus("NO_PG_BINDING");
            in.setErrorMessage("해당 가맹점에 결제대행사 바인딩이 없습니다.");
            return;
        }
        if (p.mid != null && !p.mid.isBlank()) {
            String nm = p.mid.trim();
            boolean midMatch = binds.stream()
                    .anyMatch(b -> b.getMid() != null && nm.equalsIgnoreCase(b.getMid().trim()));
            if (!midMatch) {
                in.setProcessStatus("COMP_MID_MISMATCH");
                in.setErrorMessage("업체코드에 대한 가맹점 바인딩 MID와 노티 MID가 일치하지 않습니다.");
                return;
            }
            boolean hasUrlPayBinding = binds.stream()
                    .filter(b -> b.getMid() != null && nm.equalsIgnoreCase(b.getMid().trim()))
                    .anyMatch(b -> hasUrlPayChannel(loadAgency(b.getPgCd())));
            if (!hasUrlPayBinding) {
                in.setProcessStatus("COMP_NOT_URL_PAY_PG");
                in.setErrorMessage("업체코드 경로는 URL 결제(integ_url_pay) 결제대행사 바인딩이 있어야 합니다. 노티 전용 PG는 MID+루트로 수신하세요.");
                return;
            }
        } else {
            boolean anyUrl = binds.stream().anyMatch(b -> hasUrlPayChannel(loadAgency(b.getPgCd())));
            if (!anyUrl) {
                in.setProcessStatus("COMP_NOT_URL_PAY_PG");
                in.setErrorMessage("업체코드만으로는 URL 결제 PG 바인딩이 없습니다. 노티 MID를 포함하거나 MID+루트 경로를 사용하세요.");
                return;
            }
        }
        in.setOrgUnitId(ou.getId());
        in.setMerchantId(ou.getCode());
        if (!orgServiceUseService.isOrgServiceActive(ou.getId())) {
            in.setProcessStatus("MERCHANT_DISABLED");
            in.setErrorMessage("업체 미사용(서비스 중지) — 노티 미처리");
        } else {
            in.setProcessStatus("PARSED");
        }
    }

    private void applyBindingResolved(PgNotifyInbound in, MerchantPgBinding b) {
        in.setOrgUnitId(b.getOrgUnitId());
        orgUnitRepository.findById(b.getOrgUnitId()).ifPresent(o -> in.setMerchantId(o.getCode()));
        if (!orgServiceUseService.isOrgServiceActive(b.getOrgUnitId())) {
            in.setProcessStatus("MERCHANT_DISABLED");
            in.setErrorMessage("업체 미사용(서비스 중지) — 노티 미처리");
        } else {
            in.setProcessStatus("PARSED");
        }
    }

    private PgAgency loadAgency(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return null;
        }
        return pgAgencyRepository.findByPgCd(pgCd.trim()).orElse(null);
    }

    /** URL 결제 채널이 켜진 PG 행 (단독·복합 레거시 모두 integ_url_pay_yn 기준) */
    private static boolean hasUrlPayChannel(PgAgency a) {
        return a != null && yn(a.getIntegUrlPayYn());
    }

    /**
     * 공통 MID 1:N으로 compId 분기가 필요한지: URL 결제만 켜진 행(신규 권장 스키마).
     * 노티 등 다른 용도와 같이 켜진 레거시 행은 MID+루트 우선(preferMidRoot)로 처리.
     */
    private static boolean isExclusiveUrlPayAgency(PgAgency a) {
        if (a == null) {
            return false;
        }
        return yn(a.getIntegUrlPayYn()) && !yn(a.getIntegNotiYn()) && !yn(a.getIntegApiYn()) && !yn(a.getIntegWebChatbotYn());
    }

    /**
     * 노티 수신에서 동일 MID 묶음 전부가 MID+루트 분기에 적합한지.
     * 하나라도 “URL 결제만” 전용 행이면 공통 MID 1:N 가능 → false (compId 경로로 가야 함).
     */
    private static boolean prefersMidRootRouting(PgAgency a) {
        return !isExclusiveUrlPayAgency(a);
    }

    private static boolean yn(String v) {
        return v != null && "Y".equalsIgnoreCase(v.trim());
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

    private ParsedNotify parsePayload(String raw, String contentType) {
        ParsedNotify out = new ParsedNotify();
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
        if (out.compId == null || out.compId.isBlank()) {
            String extracted = extractIcopayCompIdFromRaw(body);
            if (extracted != null) {
                out.compId = extracted;
            }
        }
        return out;
    }

    private static String extractIcopayCompIdFromRaw(String body) {
        Matcher m = ICOPAY_COMP_ID.matcher(body);
        return m.find() ? m.group(1).trim() : null;
    }

    private void parseFormUrlEncoded(String body, ParsedNotify out) {
        try {
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                int i = pair.indexOf('=');
                if (i <= 0) {
                    continue;
                }
                String k = URLDecoder.decode(pair.substring(0, i).trim(), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(i + 1).trim(), StandardCharsets.UTF_8);
                applyKeyValue(k, v, out);
            }
        } catch (Exception ignored) {
        }
    }

    private void parseJson(String body, ParsedNotify out) {
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

    private void applyKeyValue(String key, String val, ParsedNotify out) {
        if (key == null || val == null) {
            return;
        }
        String k = key.trim();
        String v = val.trim();
        if (v.isEmpty()) {
            return;
        }
        switch (k.toLowerCase()) {
            case "mid":
            case "merchantid":
            case "merchant_code":
            case "merchantcode":
            case "mchtid":
                if (out.mid == null) {
                    out.mid = v;
                }
                break;
            case "rootno":
            case "root_no":
            case "routeno":
            case "route_no":
            case "root":
                if (out.rootNo == null) {
                    out.rootNo = v;
                }
                break;
            case "midroot":
            case "mid_root":
                if (!v.contains("_")) {
                    break;
                }
                int u = v.lastIndexOf('_');
                if (out.mid == null) {
                    out.mid = v.substring(0, u).trim();
                }
                if (out.rootNo == null) {
                    out.rootNo = v.substring(u + 1).trim();
                }
                break;
            case "compid":
            case "comp_id":
            case "comp_code":
            case "compcode":
            case "merchantcompcode":
            case "merchant_comp_code":
            case "orgcode":
            case "org_code":
            case "companycode":
            case "company_code":
            case "merchantorgcode":
                if (out.compId == null) {
                    out.compId = v;
                }
                break;
            default:
                break;
        }
    }

    private static class ParsedNotify {
        String mid;
        String rootNo;
        String compId;
    }
}
