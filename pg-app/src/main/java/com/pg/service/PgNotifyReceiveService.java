package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.HqNotifyTarget;
import com.pg.repository.HqNotifyTargetRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgNotifyInboundRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 전사 PG 노티 수신 (NOTI 전산노티대상 URL 연동용).
 * <ul>
 *   <li><b>노티 연동 PG({@code integ_noti_yn=Y})</b>: 본사설정 API연동설정에서 연동용도가 노티인 결제대행사를 쓰는 가맹점만,
 *       노티미들웨어가 보내는 {@code MerchantCode}(MID) + {@code RouteNo}(루트)로 {@code tb_merchant_pg_binding} 에서 분기합니다.</li>
 *   <li><b>URL 결제(1:N)</b>: 연동용도가 <b>URL 결제만</b>인 PG({@code integ_url_pay_yn=Y} 단독)는 공통 MID이므로
 *       동일 MID로 바인딩이 여러 건이면 본문에 <b>업체코드(compId)</b> 또는 {@code icopayCompId=} 가 있어야 합니다.</li>
 *   <li>MID에 노티 연동 바인딩이 있으면 <b>항상 노티용 바인딩만</b>으로 MID+루트를 먼저 해석합니다.</li>
 * </ul>
 */
@Service
public class PgNotifyReceiveService {

    private static final Logger log = LoggerFactory.getLogger(PgNotifyReceiveService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** ChillPay Description 등에 부가하는 토큰 — 노티 본문 전체에서 재추출 */
    private static final Pattern ICOPAY_COMP_ID = Pattern.compile("icopayCompId=([A-Za-z0-9_.-]+)", Pattern.CASE_INSENSITIVE);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final PgNotifyInboundRepository inboundRepository;
    private final MerchantPgBindingRepository bindingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final PgNotifyIngressGuard notifyIngressGuard;
    private final ChillPayNotifyToTrnsctnService chillPayNotifyToTrnsctnService;
    private final HqNotifyTargetRepository hqNotifyTargetRepository;
    private final ChillPayService chillPayService;

    public PgNotifyReceiveService(HqNotifyEnvService hqNotifyEnvService,
                                PgNotifyInboundRepository inboundRepository,
                                MerchantPgBindingRepository bindingRepository,
                                OrgUnitRepository orgUnitRepository,
                                PgAgencyRepository pgAgencyRepository,
                                PgNotifyIngressGuard notifyIngressGuard,
                                ChillPayNotifyToTrnsctnService chillPayNotifyToTrnsctnService,
                                HqNotifyTargetRepository hqNotifyTargetRepository,
                                ChillPayService chillPayService) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.inboundRepository = inboundRepository;
        this.bindingRepository = bindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.notifyIngressGuard = notifyIngressGuard;
        this.chillPayNotifyToTrnsctnService = chillPayNotifyToTrnsctnService;
        this.hqNotifyTargetRepository = hqNotifyTargetRepository;
        this.chillPayService = chillPayService;
    }

    /**
     * @param notifyTargetCode 노티 URL 경로의 두 번째 세그먼트(cb…/rs… 등). 없으면 CALLBACK 로 간주합니다.
     */
    @Transactional
    public NotifyReceiveOutcome receiveAndRespond(String pathToken, String notifyTargetCode, String rawBody, String contentType, String clientIp, HttpServletRequest request) {
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
        String channelType = resolveNotifyChannelType(notifyTargetCode);
        in.setNotifyChannelType(channelType);
        in.setNotifyTargetCode(trimNotifyTargetCode(notifyTargetCode));

        resolveAndFillInbound(in, parsed);
        inboundRepository.save(in);
        try {
            chillPayNotifyToTrnsctnService.recordFromInbound(in, channelType);
        } catch (Exception e) {
            log.warn("노티→결제내역(pg_trnsctn) 후처리 실패 (수신 응답은 OK 유지): {}", e.getMessage());
        }
        String defaultOk = env.getNotifyOkResponse() != null ? env.getNotifyOkResponse() : "{\"result\":\"OK\"}";
        /* CALLBACK(cb)·RESULT(rs) 모두: 브라우저 GET/폼 POST 는 pay-result 로, JSON POST 는 서버 노티 OK JSON 유지 */
        if (("RESULT".equalsIgnoreCase(channelType) || "CALLBACK".equalsIgnoreCase(channelType))
                && request != null
                && shouldUsePayResultRedirect(request.getMethod(), body, contentType)) {
            String loc = buildPayResultRedirectUrl(request, in, body, contentType);
            if (loc != null && !loc.isBlank()) {
                log.info("pg-notify {} → pay-result redirect (targetCode={})",
                        channelType, trimNotifyTargetCode(notifyTargetCode));
                return NotifyReceiveOutcome.redirect(loc);
            }
        }
        return NotifyReceiveOutcome.json(defaultOk);
    }

