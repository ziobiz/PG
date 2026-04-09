package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.dto.NotiMiddlewareRelayRequest;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.HqNotifyTarget;
import com.pg.entity.PgNotifyInbound;
import com.pg.integration.pg.notify.PgNotifyInboundTxnDispatcher;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.HqNotifyTargetRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgNotifyInboundRepository;
import com.pg.repository.PgTrnsctnRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 전사 PG 노티 수신 (NOTI 전산노티대상 URL 연동용).
 * <ul>
 *   <li><b>노티 연동 PG({@code integ_noti_yn=Y})</b>: 배포설정 &gt; API연동설정에서 연동용도가 노티인 결제대행사를 쓰는 가맹점만,
 *       노티미들웨어가 보내는 {@code MerchantCode}(MID) + {@code RouteNo}(루트)로 {@code tb_merchant_pg_binding} 에서 분기합니다.</li>
 *   <li><b>URL 결제(1:N)</b>: 연동용도가 <b>URL 결제만</b>인 PG({@code integ_url_pay_yn=Y} 단독)는 공통 MID이므로
 *       동일 MID로 바인딩이 여러 건이면 본문에 <b>업체코드(compId)</b> 또는 {@code icopayCompId=} 가 있어야 합니다.</li>
 *   <li>MID에 노티 연동 바인딩이 있으면 MID+루트로 노티 바인딩을 먼저 고르지만,
 *       본문 {@code icopayCompId=} 가 있고 그 업체가 노티 바인딩 업체와 다르면 URL 결제용 업체코드 해석을 우선합니다.</li>
 *   <li><b>동일 MID·동일 루트에 노티 연동 가맹점이 복수</b>이면 업체코드 없이는 임의(목록 순)로 한 곳만 잡히므로,
 *       {@code URL_PAY_NEEDS_COMP_ID} 로 거절하거나, 업체코드로 해당 조직의 노티 바인딩만 고릅니다.</li>
 *   <li><b>URL 결제 연동이 있는 공통 MID로 가맹점이 복수</b>이면 업체코드가 없는 노티는 원칙적으로 거절합니다.
 *       단, RESULT 채널에서 동일 {@code orderNo}의 기존 {@code origin=URL} 행으로 가맹점을 보강할 수 있으면 예외입니다.</li>
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
    private final PgNotifyInboundTxnDispatcher pgNotifyInboundTxnDispatcher;
    private final HqNotifyTargetRepository hqNotifyTargetRepository;
    private final ChillPayService chillPayService;
    private final PgTrnsctnRepository pgTrnsctnRepository;

    public PgNotifyReceiveService(HqNotifyEnvService hqNotifyEnvService,
                                PgNotifyInboundRepository inboundRepository,
                                MerchantPgBindingRepository bindingRepository,
                                OrgUnitRepository orgUnitRepository,
                                PgAgencyRepository pgAgencyRepository,
                                PgNotifyIngressGuard notifyIngressGuard,
                                PgNotifyInboundTxnDispatcher pgNotifyInboundTxnDispatcher,
                                HqNotifyTargetRepository hqNotifyTargetRepository,
                                ChillPayService chillPayService,
                                PgTrnsctnRepository pgTrnsctnRepository) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.inboundRepository = inboundRepository;
        this.bindingRepository = bindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.notifyIngressGuard = notifyIngressGuard;
        this.pgNotifyInboundTxnDispatcher = pgNotifyInboundTxnDispatcher;
        this.hqNotifyTargetRepository = hqNotifyTargetRepository;
        this.chillPayService = chillPayService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    /**
     * @param notifyTargetCode 노티 URL 경로의 두 번째 세그먼트(cb…/rs… 등). 없으면 CALLBACK 로 간주합니다.
     */
    @Transactional
    public NotifyReceiveOutcome receiveAndRespond(String pathToken, String notifyTargetCode, String rawBody, String contentType, String clientIp, HttpServletRequest request) {
        notifyIngressGuard.assertAllowed(clientIp, rawBody != null ? rawBody : "", request);
        return receiveAndRespondCore(pathToken, notifyTargetCode, rawBody, contentType, clientIp, request);
    }

    /**
     * 노티미들웨어에서 관리자 무효·취소 등만 처리하고 PG 로는 보내지 않았을 때,
     * 동일 HMAC·토큰 정책으로 이 메서드에 <strong>중계 JSON</strong>을 POST 하면 ChillPay 형 노티로 합성해 {@code pg_trnsctn} 에 반영합니다.
     * <p>요청 본문 HMAC 은 클라이언트가 보낸 원문(JSON) 기준입니다.
     */
    @Transactional
    public NotifyReceiveOutcome receiveNotiMiddlewareRelay(String pathToken, String notifyTargetCode,
                                                           String relayRequestRawJson,
                                                           NotiMiddlewareRelayRequest relay,
                                                           String clientIp, HttpServletRequest request) {
        notifyIngressGuard.assertAllowed(clientIp, relayRequestRawJson != null ? relayRequestRawJson : "", request);
        validateNotiMiddlewareRelay(relay);
        String synthetic = buildSyntheticChillPayJsonFromRelay(relay);
        log.info("노티미들웨어 중계 수신 → 합성 ChillPay 노티 적용 (txnId={}, event={})",
                relay.getTransactionId(), relay.getEventType());
        return receiveAndRespondCore(pathToken, notifyTargetCode, synthetic, MediaType.APPLICATION_JSON_VALUE, clientIp, request);
    }

    private static void validateNotiMiddlewareRelay(NotiMiddlewareRelayRequest r) {
        if (r == null) {
            throw new IllegalArgumentException("body required");
        }
        if (r.getTransactionId() == null || r.getTransactionId().isBlank()) {
            throw new IllegalArgumentException("transactionId required");
        }
        if (r.getMerchantCode() == null || r.getMerchantCode().isBlank()) {
            throw new IllegalArgumentException("merchantCode required");
        }
        boolean hasEv = r.getEventType() != null && !r.getEventType().isBlank();
        boolean hasInt = r.getInternalStatusCode() != null && !r.getInternalStatusCode().isBlank();
        if (!hasEv && !hasInt) {
            throw new IllegalArgumentException("eventType or internalStatusCode required");
        }
    }

    private static String buildSyntheticChillPayJsonFromRelay(NotiMiddlewareRelayRequest r) {
        String internal;
        if (r.getInternalStatusCode() != null && !r.getInternalStatusCode().isBlank()) {
            internal = r.getInternalStatusCode().trim();
        } else {
            String ev = r.getEventType() != null ? r.getEventType().trim().toUpperCase(Locale.ROOT) : "";
            internal = switch (ev) {
                case "VOID", "INVALID", "VOIDED" -> "21";
                case "CANCEL", "CANCELLED" -> "20";
                case "REFUND" -> "30";
                default -> throw new IllegalArgumentException("unsupported eventType: " + ev);
            };
        }
        String paymentStatusText = switch (internal) {
            case "21", "22", "40", "41", "42" -> "Voided";
            case "20" -> "Cancelled";
            case "30", "31" -> "Refunded";
            case "99", "F0", "f0" -> "Failed";
            default -> "Voided";
        };
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("MerchantCode", r.getMerchantCode().trim());
        m.put("TransactionId", r.getTransactionId().trim());
        if (r.getOrderNo() != null && !r.getOrderNo().isBlank()) {
            m.put("OrderNo", r.getOrderNo().trim());
        }
        if (r.getRouteNo() != null && !r.getRouteNo().isBlank()) {
            m.put("RouteNo", r.getRouteNo().trim());
        }
        m.put("PaymentStatus", paymentStatusText);
        m.put("Status", internal);
        StringBuilder desc = new StringBuilder();
        if (r.getCompId() != null && !r.getCompId().isBlank()) {
            desc.append("icopayCompId=").append(r.getCompId().trim());
        }
        if (r.getReason() != null && !r.getReason().isBlank()) {
            if (desc.length() > 0) {
                desc.append(' ');
            }
            desc.append("NOTI_MW_RELAY ").append(r.getReason().trim());
        } else {
            if (desc.length() > 0) {
                desc.append(' ');
            }
            desc.append("NOTI_MW_RELAY");
        }
        m.put("PaymentDescription", desc.toString());
        try {
            return MAPPER.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("relay json build failed", e);
        }
    }

    /**
     * 인입 검증(HMAC·IP)은 호출부에서 끝낸 뒤, 본문만 동일 파이프로 넣습니다.
     */
    private NotifyReceiveOutcome receiveAndRespondCore(String pathToken, String notifyTargetCode, String rawBody, String contentType, String clientIp, HttpServletRequest request) {
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
            pgNotifyInboundTxnDispatcher.dispatch(in, channelType);
        } catch (Exception e) {
            log.warn("노티→결제내역(pg_trnsctn) 후처리 실패 (수신 응답은 OK 유지): {}", e.getMessage());
        }
        String defaultOk = env.getNotifyOkResponse() != null ? env.getNotifyOkResponse() : "{\"result\":\"OK\"}";
        /* CALLBACK(cb)·RESULT(rs) 모두: 브라우저 GET/폼 POST 는 pay-result 로.
         * JSON POST 는 기본적으로 서버 노티(OK JSON)로 두되, RESULT 이고 본문이 결제결과형 JSON(주문·거래식별자 있음)이면
         * 피지 노티 서버의 가공 JSON 송부 후에도 브라우저가 결과 페이지로 이동하도록 리다이렉트한다. */
        if (("RESULT".equalsIgnoreCase(channelType) || "CALLBACK".equalsIgnoreCase(channelType))
                && request != null
                && shouldUsePayResultRedirect(channelType, request.getMethod(), body, contentType)) {
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
     * CALLBACK·RESULT URL — 브라우저 GET·폼 POST 는 결제 결과 HTML 로 보냄.
     * JSON POST 는 CALLBACK 은 서버 노티(OK JSON) 유지, RESULT 는 결제결과형 JSON 이면 리다이렉트 허용.
     */
    private static boolean shouldUsePayResultRedirect(String notifyChannelType, String httpMethod, String rawBody, String contentType) {
        String ch = notifyChannelType != null ? notifyChannelType.trim().toUpperCase(Locale.ROOT) : "";
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
            return "RESULT".equals(ch) && jsonBodyLooksLikePaymentResultForResultRedirect(b);
        }
        if (ct.contains("application/x-www-form-urlencoded")) {
            return true;
        }
        return b.contains("=");
    }

    /**
     * RESULT 로 JSON 이 올 때 ChillPay/가공 노티 형태인지(최상위 또는 data.* 에 주문·거래 식별자).
     */
    private static boolean jsonBodyLooksLikePaymentResultForResultRedirect(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return false;
        }
        String b = rawBody.trim();
        if (!b.startsWith("{")) {
            return false;
        }
        try {
            JsonNode root = MAPPER.readTree(b);
            if (root == null || !root.isObject()) {
                return false;
            }
            String on = textDeep(root, "OrderNo", "orderNo");
            String tid = textDeep(root, "TransactionId", "transactionId");
            return (on != null && !on.isBlank()) || (tid != null && !tid.isBlank());
        } catch (Exception e) {
            return false;
        }
    }

    private String buildPayResultRedirectUrl(HttpServletRequest request, PgNotifyInbound in, String rawBody, String contentType) {
        String compFromBody = extractIcopayCompIdForPayResultRedirect(rawBody, contentType);
        String compId = firstNonBlank(
                firstNonBlank(
                        in.getPayloadCompId() != null ? in.getPayloadCompId().trim() : null,
                        in.getMerchantId() != null ? in.getMerchantId().trim() : null),
                compFromBody);
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
        r.orderNo = mapGetLoose(fm, "orderno", "order_no", "orderid");
        r.transactionId = mapGetLoose(fm, "transactionid", "transaction_id", "transid", "trans_id", "transno", "trans_no");
        r.paymentStatus = firstNonBlank(
                mapGetLoose(fm, "paymentstatus", "payment_status"),
                mapGetLoose(fm, "status"));
        return r;
    }

    /**
     * 브라우저 리다이렉트용 결과 URL에 {@code m=} 을 붙이기 위해, 노티 본문에서 {@code icopayCompId} 를 재추출합니다.
     * (수신 해석 단계에서 merchant 만 틀어진 경우에도 복귀 페이지·결제 초기화 URL 이 깨지지 않게 함)
     */
    private String extractIcopayCompIdForPayResultRedirect(String rawBody, String contentType) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        String body = rawBody.trim();
        Matcher m0 = ICOPAY_COMP_ID.matcher(body);
        if (m0.find()) {
            return m0.group(1).trim();
        }
        String ct = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        if (body.startsWith("{") || ct.contains("json")) {
            try {
                JsonNode root = MAPPER.readTree(body);
                if (root != null && root.isObject()) {
                    String desc = textDeep(root, "PaymentDescription", "paymentDescription");
                    if (desc != null && !desc.isBlank()) {
                        Matcher m1 = ICOPAY_COMP_ID.matcher(desc);
                        if (m1.find()) {
                            return m1.group(1).trim();
                        }
                    }
                }
            } catch (Exception ignored) {
                /* ignore */
            }
        }
        return null;
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

        /* 0) 등록 업체코드 + 노티 MID 동시 수신 시, 해당 가맹점의 노티용 바인딩을 MID 전역 검색보다 우선한다. */
        if (hasComp && p.mid != null && !p.mid.isBlank()) {
            Optional<OrgUnit> ouByComp = orgUnitRepository.findByCode(compStore);
            if (ouByComp.isEmpty()) {
                ouByComp = orgUnitRepository.findByCodeIgnoreCase(compStore);
            }
            if (ouByComp.isPresent()) {
                String m = p.mid.trim();
                List<MerchantPgBinding> orgAll = bindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ouByComp.get().getId());
                List<MerchantPgBinding> midOnOrg = orgAll.stream()
                        .filter(b -> b.getMid() != null && m.equalsIgnoreCase(b.getMid().trim()))
                        .toList();
                List<MerchantPgBinding> notiOnOrgMid = filterNotiPurposeBindings(midOnOrg);
                if (!notiOnOrgMid.isEmpty()) {
                    Optional<MerchantPgBinding> chosen = resolveBindingFromList(notiOnOrgMid, p.rootNo);
                    if (chosen.isPresent()) {
                        in.setPayloadCompId(compStore);
                        applyBindingResolved(in, chosen.get());
                        return;
                    }
                }
            }
        }

        /* URL 결제용 공통 MID + 가맹점 복수: 업체코드 없으면 거부(잘못된 가맹 적재 방지). RESULT·기존 URL 주문번호 보강만 예외. */
        if (p.mid != null && !p.mid.isBlank()) {
            List<MerchantPgBinding> sameMidAll = bindingRepository.findByMidOrderByOperationalYnDescIdAsc(p.mid.trim());
            if (urlPaySharedMidRequiresMerchantCode(sameMidAll) && (!hasComp || compStore == null)) {
                if (tryResolveUrlPayResultFromPriorTxn(in, p)) {
                    return;
                }
                in.setPayloadCompId(null);
                in.setProcessStatus("URL_PAY_NEEDS_COMP_ID");
                in.setErrorMessage("URL 결제용 공통 MID 환경에서는 노티에 업체코드(compId, merchantCompCode 등) 또는 icopayCompId= 가 필요합니다.");
                return;
            }
        }

        // 1) 연동용도「노티」PG 바인딩만: MID(MerchantCode) + 루트(RouteNo)로 가맹점 분기 (노티미들웨어 표준)
        if (p.mid != null && !p.mid.isBlank()) {
            String m = p.mid.trim();
            List<MerchantPgBinding> sameMid = bindingRepository.findByMidOrderByOperationalYnDescIdAsc(m);
            List<MerchantPgBinding> notiForMid = filterNotiPurposeBindings(sameMid);
            if (!notiForMid.isEmpty()) {
                Optional<MerchantPgBinding> bNoti = resolveBindingFromList(notiForMid, p.rootNo);
                if (bNoti.isEmpty()) {
                    in.setProcessStatus("MERCHANT_UNRESOLVED");
                    in.setErrorMessage("노티 연동 PG(integ_noti_yn=Y) 바인딩은 있으나 MID+루트(RouteNo)와 일치하는 행이 없습니다.");
                    return;
                }
                if (isAmbiguousNotiMidRoot(notiForMid, p.rootNo)) {
                    if (!hasComp || compStore == null) {
                        in.setPayloadCompId(null);
                        in.setProcessStatus("URL_PAY_NEEDS_COMP_ID");
                        in.setErrorMessage("동일 MID·루트로 노티 연동된 가맹점이 여러 곳입니다. 노티 본문에 업체코드(compId, merchantCompCode 등) 또는 icopayCompId= 를 넣어 주세요.");
                        return;
                    }
                    Optional<OrgUnit> ouComp = orgUnitRepository.findByCode(compStore);
                    if (ouComp.isEmpty()) {
                        ouComp = orgUnitRepository.findByCodeIgnoreCase(compStore);
                    }
                    if (ouComp.isEmpty()) {
                        in.setProcessStatus("UNKNOWN_COMP");
                        in.setErrorMessage("알 수 없는 업체코드: " + (compStore.length() > 64 ? compStore.substring(0, 64) : compStore));
                        return;
                    }
                    Optional<MerchantPgBinding> bForComp = findNotiBindingForOrgAndRoot(
                            notiForMid, p.rootNo, ouComp.get().getId());
                    if (bForComp.isPresent()) {
                        in.setPayloadCompId(compStore);
                        applyBindingResolved(in, bForComp.get());
                        return;
                    }
                    in.setPayloadCompId(compStore);
                    resolveUrlPayByCompId(in, p);
                    if ("PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
                        log.info("복수 노티 바인딩 환경에서 업체코드 {} 로 URL결제 경로 해석", compStore);
                        return;
                    }
                    return;
                }
                if (hasComp && compStore != null) {
                    Optional<OrgUnit> ouComp = orgUnitRepository.findByCode(compStore);
                    if (ouComp.isEmpty()) {
                        ouComp = orgUnitRepository.findByCodeIgnoreCase(compStore);
                    }
                    if (ouComp.isPresent()
                            && !Objects.equals(ouComp.get().getId(), bNoti.get().getOrgUnitId())) {
                        in.setPayloadCompId(compStore);
                        resolveUrlPayByCompId(in, p);
                        if ("PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
                            log.info("노티 MID·노티바인딩과 icopayCompId={} 불일치 → URL결제 업체코드 해석 사용", compStore);
                            return;
                        }
                        String failCode = in.getProcessStatus();
                        String failHint = in.getErrorMessage();
                        in.setProcessStatus(null);
                        in.setErrorMessage(null);
                        in.setOrgUnitId(null);
                        in.setMerchantId(null);
                        log.warn("icopayCompId={} URL결제 해석 실패({}/{}) — 노티 바인딩으로 폴백",
                                compStore, failCode, failHint);
                    }
                }
                if (hasComp) {
                    in.setPayloadCompId(compStore);
                } else {
                    in.setPayloadCompId(null);
                }
                applyBindingResolved(in, bNoti.get());
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
        if (tryResolveUrlPayResultFromPriorTxn(in, p)) {
            return;
        }
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

    /**
     * ChillPay URL 결제 RESULT URL이 {@code orderNo=…&transNo=…&respCode=…} 만 보내 MID가 없을 때,
     * 동일 주문으로 이미 적재된 URL DirectCredit 행({@code origin=URL})으로 가맹점·MID·루트를 보강합니다.
     * {@code raw_body} 저장 값은 변경하지 않습니다.
     */
    private boolean tryResolveUrlPayResultFromPriorTxn(PgNotifyInbound in, ParsedNotify p) {
        if (in == null || !"RESULT".equalsIgnoreCase(String.valueOf(in.getNotifyChannelType()).trim())) {
            return false;
        }
        String body = in.getRawBody() != null ? in.getRawBody() : "";
        if (body.isBlank() || body.trim().startsWith("{")) {
            return false;
        }
        String orderNo = firstNonBlank(p.orderNo, extractFormFieldLoose(body, "orderNo", "orderno", "order_no"));
        if (orderNo == null || orderNo.isBlank()) {
            return false;
        }
        String on = orderNo.trim();
        Optional<PgTrnsctn> txnOpt = pgTrnsctnRepository.findFirstByOrderNoAndOriginOrderByCreatedAtDesc(on, "URL");
        if (txnOpt.isEmpty()) {
            txnOpt = pgTrnsctnRepository.findFirstByOrderNoAndOriginOrderByCreatedAtDesc(on, "API");
        }
        if (txnOpt.isEmpty()) {
            return false;
        }
        PgTrnsctn t = txnOpt.get();
        String merchantCode = t.getMerchantId();
        if (merchantCode == null || merchantCode.isBlank()) {
            return false;
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(merchantCode.trim());
        if (ouOpt.isEmpty()) {
            return false;
        }
        OrgUnit ou = ouOpt.get();
        List<MerchantPgBinding> binds = bindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ou.getId());
        boolean anyUrlPay = binds.stream().anyMatch(b -> hasUrlPayChannel(loadAgency(b.getPgCd())));
        if (!anyUrlPay) {
            return false;
        }
        String route = firstNonBlank(p.rootNo, t.getRouteNo());
        Optional<MerchantPgBinding> chosen = Optional.empty();
        if (route != null && !route.isBlank()) {
            String r = route.trim();
            chosen = binds.stream()
                    .filter(b -> hasUrlPayChannel(loadAgency(b.getPgCd())))
                    .filter(b -> b.getRootNo() != null && r.equals(b.getRootNo().trim()))
                    .findFirst();
            if (chosen.isEmpty()) {
                chosen = binds.stream()
                        .filter(b -> hasUrlPayChannel(loadAgency(b.getPgCd())))
                        .filter(b -> b.getRootNo() == null || b.getRootNo().isBlank())
                        .findFirst();
            }
        }
        if (chosen.isEmpty()) {
            chosen = binds.stream().filter(b -> hasUrlPayChannel(loadAgency(b.getPgCd()))).findFirst();
        }
        in.setPayloadCompId(merchantCode.trim());
        in.setOrgUnitId(ou.getId());
        in.setMerchantId(merchantCode.trim());
        chosen.ifPresent(b -> in.setMid(b.getMid()));
        if (in.getMid() == null || in.getMid().isBlank()) {
            binds.stream().filter(b -> hasUrlPayChannel(loadAgency(b.getPgCd()))).findFirst()
                    .ifPresent(b -> in.setMid(b.getMid()));
        }
        if (route != null && !route.isBlank()) {
            in.setRootNo(route.trim());
        } else if (t.getRouteNo() != null && !t.getRouteNo().isBlank()) {
            in.setRootNo(t.getRouteNo().trim());
        }
        in.setProcessStatus("PARSED");
        in.setErrorMessage(null);
        return true;
    }

    private static String extractFormFieldLoose(String body, String... names) {
        if (body == null || body.isBlank() || names == null) {
            return null;
        }
        Map<String, String> fm = new LinkedHashMap<>();
        parseFormToLowerMap(body, fm);
        for (String n : names) {
            if (n == null) {
                continue;
            }
            String v = fm.get(n.toLowerCase(Locale.ROOT).replace('-', '_'));
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return mapGetLoose(fm, names);
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

    /**
     * 동일 MID에 URL 결제 연동 바인딩이 하나라도 있고, 서로 다른 가맹점(org_unit_id) 바인딩이 2곳 이상인 경우.
     * 이 때는 노티에 업체코드가 없으면 분기 불가이므로 {@link #resolveAndFillInbound} 에서 거절한다.
     */
    private boolean urlPaySharedMidRequiresMerchantCode(List<MerchantPgBinding> sameMid) {
        if (sameMid == null || sameMid.isEmpty()) {
            return false;
        }
        boolean anyUrlPay = sameMid.stream().anyMatch(b -> hasUrlPayChannel(loadAgency(b.getPgCd())));
        if (!anyUrlPay) {
            return false;
        }
        return sameMid.stream().map(MerchantPgBinding::getOrgUnitId).distinct().count() > 1;
    }

    /** 배포설정 &gt; API연동설정에서 연동용도「노티」가 켜진 결제대행사에 매핑된 가맹점 바인딩만 */
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

    /**
     * 동일 MID의 노티 연동 바인딩 중 루트 규칙과 맞는 행들. 루트 미수신 시 전체(동일 MID 노티 바인딩).
     * {@link #resolveBindingFromList} 와 동일한 루트 매칭 규칙.
     */
    private static List<MerchantPgBinding> notiBindingsMatchingRoot(List<MerchantPgBinding> notiForMid, String rootNo) {
        if (notiForMid == null || notiForMid.isEmpty()) {
            return List.of();
        }
        if (rootNo == null || rootNo.isBlank()) {
            return notiForMid;
        }
        String r = rootNo.trim();
        List<MerchantPgBinding> exact = notiForMid.stream()
                .filter(b -> b.getRootNo() != null && r.equals(b.getRootNo().trim()))
                .toList();
        if (!exact.isEmpty()) {
            return exact;
        }
        return notiForMid.stream()
                .filter(b -> b.getRootNo() == null || b.getRootNo().isBlank())
                .toList();
    }

    /** 동일 MID·동일(또는 공통) 루트로 노티 연동된 서로 다른 가맹점(org)이 2곳 이상 */
    private static boolean isAmbiguousNotiMidRoot(List<MerchantPgBinding> notiForMid, String rootNo) {
        List<MerchantPgBinding> cand = notiBindingsMatchingRoot(notiForMid, rootNo);
        if (cand.size() <= 1) {
            return false;
        }
        return cand.stream().map(MerchantPgBinding::getOrgUnitId).distinct().count() > 1;
    }

    private static Optional<MerchantPgBinding> findNotiBindingForOrgAndRoot(List<MerchantPgBinding> notiForMid,
                                                                            String rootNo,
                                                                            Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        return notiBindingsMatchingRoot(notiForMid, rootNo).stream()
                .filter(b -> orgUnitId.equals(b.getOrgUnitId()))
                .findFirst();
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
            case "memberid":
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
            case "orderno":
            case "order_no":
            case "orderid":
                if (out.orderNo == null) {
                    out.orderNo = v;
                }
                break;
            case "transno":
            case "trans_no":
                if (out.transNo == null) {
                    out.transNo = v;
                }
                break;
            case "respcode":
            case "resp_code":
                if (out.respCode == null) {
                    out.respCode = v;
                }
                break;
            case "status":
                if (out.resultStatus == null) {
                    out.resultStatus = v;
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
        /** ChillPay RESULT URL 등 x-www-form-urlencoded */
        String orderNo;
        String transNo;
        String respCode;
        String resultStatus;
    }
}
