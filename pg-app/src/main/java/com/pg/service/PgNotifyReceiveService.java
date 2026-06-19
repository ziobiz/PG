package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.dto.NotiMiddlewareRelayRequest;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.HqNotifyTarget;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.MerchantProfile;
import com.pg.entity.PgNotifyInbound;
import com.pg.integration.pg.notify.PgNotifyInboundTxnDispatcher;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.HqNotifyTargetRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgNotifyInboundRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.NotifyIngressDeliveryKindResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
 *   <li><b>동일 MID·동일 루트에 노티 연동 가맹점이 복수</b>이면 원칙적으로 {@code AMBIGUOUS_NOTI_MID_ROOT} 로 거절하거나 업체코드로 좁힙니다.
 *       우선 <b>수신 URL의 {@code targetCode}(cb…/rs…)</b>가 각 총판 {@code tb_merchant_notify_url}(NOTIFY_1·2)에 실린 주소와
 *       일치하는 조직 트리에 속한 후보만 남겨 단일 바인딩을 확정합니다(동일 MID·루트를 쓰는 서로 다른 총판).
 *       그다음 보조로 노티 본문 통화와 가맹점 기준통화가 정확히 하나만 일치할 때 확정합니다.</li>
 *   <li><b>수신 URL 경로의 노티 대상코드</b>({@code tb_hq_notify_target})에 연결 총판({@code org_unit_id})이 있으면,
 *       MID·URL결제 분기는 <b>해당 총판 및 하위 조직</b> 바인딩으로만 한정합니다.
 *       {@code org_unit_id} 가 비어 있으면 동일 {@code targetCode} 가 총판 {@code tb_merchant_notify_url} 에 포함된 경우 그 조직으로 보강합니다.</li>
 *   <li>연결 총판 프로필에 기준통화가 있고 노티 본문에 통화가 있으면 불일치 시 {@code BOUND_CURRENCY_MISMATCH} 로 격리합니다.</li>
 *   <li><b>URL 결제 연동이 있는 공통 MID로 가맹점이 복수</b>이면 업체코드가 없는 노티는 원칙적으로 거절합니다.
 *       단, RESULT 채널에서 동일 {@code orderNo}의 기존 {@code origin=URL} 행으로 가맹점을 보강할 수 있으면 예외입니다.</li>
 *   <li><b>서버-투-서버(JSON/폼, 리다이렉트 미적용)</b>: {@code processStatus≠PARSED} 이면 HTTP 422 + {@code success:false, processed:false};
 *       {@code pg_trnsctn} 후처리 예외 시 503. 성공 시 200 + {@code success:true, processed:true, inboundId} 및 본사설정 {@code notifyOkResponse} 병합.</li>
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
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;

    public PgNotifyReceiveService(HqNotifyEnvService hqNotifyEnvService,
                                PgNotifyInboundRepository inboundRepository,
                                MerchantPgBindingRepository bindingRepository,
                                OrgUnitRepository orgUnitRepository,
                                PgAgencyRepository pgAgencyRepository,
                                PgNotifyIngressGuard notifyIngressGuard,
                                PgNotifyInboundTxnDispatcher pgNotifyInboundTxnDispatcher,
                                HqNotifyTargetRepository hqNotifyTargetRepository,
                                ChillPayService chillPayService,
                                PgTrnsctnRepository pgTrnsctnRepository,
                                MerchantProfileRepository merchantProfileRepository,
                                MerchantNotifyUrlRepository merchantNotifyUrlRepository) {
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
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
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
        in.setIngressDeliveryKind(NotifyIngressDeliveryKindResolver.resolve(request));

        if (rejectUnknownNotifyTargetIfProvided(in)) {
            inboundRepository.save(in);
            return NotifyReceiveOutcome.json(buildNotifyApiJsonFailure(in, in.getProcessStatus(), in.getErrorMessage(), true),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        resolveAndFillInbound(in, parsed, body, contentType);
        inboundRepository.save(in);
        boolean dispatchFailed = false;
        try {
            pgNotifyInboundTxnDispatcher.dispatch(in, channelType);
        } catch (Exception e) {
            dispatchFailed = true;
            log.warn("노티→결제내역(pg_trnsctn) 후처리 실패: {}", e.getMessage());
        }
        String defaultOk = env.getNotifyOkResponse() != null ? env.getNotifyOkResponse() : "{\"result\":\"OK\"}";
        /* CALLBACK(cb)·RESULT(rs): 브라우저 GET/폼 POST 는 pay-result 로.
         * application/json POST 는 결제대행사 서버 노티 → 200 JSON 만(리다이렉트 없음). */
        if (("RESULT".equalsIgnoreCase(channelType) || "CALLBACK".equalsIgnoreCase(channelType))
                && request != null
                && shouldUsePayResultRedirect(channelType, request.getMethod(), body, contentType, isLikelyBrowserClient(request))) {
            String loc = isJpayIngressTarget(notifyTargetCode)
                    ? buildJpayMerchantNotifyRedirectUrl(in, body, contentType)
                    : buildPayResultRedirectUrl(request, in, body, contentType);
            if ((loc == null || loc.isBlank()) && isJpayIngressTarget(notifyTargetCode)) {
                loc = buildPayResultRedirectUrl(request, in, body, contentType);
            }
            if (loc != null && !loc.isBlank()) {
                log.info("pg-notify {} → {} redirect (targetCode={})",
                        channelType,
                        isJpayIngressTarget(notifyTargetCode) ? "JPAY merchant notify" : "pay-result",
                        trimNotifyTargetCode(notifyTargetCode));
                return NotifyReceiveOutcome.redirect(loc);
            }
        }
        /* 서버-투-서버: 수신 로그는 저장됨. 파싱 실패·후처리 실패는 4xx/5xx + JSON 으로 노티미들웨어 재전송 판별 가능하게 함. */
        try {
            boolean inboundParsed = "PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim());
            if (!inboundParsed) {
                String code = in.getProcessStatus() != null && !in.getProcessStatus().isBlank()
                        ? in.getProcessStatus().trim() : "NOT_PARSED";
                String msg = in.getErrorMessage() != null ? in.getErrorMessage() : "notify not accepted";
                return NotifyReceiveOutcome.json(buildNotifyApiJsonFailure(in, code, msg, true),
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (dispatchFailed) {
                return NotifyReceiveOutcome.json(
                        buildNotifyApiJsonFailure(in, "PG_TRNSCTN_DISPATCH_FAILED",
                                "pg_trnsctn post-process failed", true),
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            return NotifyReceiveOutcome.json(buildNotifyApiJsonSuccess(in, defaultOk), HttpStatus.OK);
        } catch (Exception e) {
            log.warn("노티 API 응답 JSON 생성 실패: {}", e.getMessage());
            return NotifyReceiveOutcome.json(
                    "{\"success\":false,\"processed\":false,\"retryable\":true,\"errorCode\":\"RESPONSE_BUILD_ERROR\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static String buildNotifyApiJsonFailure(PgNotifyInbound in, String errorCode, String message, boolean retryable) {
        try {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("success", false);
            n.put("processed", false);
            n.put("retryable", retryable);
            if (in.getId() != null) {
                n.put("inboundId", in.getId());
            }
            n.put("errorCode", errorCode != null ? errorCode : "ERROR");
            n.put("message", message != null ? message : "");
            return MAPPER.writeValueAsString(n);
        } catch (Exception e) {
            return "{\"success\":false,\"processed\":false,\"retryable\":true,\"errorCode\":\"JSON_SERIALIZE_ERROR\"}";
        }
    }

    private static String buildNotifyApiJsonSuccess(PgNotifyInbound in, String defaultOk) throws Exception {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("success", true);
        n.put("processed", true);
        n.put("retryable", false);
        if (in.getId() != null) {
            n.put("inboundId", in.getId());
        }
        String d = defaultOk != null ? defaultOk.trim() : "";
        if (!d.isEmpty()) {
            if (d.startsWith("{")) {
                try {
                    JsonNode extra = MAPPER.readTree(d);
                    if (extra.isObject()) {
                        Iterator<String> it = extra.fieldNames();
                        while (it.hasNext()) {
                            String key = it.next();
                            n.set(key, extra.get(key));
                        }
                    } else {
                        n.put("legacyNotifyOk", d);
                    }
                } catch (Exception e) {
                    n.put("legacyNotifyOk", d);
                }
            } else {
                n.put("result", d);
            }
        }
        return MAPPER.writeValueAsString(n);
    }

    /**
     * CALLBACK·RESULT URL — 브라우저 GET·폼 POST 는 결제 결과 HTML 로 보냄.
     * {@code application/json} POST 는 결제대행사·NOTI 서버 노티로 간주하고 리다이렉트하지 않음(항상 200 JSON).
     * 그 외 RESULT + 비표준 JSON(CT 없음 등)만 레거시 브라우저 복귀용 리다이렉트 후보.
     */
    /**
     * @param browserClient 실제 브라우저(고객 결제창 복귀) 요청으로 보이면 true. NOTI·연동사 서버-투-서버
     *                      릴레이(axios 등)이면 false — 이때 form 본문이어도 pay-result 리다이렉트 대신
     *                      서버 노티(200/422 JSON)로 처리해 NOTI가 처리 결과·재전송을 판별할 수 있게 한다.
     */
    private static boolean shouldUsePayResultRedirect(String notifyChannelType, String httpMethod, String rawBody, String contentType, boolean browserClient) {
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
            if (ct.contains("application/json")) {
                return false;
            }
            return browserClient && "RESULT".equals(ch) && jsonBodyLooksLikePaymentResultForResultRedirect(b);
        }
        /* JPAY 원문(form) 등 — 실제 브라우저 복귀만 pay-result 로 보내고, NOTI 서버 릴레이는 노티로 처리 */
        if (ct.contains("application/x-www-form-urlencoded")) {
            return browserClient;
        }
        return browserClient && b.contains("=");
    }

    /**
     * 결제 후 고객 브라우저의 콜백/결과 복귀로 보이는지(User-Agent·Accept 기준).
     * NOTI·연동사의 서버-투-서버 릴레이(axios/curl 등)는 false.
     */
    private static boolean isLikelyBrowserClient(HttpServletRequest req) {
        if (req == null) {
            return false;
        }
        String ua = req.getHeader("User-Agent");
        if (ua != null) {
            String u = ua.toLowerCase(Locale.ROOT);
            if (u.contains("mozilla") || u.contains("webkit") || u.contains("gecko")
                    || u.contains("chrome") || u.contains("safari") || u.contains("firefox")
                    || u.contains("edge") || u.contains("opera") || u.contains("trident")) {
                return true;
            }
        }
        String accept = req.getHeader("Accept");
        return accept != null && accept.toLowerCase(Locale.ROOT).contains("text/html");
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

    private static boolean isJpayIngressTarget(String notifyTargetCode) {
        if (notifyTargetCode == null || notifyTargetCode.isBlank()) {
            return false;
        }
        String c = notifyTargetCode.trim().toLowerCase(Locale.ROOT);
        return "rsjpay".equals(c) || "cbjpay".equals(c);
    }

    /**
     * JPAY rsJpay/cbJpay 브라우저 복귀 — 가맹 {@code JPAY_CALLBACK}·{@code JPAY_NOTIFY}(노티미들웨어)로 리다이렉트.
     * ICOPAY ingress URL 자체로는 보내지 않음(루프 방지). 미설정 시 {@link #buildPayResultRedirectUrl} 폴백.
     */
    private String buildJpayMerchantNotifyRedirectUrl(PgNotifyInbound in, String rawBody, String contentType) {
        Long orgUnitId = in != null ? in.getOrgUnitId() : null;
        if (orgUnitId == null && in != null && in.getMerchantId() != null && !in.getMerchantId().isBlank()) {
            orgUnitId = orgUnitRepository.findByCode(in.getMerchantId().trim())
                    .or(() -> orgUnitRepository.findByCodeIgnoreCase(in.getMerchantId().trim()))
                    .map(OrgUnit::getId)
                    .orElse(null);
        }
        if (orgUnitId == null) {
            return null;
        }
        String base = resolveMerchantJpayRelayRedirectUrl(orgUnitId);
        if (base == null || base.isBlank()) {
            return null;
        }
        JpayRedirectParams p = extractJpaySyncCallbackParams(rawBody, contentType);
        StringBuilder q = new StringBuilder();
        appendUrlQueryParam(q, "paymentStatus", p.paymentStatus);
        appendUrlQueryParam(q, "orderID", p.orderId);
        appendUrlQueryParam(q, "orderId", p.orderId);
        appendUrlQueryParam(q, "OrderNo", p.orderId);
        appendUrlQueryParam(q, "transaction_id", p.transactionId);
        appendUrlQueryParam(q, "memberid", p.memberId);
        appendUrlQueryParam(q, "returncode", p.returnCode);
        if (q.length() == 0) {
            return base;
        }
        String sep = base.contains("?") ? "&" : "?";
        return base + sep + q;
    }

    /** 가맹 JPAY 수신통보 — Callback(3DS 복귀) 우선, 없으면 Notify URL */
    private String resolveMerchantJpayRelayRedirectUrl(Long orgUnitId) {
        for (String urlType : new String[]{
                MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK,
                MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY}) {
            Optional<MerchantNotifyUrl> row = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, urlType);
            if (row.isEmpty()) {
                continue;
            }
            MerchantNotifyUrl n = row.get();
            if (n.getUseYn() != null && !"Y".equalsIgnoreCase(n.getUseYn().trim())) {
                continue;
            }
            String u = n.getNotiUrl() != null ? n.getNotiUrl().trim() : "";
            if (u.isBlank() || looksLikeIcopayJpayIngressUrl(u)) {
                continue;
            }
            return u;
        }
        return null;
    }

    private static boolean looksLikeIcopayJpayIngressUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        boolean ingressPath = lower.contains("/api/open/pg-notify/")
                || lower.contains("/api/middleware/notify/v1/pg-notify/");
        boolean jpayTail = lower.contains("/rsjpay") || lower.contains("/cbjpay");
        return ingressPath && jpayTail;
    }

    private static final class JpayRedirectParams {
        String orderId;
        String transactionId;
        String memberId;
        String returnCode;
        String paymentStatus;
    }

    private JpayRedirectParams extractJpaySyncCallbackParams(String raw, String contentType) {
        JpayRedirectParams r = new JpayRedirectParams();
        String body = raw != null ? raw.trim() : "";
        if (body.isEmpty()) {
            return r;
        }
        Map<String, String> fm = new LinkedHashMap<>();
        String ct = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        if (body.startsWith("{") || ct.contains("json")) {
            try {
                JsonNode root = MAPPER.readTree(body);
                if (root != null && root.isObject()) {
                    r.orderId = textDeep(root, "orderID", "orderId", "orderid", "OrderNo", "orderNo");
                    r.transactionId = textDeep(root, "transaction_id", "transactionId", "TransactionId");
                    r.memberId = textDeep(root, "memberid", "memberId", "MemberId");
                    r.returnCode = textDeep(root, "returncode", "returnCode", "ReturnCode");
                    r.paymentStatus = textDeep(root, "paymentStatus", "PaymentStatus", "status");
                }
            } catch (Exception ignored) {
                /* ignore */
            }
        } else {
            parseFormToLowerMap(body, fm);
            r.orderId = mapGetLoose(fm, "orderid", "order_id", "orderno");
            r.transactionId = mapGetLoose(fm, "transaction_id", "transactionid");
            r.memberId = mapGetLoose(fm, "memberid", "member_id");
            r.returnCode = mapGetLoose(fm, "returncode", "return_code");
            r.paymentStatus = firstNonBlank(
                    mapGetLoose(fm, "paymentstatus", "payment_status"),
                    mapGetLoose(fm, "status"));
        }
        if (r.paymentStatus == null || r.paymentStatus.isBlank()) {
            r.paymentStatus = mapJpayReturnCodeToPaymentStatus(r.returnCode);
        }
        return r;
    }

    private static String mapJpayReturnCodeToPaymentStatus(String returnCode) {
        if (returnCode == null || returnCode.isBlank()) {
            return "";
        }
        String rc = returnCode.trim();
        if ("00".equals(rc)) {
            return "succeeded";
        }
        if ("2".equals(rc)) {
            return "failed";
        }
        return "processing";
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

    /**
     * 노티 URL 경로의 {@code tb_hq_notify_target} → 연결 총판 {@code org_unit_id}.
     * 본사 행에 org 가 비어 있으면, 총판이 복사해 둔 {@code tb_merchant_notify_url} 의 URL 문자열에 동일
     * {@code targetCode} 가 포함된 조직이 정확히 하나일 때 그 조직으로 보강합니다.
     */
    private Long resolveIngressBoundRootOrgId(String notifyTargetCode) {
        String code = trimNotifyTargetCode(notifyTargetCode);
        if (code == null || code.isBlank()) {
            return null;
        }
        Optional<HqNotifyTarget> tgt = hqNotifyTargetRepository.findByTargetCode(code.trim());
        if (tgt.isEmpty()) {
            return null;
        }
        Long oid = tgt.get().getOrgUnitId();
        if (oid != null) {
            return oid;
        }
        List<Long> fromNu = merchantNotifyUrlRepository.findDistinctOrgUnitIdsByNotiUrlContainingSegment(code.trim());
        if (fromNu.size() == 1) {
            log.info("노티 대상코드 {} 의 tb_hq_notify_target.org_unit_id 비어 있음 — tb_merchant_notify_url 로 총판 {} 보강",
                    code, fromNu.get(0));
            return fromNu.get(0);
        }
        if (fromNu.size() > 1) {
            log.warn("노티 대상코드 {} 가 여러 조직 noti_url 에 동시 포함 — ingress 총판 보강 불가 orgIds={}", code, fromNu);
        }
        return null;
    }

    /**
     * 노티 경로에 targetCode(cb/rs...)가 붙어 들어왔는데 DB에 없으면, 레거시 CALLBACK 로 폴백하면
     * 총판 스코프 분리가 풀려 MID+루트 충돌 시 오동작(또는 미적재)이 발생할 수 있습니다.
     * 따라서 명시된 targetCode가 미등록이면 즉시 격리합니다.
     *
     * @return true 이면 호출부에서 즉시 return
     */
    private boolean rejectUnknownNotifyTargetIfProvided(PgNotifyInbound in) {
        if (in == null) {
            return false;
        }
        String code = trimNotifyTargetCode(in.getNotifyTargetCode());
        if (code == null || code.isBlank()) {
            return false;
        }
        if (hqNotifyTargetRepository.findByTargetCode(code.trim()).isPresent()) {
            return false;
        }
        in.setProcessStatus("UNKNOWN_NOTIFY_TARGET");
        in.setErrorMessage("노티 수신 URL 경로 코드(" + code + ")가 등록돼 있지 않습니다. 총판별 CALLBACK/RESULT URL(cb…/rs…)을 정확히 사용하세요.");
        return true;
    }

    private static boolean ingressScopeActive(Set<Long> ingressScope) {
        return ingressScope != null && !ingressScope.isEmpty();
    }

    /**
     * 연결 총판 및 그 하위 조직(가맹점 트리)만 노티 MID 분기 대상이 됩니다.
     * {@code boundRootId} 가 null 이면 null 반환(필터 미적용·레거시 URL 등).
     */
    private Set<Long> buildIngressOrgScope(Long boundRootOrgId) {
        if (boundRootOrgId == null) {
            return null;
        }
        Set<Long> out = new HashSet<>();
        out.add(boundRootOrgId);
        List<OrgUnit> all = orgUnitRepository.findAll();
        Map<Long, List<OrgUnit>> byParent = all.stream()
                .filter(o -> o.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        collectDescendantOrgIdsRec(boundRootOrgId, byParent, out);
        return out;
    }

    private static void collectDescendantOrgIdsRec(Long parentId, Map<Long, List<OrgUnit>> byParent, Set<Long> out) {
        for (OrgUnit child : byParent.getOrDefault(parentId, List.of())) {
            out.add(child.getId());
            collectDescendantOrgIdsRec(child.getId(), byParent, out);
        }
    }

    private static List<MerchantPgBinding> applyIngressScope(List<MerchantPgBinding> bindings, Set<Long> ingressScope) {
        if (!ingressScopeActive(ingressScope) || bindings == null || bindings.isEmpty()) {
            return bindings;
        }
        return bindings.stream()
                .filter(b -> b.getOrgUnitId() != null && ingressScope.contains(b.getOrgUnitId()))
                .toList();
    }

    private void enrichCurrencyFromRaw(String rawBody, String contentType, ParsedNotify p) {
        if (p == null || rawBody == null || rawBody.isBlank()) {
            return;
        }
        if (p.currency != null && !p.currency.isBlank()) {
            return;
        }
        String body = rawBody.trim();
        try {
            if (body.startsWith("{")) {
                JsonNode root = MAPPER.readTree(body);
                String c = textDeep(root, "Currency", "currency", "CurrencyCode", "currencyCode");
                if (c != null && !c.isBlank()) {
                    p.currency = c.trim();
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 연결 총판 프로필의 기준통화와 노티 본문 통화가 둘 다 있으면 불일치 시 거부합니다.
     *
     * @return true 이면 호출부에서 즉시 return (격리 상태 설정됨)
     */
    private boolean rejectCurrencyMismatchIfNeeded(PgNotifyInbound in, ParsedNotify p, Long ingressRootOrgId) {
        if (ingressRootOrgId == null) {
            return false;
        }
        Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(ingressRootOrgId);
        String distCur = prof.map(MerchantProfile::getBaseCurrency).map(String::trim).filter(s -> !s.isEmpty()).orElse("");
        String payloadCur = p != null && p.currency != null ? p.currency.trim() : "";
        if (distCur.isEmpty() || payloadCur.isEmpty()) {
            return false;
        }
        if (currenciesEquivalent(distCur, payloadCur)) {
            return false;
        }
        in.setProcessStatus("BOUND_CURRENCY_MISMATCH");
        in.setErrorMessage("수신 노티 경로(연결 총판) 기준통화 " + distCur + " 과 노티 본문 통화 " + payloadCur + " 가 일치하지 않습니다.");
        log.warn("노티 통화 불일치 거부: targetCode={} distCur={} payloadCur={}",
                in.getNotifyTargetCode(), distCur, payloadCur);
        return true;
    }

    private static boolean currenciesEquivalent(String a, String b) {
        String u1 = normalizeCurrencyToken(a).toUpperCase(Locale.ROOT);
        String u2 = normalizeCurrencyToken(b).toUpperCase(Locale.ROOT);
        if (u1.isEmpty() || u2.isEmpty()) {
            return false;
        }
        if (u1.equals(u2)) {
            return true;
        }
        String x1 = expandIfThreeDigitNumericIso4217(u1);
        String x2 = expandIfThreeDigitNumericIso4217(u2);
        return x1.equalsIgnoreCase(x2);
    }

    private static String normalizeCurrencyToken(String c) {
        if (c == null) {
            return "";
        }
        return c.trim();
    }

    private static String expandIfThreeDigitNumericIso4217(String u) {
        if (u.length() != 3 || !u.chars().allMatch(Character::isDigit)) {
            return u;
        }
        return switch (u) {
            case "392" -> "JPY";
            case "410" -> "KRW";
            case "764" -> "THB";
            case "840" -> "USD";
            case "978" -> "EUR";
            default -> u;
        };
    }

    private void resolveAndFillInbound(PgNotifyInbound in, ParsedNotify p, String rawBody, String contentType) {
        enrichCurrencyFromRaw(rawBody, contentType, p);
        Long ingressRootOrgId = resolveIngressBoundRootOrgId(in.getNotifyTargetCode());
        Set<Long> ingressScope = buildIngressOrgScope(ingressRootOrgId);
        if (rejectCurrencyMismatchIfNeeded(in, p, ingressRootOrgId)) {
            return;
        }

        boolean hasComp = p.compId != null && !p.compId.trim().isEmpty();
        String compStore = hasComp ? (p.compId.trim().length() > 64 ? p.compId.trim().substring(0, 64) : p.compId.trim()) : null;

        /* 0) 등록 업체코드 + 노티 MID 동시 수신 시, 해당 가맹점의 노티용 바인딩을 MID 전역 검색보다 우선한다. */
        if (hasComp && p.mid != null && !p.mid.isBlank()) {
            Optional<OrgUnit> ouByComp = orgUnitRepository.findByCode(compStore);
            if (ouByComp.isEmpty()) {
                ouByComp = orgUnitRepository.findByCodeIgnoreCase(compStore);
            }
            if (ouByComp.isPresent()) {
                if (ingressScopeActive(ingressScope) && !ingressScope.contains(ouByComp.get().getId())) {
                    in.setProcessStatus("INGRESS_ORG_SCOPE_MISMATCH");
                    in.setErrorMessage("노티 수신 주소(연결 총판) 트리에 속하지 않는 업체코드입니다.");
                    return;
                }
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

        // 1) 연동용도「노티」PG 바인딩만: MID(MerchantCode) + 루트(RouteNo)로 가맹점 분기 (노티미들웨어 표준)
        /* URL 공통 MID 검사보다 먼저 수행 — 동일 MID에 URL 결제 가맹점이 섞여 있어도 노티 전용 바인딩이면 업체코드 없이 수신 가능해야 함. */
        if (p.mid != null && !p.mid.isBlank()) {
            String m = p.mid.trim();
            List<MerchantPgBinding> sameMid = bindingRepository.findByMidOrderByOperationalYnDescIdAsc(m);
            List<MerchantPgBinding> notiForMid = filterNotiPurposeBindings(applyIngressScope(sameMid, ingressScope));
            if (!notiForMid.isEmpty()) {
                Optional<MerchantPgBinding> bNoti = resolveBindingFromList(notiForMid, p.rootNo);
                if (bNoti.isEmpty()) {
                    in.setProcessStatus("MERCHANT_UNRESOLVED");
                    in.setErrorMessage("노티 연동 PG(integ_noti_yn=Y) 바인딩은 있으나 MID+루트(RouteNo)와 일치하는 행이 없습니다.");
                    return;
                }
                if (isAmbiguousNotiMidRoot(notiForMid, p.rootNo)) {
                    Optional<MerchantPgBinding> byPath = tryResolveAmbiguousNotiByNotifyUrlTarget(
                            notiForMid, p.rootNo, in.getNotifyTargetCode());
                    if (byPath.isPresent()) {
                        log.info("노티 MID+루트 복수 후보 → 수신 경로코드·총판 NOTIFY URL 로 단일 바인딩 확정 (targetCode={}, orgUnitId={})",
                                trimNotifyTargetCode(in.getNotifyTargetCode()), byPath.get().getOrgUnitId());
                        if (hasComp) {
                            in.setPayloadCompId(compStore);
                        } else {
                            in.setPayloadCompId(null);
                        }
                        applyBindingResolved(in, byPath.get());
                        return;
                    }
                    Optional<MerchantPgBinding> byCur = tryResolveAmbiguousNotiByPayloadCurrency(notiForMid, p.rootNo, p);
                    if (byCur.isPresent()) {
                        log.info("노티 MID+루트 복수 후보 → 본문 통화({})·가맹 기준통화 일치로 단일 바인딩 확정 (orgUnitId={})",
                                p.currency != null ? p.currency.trim() : "", byCur.get().getOrgUnitId());
                        if (hasComp) {
                            in.setPayloadCompId(compStore);
                        } else {
                            in.setPayloadCompId(null);
                        }
                        applyBindingResolved(in, byCur.get());
                        return;
                    }
                    if (!hasComp || compStore == null) {
                        in.setPayloadCompId(null);
                        in.setProcessStatus("AMBIGUOUS_NOTI_MID_ROOT");
                        String hint = ingressRootOrgId != null
                                ? "노티 본문에 업체코드(compId, merchantCompCode 등) 또는 icopayCompId= 를 넣어 주세요."
                                : "총판별 CALLBACK/RESULT URL(cb…/rs…)을 사용하거나, 노티 본문에 업체코드(compId, merchantCompCode 등) 또는 icopayCompId= 를 넣어 주세요.";
                        in.setErrorMessage("동일 MID·루트로 노티 연동된 가맹점이 여러 곳입니다. " + hint);
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
                    if (ingressScopeActive(ingressScope) && !ingressScope.contains(ouComp.get().getId())) {
                        in.setProcessStatus("INGRESS_ORG_SCOPE_MISMATCH");
                        in.setErrorMessage("노티 수신 주소(연결 총판) 트리에 속하지 않는 업체코드입니다.");
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
                    resolveUrlPayByCompId(in, p, ingressScope);
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
                    if (ouComp.isPresent() && ingressScopeActive(ingressScope) && !ingressScope.contains(ouComp.get().getId())) {
                        in.setProcessStatus("INGRESS_ORG_SCOPE_MISMATCH");
                        in.setErrorMessage("노티 수신 주소(연결 총판) 트리에 속하지 않는 업체코드입니다.");
                        return;
                    }
                    if (ouComp.isPresent()
                            && !Objects.equals(ouComp.get().getId(), bNoti.get().getOrgUnitId())) {
                        in.setPayloadCompId(compStore);
                        resolveUrlPayByCompId(in, p, ingressScope);
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
            if (ingressScopeActive(ingressScope)) {
                List<MerchantPgBinding> notiGlobal = filterNotiPurposeBindings(sameMid);
                if (!notiGlobal.isEmpty()) {
                    in.setProcessStatus("MERCHANT_UNRESOLVED");
                    in.setErrorMessage("동일 MID의 노티 연동 바인딩이 이 수신 주소(연결 총판) 트리 밖에만 있습니다.");
                    return;
                }
            }
        }

        /* URL 결제용 공통 MID + (URL 바인딩 기준) 가맹점 복수: 업체코드 없으면 거부. 노티 분기가 처리 못한 경우만. */
        if (p.mid != null && !p.mid.isBlank()) {
            List<MerchantPgBinding> sameMidAll = applyIngressScope(
                    bindingRepository.findByMidOrderByOperationalYnDescIdAsc(p.mid.trim()), ingressScope);
            if (urlPaySharedMidRequiresMerchantCode(sameMidAll) && (!hasComp || compStore == null)) {
                if (tryResolveUrlPayResultFromPriorTxn(in, p, ingressScope)) {
                    return;
                }
                in.setPayloadCompId(null);
                in.setProcessStatus("URL_PAY_NEEDS_COMP_ID");
                in.setErrorMessage("URL 결제용 공통 MID 환경에서는 노티에 업체코드(compId, merchantCompCode 등) 또는 icopayCompId= 가 필요합니다.");
                return;
            }
        }

        // 2) URL 결제용 PG(공통 MID 1:N): 업체코드 필수 (동일 MID 다건일 때)
        if (hasComp) {
            in.setPayloadCompId(compStore);
            resolveUrlPayByCompId(in, p, ingressScope);
            return;
        }

        // 3) compId 없음 — MID+루트만
        in.setPayloadCompId(null);
        if (tryResolveUrlPayResultFromPriorTxn(in, p, ingressScope)) {
            return;
        }
        if (p.mid == null || p.mid.isBlank()) {
            in.setProcessStatus("MERCHANT_UNRESOLVED");
            in.setErrorMessage("mid missing");
            return;
        }
        List<MerchantPgBinding> sameMidOnly = applyIngressScope(
                bindingRepository.findByMidOrderByOperationalYnDescIdAsc(p.mid.trim()), ingressScope);
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

    private void resolveUrlPayByCompId(PgNotifyInbound in, ParsedNotify p, Set<Long> ingressScope) {
        String compRaw = p.compId.trim();
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(compRaw);
        if (ouOpt.isEmpty()) {
            in.setProcessStatus("UNKNOWN_COMP");
            in.setErrorMessage("알 수 없는 업체코드: " + (compRaw.length() > 64 ? compRaw.substring(0, 64) : compRaw));
            return;
        }
        OrgUnit ou = ouOpt.get();
        if (ingressScopeActive(ingressScope) && !ingressScope.contains(ou.getId())) {
            in.setProcessStatus("INGRESS_ORG_SCOPE_MISMATCH");
            in.setErrorMessage("노티 수신 주소(연결 총판) 트리에 속하지 않는 업체코드입니다.");
            return;
        }
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
    private boolean tryResolveUrlPayResultFromPriorTxn(PgNotifyInbound in, ParsedNotify p, Set<Long> ingressScope) {
        if (in == null || !"RESULT".equalsIgnoreCase(String.valueOf(in.getNotifyChannelType()).trim())) {
            return false;
        }
        String body = in.getRawBody() != null ? in.getRawBody() : "";
        if (body.isBlank() || body.trim().startsWith("{")) {
            return false;
        }
        String orderNo = firstNonBlank(p.orderNo, extractFormFieldLoose(body, "orderNo", "orderno", "order_no", "orderid", "orderID"));
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
        if (ingressScopeActive(ingressScope) && !ingressScope.contains(ou.getId())) {
            return false;
        }
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
        Long orgId = b != null ? b.getOrgUnitId() : null;
        in.setOrgUnitId(orgId);
        if (orgId != null) {
            orgUnitRepository.findById(orgId).ifPresent(o -> in.setMerchantId(o.getCode()));
        }
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
     * 동일 MID에 URL 결제 연동 바인딩이 하나라도 있고, 그 URL 바인딩들이 서로 다른 가맹점(org_unit_id) 2곳 이상인 경우.
     * 노티 전용 바인딩만 있는 가맹점은 여기서 제외한다(동일 MID에 URL+노티가 섞여도 노티 수신이 막히지 않게).
     */
    private boolean urlPaySharedMidRequiresMerchantCode(List<MerchantPgBinding> sameMid) {
        if (sameMid == null || sameMid.isEmpty()) {
            return false;
        }
        List<MerchantPgBinding> urlBinds = sameMid.stream()
                .filter(b -> hasUrlPayChannel(loadAgency(b.getPgCd())))
                .toList();
        if (urlBinds.isEmpty()) {
            return false;
        }
        return urlBinds.stream().map(MerchantPgBinding::getOrgUnitId).distinct().count() > 1;
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

    /**
     * 동일 MID·동일 루트 후보가 복수일 때, 이번 요청의 노티 경로 코드(cb…/rs…)가 어느 총판의
     * {@code tb_merchant_notify_url}(NOTIFY_1·NOTIFY_2) URL 에 실려 있는지(상위 조직까지 탐색)로
     * 단일 가맹점 바인딩을 고릅니다.
     */
    private Optional<MerchantPgBinding> tryResolveAmbiguousNotiByNotifyUrlTarget(
            List<MerchantPgBinding> notiForMid, String rootNo, String notifyTargetCode) {
        String code = trimNotifyTargetCode(notifyTargetCode);
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        List<MerchantPgBinding> cand = notiBindingsMatchingRoot(notiForMid, rootNo);
        if (cand.size() < 2) {
            return Optional.empty();
        }
        String needle = "/" + code.trim();
        Map<Long, OrgUnit> byId = orgUnitRepository.findAll().stream()
                .collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        List<MerchantPgBinding> hits = new ArrayList<>();
        for (MerchantPgBinding b : cand) {
            if (b.getOrgUnitId() == null) {
                continue;
            }
            if (ancestorNotifyUrlContainsTargetSegment(byId, b.getOrgUnitId(), needle)) {
                hits.add(b);
            }
        }
        if (hits.size() == 1) {
            return Optional.of(hits.get(0));
        }
        return Optional.empty();
    }

    private boolean ancestorNotifyUrlContainsTargetSegment(Map<Long, OrgUnit> byId, Long startOrgId, String needle) {
        Long cur = startOrgId;
        Set<Long> guard = new HashSet<>();
        while (cur != null && !guard.contains(cur)) {
            guard.add(cur);
            if (notifyUrlRowContainsSegment(cur, needle)) {
                return true;
            }
            OrgUnit ou = byId.get(cur);
            cur = ou != null ? ou.getParentId() : null;
        }
        return false;
    }

    private boolean notifyUrlRowContainsSegment(Long orgUnitId, String needle) {
        for (String ut : List.of("NOTIFY_1", "NOTIFY_2")) {
            Optional<MerchantNotifyUrl> nu = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, ut);
            if (nu.isEmpty()) {
                continue;
            }
            String url = nu.get().getNotiUrl();
            if (url != null && url.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 동일 MID·동일 루트로 노티 연동 후보가 복수(서로 다른 가맹점 org)일 때,
     * 노티 본문 통화와 각 가맹점 {@link MerchantProfile#getBaseCurrency()} 가 <strong>정확히 하나</strong>만
     * {@link #currenciesEquivalent} 하면 그 바인딩을 반환합니다. (ChillPay {@code Currency}=764/392 등)
     */
    private Optional<MerchantPgBinding> tryResolveAmbiguousNotiByPayloadCurrency(
            List<MerchantPgBinding> notiForMid, String rootNo, ParsedNotify p) {
        if (p == null || p.currency == null || p.currency.isBlank()) {
            return Optional.empty();
        }
        List<MerchantPgBinding> cand = notiBindingsMatchingRoot(notiForMid, rootNo);
        if (cand.size() < 2) {
            return Optional.empty();
        }
        String payloadCur = p.currency.trim();
        List<MerchantPgBinding> matches = cand.stream()
                .filter(b -> b.getOrgUnitId() != null)
                .filter(b -> {
                    Optional<MerchantProfile> prof = merchantProfileRepository.findByOrgUnitId(b.getOrgUnitId());
                    String bc = prof.map(MerchantProfile::getBaseCurrency).map(String::trim).filter(s -> !s.isEmpty()).orElse("");
                    return !bc.isEmpty() && currenciesEquivalent(bc, payloadCur);
                })
                .toList();
        if (matches.size() == 1) {
            return Optional.of(matches.get(0));
        }
        return Optional.empty();
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
            case "currency":
            case "currencycode":
            case "curtype":
            case "cur_type":
            case "paymentcurrency":
                if (out.currency == null) {
                    out.currency = v;
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

    /**
     * 본사 「노티수령정보」에서 과거 수신 건을 다시 파싱·MID/가맹 분기한 뒤 {@code pg_trnsctn} 적재 파이프라인을 실행합니다.
     * 바인딩·노티대상·총판 통화 설정을 고친 뒤 결제내역에 반영할 때 사용합니다.
     *
     * @return 처리 후 {@code processStatus}, {@code merchantId}, dispatch 성공 여부 등
     */
    @Transactional
    public Map<String, Object> replayInboundProcessing(long inboundId) {
        PgNotifyInbound in = inboundRepository.findById(inboundId)
                .orElseThrow(() -> new IllegalArgumentException("수신 로그를 찾을 수 없습니다: " + inboundId));
        String body = in.getRawBody() != null ? in.getRawBody() : "";
        if (body.isBlank()) {
            throw new IllegalArgumentException("저장된 원문(raw_body)이 비어 있습니다.");
        }
        if (body.contains("...(truncated)")) {
            throw new IllegalArgumentException("원문이 길이 제한으로 잘린 건은 재처리할 수 없습니다.");
        }
        String contentType = in.getContentType() != null ? in.getContentType() : "";
        ParsedNotify parsed = parsePayload(body, contentType);
        in.setMid(parsed.mid);
        in.setRootNo(parsed.rootNo);
        in.setPayloadCompId(null);
        in.setOrgUnitId(null);
        in.setMerchantId(null);
        in.setProcessStatus("RECEIVED");
        in.setErrorMessage(null);
        resolveAndFillInbound(in, parsed, body, contentType);
        inboundRepository.save(in);
        String channelType = in.getNotifyChannelType() != null && !in.getNotifyChannelType().isBlank()
                ? in.getNotifyChannelType().trim()
                : resolveNotifyChannelType(in.getNotifyTargetCode());
        if (channelType == null || channelType.isBlank()) {
            channelType = "CALLBACK";
        }
        boolean dispatchFailed = false;
        String dispatchError = null;
        try {
            pgNotifyInboundTxnDispatcher.dispatch(in, channelType);
        } catch (Exception e) {
            dispatchFailed = true;
            dispatchError = e.getMessage();
            log.warn("노티 재처리 dispatch 실패 inboundId={}: {}", inboundId, e.getMessage());
        }
        inboundRepository.save(in);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("inboundId", in.getId());
        m.put("processStatus", in.getProcessStatus());
        m.put("errorMessage", in.getErrorMessage());
        m.put("merchantId", in.getMerchantId());
        m.put("orgUnitId", in.getOrgUnitId());
        m.put("mid", in.getMid());
        m.put("rootNo", in.getRootNo());
        m.put("dispatchFailed", dispatchFailed);
        m.put("dispatchError", dispatchError);
        return m;
    }

    private static class ParsedNotify {
        String mid;
        String rootNo;
        String compId;
        /** 노티 본문 통화(검증용). 폼·JSON 키 또는 JSON 루트/중첩에서 채움 */
        String currency;
        /** ChillPay RESULT URL 등 x-www-form-urlencoded */
        String orderNo;
        String transNo;
        String respCode;
        String resultStatus;
    }
}