    /**
     * CALLBACK·RESULT URL — 브라우저 GET·폼 POST 는 결제 결과 HTML 로 보냄. JSON 본문 POST 는 서버 노티로 간주해 JSON OK 유지.
     */
    private static boolean shouldUsePayResultRedirect(String httpMethod, String rawBody, String contentType) {
        String m = httpMethod != null ? httpMethod.trim().toUpperCase(Locale.ROOT) : "";
        if ("GET".equals(m)) {
            return true;
        }
        if (!"POST".equals(m) && !"PUT".equals(m)) {
            return false;
        }
        String ct = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        String b = rawBody != null ? rawBody.trim() : "";
        if (b.startsWith("{")) {
            return false;
        }
        if (ct.contains("application/x-www-form-urlencoded")) {
            return true;
        }
        return b.contains("=");
    }

    private String buildPayResultRedirectUrl(HttpServletRequest request, PgNotifyInbound in, String rawBody, String contentType) {
        String compId = firstNonBlank(
                in.getPayloadCompId() != null ? in.getPayloadCompId().trim() : null,
                in.getMerchantId() != null ? in.getMerchantId().trim() : null);
        String base = chillPayService.resolveUrlPayResultAbsolute(request, compId);
        if (base == null || base.isBlank()) {
            return null;
        }
        ResultPageParams p = extractChillPayLikeResultParams(rawBody, contentType);
        StringBuilder q = new StringBuilder();
        appendUrlQueryParam(q, "OrderNo", p.orderNo);
        appendUrlQueryParam(q, "TransactionId", p.transactionId);
        appendUrlQueryParam(q, "PaymentStatus", p.paymentStatus);
        if (q.length() == 0) {
            return base;
        }
        String sep = base.contains("?") ? "&" : "?";
        return base + sep + q;
    }

    private static void appendUrlQueryParam(StringBuilder q, String key, String val) {
        if (val == null || val.isBlank() || key == null || key.isBlank()) {
            return;
        }
        String v = val.trim();
        if (v.length() > 512) {
            v = v.substring(0, 512);
        }
        if (q.length() > 0) {
            q.append('&');
        }
        q.append(URLEncoder.encode(key.trim(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
    }

    private static final class ResultPageParams {
        String orderNo;
        String transactionId;
        String paymentStatus;
    }

    private ResultPageParams extractChillPayLikeResultParams(String raw, String contentType) {
        ResultPageParams r = new ResultPageParams();
        String body = raw != null ? raw.trim() : "";
        if (body.isEmpty()) {
            return r;
        }
        String ct = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        if (body.startsWith("{") || ct.contains("json")) {
            try {
                JsonNode root = MAPPER.readTree(body);
                if (root != null && root.isObject()) {
                    r.orderNo = textDeep(root, "OrderNo", "orderNo");
                    r.transactionId = textDeep(root, "TransactionId", "transactionId");
                    r.paymentStatus = firstNonBlank(
                            textDeep(root, "PaymentStatus", "paymentStatus", "Paymentstatus"),
                            textDeep(root, "Status", "status"));
                }
            } catch (Exception ignored) {
                /* ignore */
            }
            return r;
        }
        Map<String, String> fm = new LinkedHashMap<>();
        parseFormToLowerMap(body, fm);
        r.orderNo = mapGetLoose(fm, "orderno", "order_no");
        r.transactionId = mapGetLoose(fm, "transactionid", "transaction_id", "transid", "trans_id");
        r.paymentStatus = firstNonBlank(
                mapGetLoose(fm, "paymentstatus", "payment_status"),
                mapGetLoose(fm, "status"));
        return r;
    }

    private static void parseFormToLowerMap(String body, Map<String, String> map) {
        try {
            for (String pair : body.split("&")) {
                int i = pair.indexOf('=');
                if (i <= 0) {
                    continue;
                }
                String k = URLDecoder.decode(pair.substring(0, i).trim(), StandardCharsets.UTF_8)
                        .toLowerCase(Locale.ROOT);
                String v = URLDecoder.decode(pair.substring(i + 1).trim(), StandardCharsets.UTF_8);
                if (!v.isEmpty()) {
                    map.put(k, v);
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
    }

    private static String mapGetLoose(Map<String, String> m, String... keys) {
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String v = m.get(key.toLowerCase(Locale.ROOT));
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String textDeep(JsonNode root, String... names) {
        String t = text(root, names);
        if (t != null) {
            return t;
        }
        JsonNode d = root.get("data");
        if (d != null && d.isObject()) {
            return text(d, names);
        }
        return null;
    }

    private static String text(JsonNode n, String... names) {
        if (n == null || !n.isObject()) {
            return null;
        }
        for (String c : names) {
            JsonNode x = n.get(c);
            if (x != null && !x.isNull()) {
                if (x.isTextual()) {
                    String s = x.asText().trim();
                    if (!s.isEmpty()) {
                        return s;
                    }
                }
                if (x.isNumber()) {
                    return x.asText();
                }
                if (x.isBoolean()) {
                    return x.asBoolean() ? "true" : "false";
                }
            }
        }
        return null;
    }

    private static String trimNotifyTargetCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String t = code.trim();
        return t.length() > 64 ? t.substring(0, 64) : t;
    }

    /** 본사설정 노티 대상 URL의 경로 코드 → CALLBACK/RESULT (미등록 시 CALLBACK) */
    private String resolveNotifyChannelType(String targetCode) {
        if (targetCode == null || targetCode.isBlank()) {
            return "CALLBACK";
        }
        Optional<HqNotifyTarget> t = hqNotifyTargetRepository.findByTargetCode(targetCode.trim());
        if (t.isEmpty()) {
            return "CALLBACK";
        }
        String ct = t.get().getChannelType();
        if (ct == null || ct.isBlank()) {
            return "CALLBACK";
        }
        return ct.trim().toUpperCase(Locale.ROOT);
    }

    private void resolveAndFillInbound(PgNotifyInbound in, ParsedNotify p) {
        boolean hasComp = p.compId != null && !p.compId.trim().isEmpty();
        String compStore = hasComp ? (p.compId.trim().length() > 64 ? p.compId.trim().substring(0, 64) : p.compId.trim()) : null;

        // 1) 연동용도「노티」PG 바인딩만: MID(MerchantCode) + 루트(RouteNo)로 가맹점 분기 (노티미들웨어 표준)
        if (p.mid != null && !p.mid.isBlank()) {
            String m = p.mid.trim();
            List<MerchantPgBinding> sameMid = bindingRepository.findByMidOrderByOperationalYnDescIdAsc(m);
            List<MerchantPgBinding> notiForMid = filterNotiPurposeBindings(sameMid);
            if (!notiForMid.isEmpty()) {
                Optional<MerchantPgBinding> bNoti = resolveBindingFromList(notiForMid, p.rootNo);
                if (bNoti.isPresent()) {
                    if (hasComp) {
                        in.setPayloadCompId(compStore);
                    } else {
                        in.setPayloadCompId(null);
                    }
                    applyBindingResolved(in, bNoti.get());
                    return;
                }
                in.setProcessStatus("MERCHANT_UNRESOLVED");
                in.setErrorMessage("노티 연동 PG(integ_noti_yn=Y) 바인딩은 있으나 MID+루트(RouteNo)와 일치하는 행이 없습니다.");
                return;
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
        Optional<MerchantPgBinding> bindingOpt = resolveBindingFromList(sameMidOnly, p.rootNo);
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
        /* 노티 수신·적재는 감사 목적이므로 가맹점 프로필 use_yn 으로 차단하지 않음 (결제 API 게이트와 분리). */
        in.setProcessStatus("PARSED");
    }

    private void applyBindingResolved(PgNotifyInbound in, MerchantPgBinding b) {
        in.setOrgUnitId(b.getOrgUnitId());
        orgUnitRepository.findById(b.getOrgUnitId()).ifPresent(o -> in.setMerchantId(o.getCode()));
        in.setProcessStatus("PARSED");
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

    /** 본사설정 API연동설정에서 연동용도「노티」가 켜진 결제대행사에 매핑된 가맹점 바인딩만 */
    private List<MerchantPgBinding> filterNotiPurposeBindings(List<MerchantPgBinding> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .filter(b -> hasNotiIntegration(loadAgency(b.getPgCd())))
                .toList();
    }

    private static boolean hasNotiIntegration(PgAgency a) {
        return a != null && yn(a.getIntegNotiYn());
    }

    private static boolean yn(String v) {
        return v != null && "Y".equalsIgnoreCase(v.trim());
    }

    private Optional<MerchantPgBinding> resolveBindingFromList(List<MerchantPgBinding> list, String rootNo) {
        if (list == null || list.isEmpty()) {
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
