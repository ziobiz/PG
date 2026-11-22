package com.pg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PG 노티 또는 DirectCredit 승인 적재 후(커밋 이후) 가맹점 {@code tb_merchant_notify_url} 로 JSON POST.
 * <ul>
 *   <li>{@link MerchantNotifyUrl#URL_TYPE_JPAY_NOTIFY}/{@link MerchantNotifyUrl#URL_TYPE_JPAY_CALLBACK}
 *       — 업체 「수신통보」(기본·우선). 만료·실패·성공 공통.</li>
 *   <li>{@link #URL_TYPE_MIDDLEWARE} · {@link #URL_TYPE_BACKGROUND}/{@link #URL_TYPE_RESULT}
 *       — 「결제통보」. URL결제 예외·기존 등록 시 병행.</li>
 * </ul>
 * <p>통보 POST는 전용 스레드 풀에서 실행하고, 4xx·NOTI Result URL은 재시도하지 않아
 * Tomcat 요청 스레드 고갈을 막습니다.</p>
 */
@Service
public class MerchantOutboundNotifyService {

    private static final Logger log = LoggerFactory.getLogger(MerchantOutboundNotifyService.class);
    public static final String URL_TYPE_MIDDLEWARE = "MIDDLEWARE";
    public static final String URL_TYPE_BACKGROUND = "BACKGROUND";
    public static final String URL_TYPE_RESULT = "RESULT";
    private static final ObjectMapper OM = new ObjectMapper();
    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MS = { 0L, 250L, 900L };
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 8_000;

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final MerchantNotifyOutboundLogService merchantNotifyOutboundLogService;
    private final RestTemplate restTemplate;
    private final ExecutorService outboundExecutor;

    public MerchantOutboundNotifyService(PgTrnsctnRepository pgTrnsctnRepository,
                                         OrgUnitRepository orgUnitRepository,
                                         MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                         MerchantNotifyOutboundLogService merchantNotifyOutboundLogService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.merchantNotifyOutboundLogService = merchantNotifyOutboundLogService;
        this.restTemplate = buildRestTemplate();
        this.outboundExecutor = buildOutboundExecutor();
    }

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    private static ExecutorService buildOutboundExecutor() {
        AtomicInteger n = new AtomicInteger();
        return new ThreadPoolExecutor(
                2,
                8,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256),
                r -> {
                    Thread t = new Thread(r, "merchant-outbound-" + n.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    @PreDestroy
    void shutdownOutboundExecutor() {
        outboundExecutor.shutdownNow();
    }

    /**
     * 트랜잭션 커밋 뒤 전용 풀에서 한 번 실행 — HTTP 요청 스레드를 막지 않음. 실패는 로그만.
     */
    public void scheduleAfterTxnCommit(PgTrnsctn savedTxn, PgNotifyInbound inbound, String notifyChannel) {
        if (savedTxn == null || savedTxn.getTrnId() == null || savedTxn.getTrnId().isBlank()) {
            return;
        }
        if (!PgVendor.isChillPayVendorCode(savedTxn.getVan())
                && !PgVendor.isJpayFamily(savedTxn.getVan())
                && !PgVendor.isElementPayFamily(savedTxn.getVan())
                && !PgVendor.isIlkFamily(savedTxn.getVan())
                && !PgVendor.isOxFamily(savedTxn.getVan())
                && !PgVendor.isQxipayFamily(savedTxn.getVan())
                && !PgVendor.isOmspayFamily(savedTxn.getVan())
                && !PgVendor.isPaysoFamily(savedTxn.getVan())
                && !PgVendor.isKsnetFamily(savedTxn.getVan())
                && !PgVendor.isQbangFamily(savedTxn.getVan())) {
            return;
        }
        final String trnId = savedTxn.getTrnId().trim();
        Runnable job = () -> {
            try {
                deliverIfConfigured(trnId, inbound, notifyChannel);
            } catch (Exception e) {
                log.warn("가맹 통보 job 실패 trnId={}: {}", trnId, e.getMessage());
            }
        };
        Runnable enqueue = () -> {
            try {
                outboundExecutor.execute(job);
            } catch (Exception e) {
                log.warn("가맹 통보 enqueue 실패 trnId={}: {}", trnId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue.run();
                }
            });
        } else {
            enqueue.run();
        }
    }

    private void deliverIfConfigured(String trnId, PgNotifyInbound inbound, String notifyChannel) {
        Optional<PgTrnsctn> opt = pgTrnsctnRepository.findById(trnId);
        if (opt.isEmpty()) {
            return;
        }
        PgTrnsctn t = opt.get();
        String st = t.getStatus();
        if (!shouldSendForInternalStatus(st)) {
            return;
        }
        if (st != null && st.equals(t.getMwOutboundLastSentStatus())) {
            return;
        }
        String merchantCode = t.getMerchantId();
        if (merchantCode == null || merchantCode.isBlank()) {
            return;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantCode.trim());
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(merchantCode.trim());
        }
        if (ou.isEmpty()) {
            return;
        }
        Long orgUnitId = ou.get().getId();
        String merchantCodeTrim = merchantCode.trim();
        String notifyCh = notifyChannel;
        Optional<MerchantNotifyUrl> recvNotifyOpt = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(
                orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY);
        Optional<MerchantNotifyUrl> recvCallbackOpt = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(
                orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK);
        Optional<MerchantNotifyUrl> mwOpt = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, URL_TYPE_MIDDLEWARE);
        Optional<MerchantNotifyUrl> bgOpt = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, URL_TYPE_BACKGROUND);
        Optional<MerchantNotifyUrl> rsOpt = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, URL_TYPE_RESULT);

        Map<String, Object> basePayload = buildPayload(t, inbound, notifyChannel);

        boolean anySuccess = false;
        boolean anySoftFail = false;
        boolean anyTarget = false;
        boolean anyRecvConfigured = false;

        Optional<MerchantNotifyUrl> recvN = usable(recvNotifyOpt);
        if (recvN.isPresent()) {
            anyRecvConfigured = true;
            anyTarget = true;
            PostOutcome out = postOne(merchantCodeTrim, orgUnitId, t, notifyCh,
                    MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY, recvN.get(), basePayload, true);
            anySuccess |= out.success;
            anySoftFail |= out.softFail;
        }
        Optional<MerchantNotifyUrl> recvC = usable(recvCallbackOpt);
        if (recvC.isPresent()) {
            anyRecvConfigured = true;
            anyTarget = true;
            PostOutcome out = postOne(merchantCodeTrim, orgUnitId, t, notifyCh,
                    MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK, recvC.get(), basePayload, true);
            anySuccess |= out.success;
            anySoftFail |= out.softFail;
        }

        Optional<MerchantNotifyUrl> mw = usable(mwOpt);
        if (mw.isPresent()) {
            anyTarget = true;
            PostOutcome out = postOne(merchantCodeTrim, orgUnitId, t, notifyCh, URL_TYPE_MIDDLEWARE, mw.get(), basePayload, false);
            anySuccess |= out.success;
            anySoftFail |= out.softFail;
        }
        Optional<MerchantNotifyUrl> bg = usable(bgOpt);
        if (bg.isPresent()) {
            anyTarget = true;
            PostOutcome out = postOne(merchantCodeTrim, orgUnitId, t, notifyCh, URL_TYPE_BACKGROUND, bg.get(),
                    withMerchantNotifyTarget(basePayload, URL_TYPE_BACKGROUND), true);
            anySuccess |= out.success;
            anySoftFail |= out.softFail;
        }
        Optional<MerchantNotifyUrl> rs = usable(rsOpt);
        if (rs.isPresent()) {
            anyTarget = true;
            PostOutcome out = postOne(merchantCodeTrim, orgUnitId, t, notifyCh, URL_TYPE_RESULT, rs.get(),
                    withMerchantNotifyTarget(basePayload, URL_TYPE_RESULT), true);
            anySuccess |= out.success;
            anySoftFail |= out.softFail;
        }

        if (!anyRecvConfigured && mw.isEmpty() && bg.isEmpty() && rs.isEmpty()) {
            log.warn("가맹 통보 URL 없음(수신통보·결제통보) mid={} trnId={} orderNo={} channel={} — 업체관리 수신통보 URL 등록 필요",
                    merchantCodeTrim, trnId, t.getOrderNo(), notifyCh);
            return;
        }
        /* 성공 또는 영구 실패/스킵만 → 동일 상태 재폭주 방지. 5xx·타임아웃은 다음에 재시도 허용 */
        if (anySuccess || (anyTarget && !anySoftFail)) {
            markMwOutboundSent(trnId, st);
        }
    }

    private static Optional<MerchantNotifyUrl> usable(Optional<MerchantNotifyUrl> opt) {
        return opt.filter(r -> yn(r.getUseYn()) && r.getNotiUrl() != null && !r.getNotiUrl().isBlank());
    }

    private PostOutcome postOne(String merchantCodeTrim, Long orgUnitId, PgTrnsctn t, String notifyCh,
                                String urlType, MerchantNotifyUrl row, Map<String, Object> payload, boolean plainOrSigned) {
        String url = row.getNotiUrl() != null ? row.getNotiUrl().trim() : "";
        try {
            if (PgNotifyIngressPaths.isForbiddenMerchantOutboundUrl(url)) {
                String reason = PgNotifyIngressPaths.isNotiBrowserResultUrl(url)
                        ? "NOTI_BROWSER_RESULT_SKIPPED"
                        : "ICOPAY_INGRESS_LOOPBACK_SKIPPED";
                log.warn("{} 가맹 통보 스킵({}): {}", urlType, reason, url);
                PostOutcome skipped = PostOutcome.skipped(reason);
                recordOutboundLog(merchantCodeTrim, orgUnitId, t, notifyCh, urlType, url, "", skipped);
                return skipped;
            }
            String body = OM.writeValueAsString(payload);
            PostOutcome out = plainOrSigned
                    ? postPlainPayNotify(row, body)
                    : postWithRetries(url, body, row.getSignSecret());
            recordOutboundLog(merchantCodeTrim, orgUnitId, t, notifyCh, urlType, url, body, out);
            if (!out.success && !out.skipped) {
                log.warn("{} 가맹 통보 전송 실패{}: {}", urlType,
                        out.permanent ? " (영구·재시도 안 함)" : " (재시도 소진)", url);
            }
            return out;
        } catch (Exception e) {
            log.warn("{} 가맹 통보 직렬화 실패 trnId={}: {}", urlType, t.getTrnId(), e.getMessage());
            return PostOutcome.softFail(0, null, e.getMessage());
        }
    }

    private static Map<String, Object> withMerchantNotifyTarget(Map<String, Object> base, String merchantNotifyTarget) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(base);
        if (merchantNotifyTarget != null) {
            m.put("merchantNotifyTarget", merchantNotifyTarget);
        }
        return m;
    }

    /**
     * 업체등록 「URL Background · URL Result」。DB에 단일 행이라도 {@link MerchantNotifyUrl#getSignSecret()} 이 있으면 MIDDLEWARE와 동일 헤더로 서명.
     */
    private PostOutcome postPlainPayNotify(MerchantNotifyUrl row, String bodyJson) {
        String sig = row.getSignSecret();
        return postWithRetries(row.getNotiUrl().trim(), bodyJson, sig);
    }

    private void recordOutboundLog(String compId, Long orgUnitId, PgTrnsctn t, String notifyChannel,
                                   String urlType, String targetUrl, String payloadBody, PostOutcome outcome) {
        if (outcome == null) {
            return;
        }
        try {
            merchantNotifyOutboundLogService.append(
                    compId,
                    orgUnitId,
                    t.getTrnId(),
                    t.getOrderNo(),
                    urlType,
                    targetUrl,
                    notifyChannel,
                    payloadBody,
                    outcome.success,
                    outcome.attempts,
                    outcome.httpStatus,
                    outcome.errorMessage);
        } catch (Exception e) {
            log.warn("결제통보 전송 이력 저장 실패 trnId={} urlType={}: {}", t.getTrnId(), urlType, e.getMessage());
        }
    }

    private static final class PostOutcome {
        private final boolean success;
        private final boolean skipped;
        private final boolean permanent;
        private final boolean softFail;
        private final int attempts;
        private final Integer httpStatus;
        private final String errorMessage;

        private PostOutcome(boolean success, boolean skipped, boolean permanent, boolean softFail,
                            int attempts, Integer httpStatus, String errorMessage) {
            this.success = success;
            this.skipped = skipped;
            this.permanent = permanent;
            this.softFail = softFail;
            this.attempts = attempts;
            this.httpStatus = httpStatus;
            this.errorMessage = errorMessage;
        }

        static PostOutcome ok(int attempts, Integer httpStatus) {
            return new PostOutcome(true, false, false, false, attempts, httpStatus, null);
        }

        static PostOutcome skipped(String reason) {
            return new PostOutcome(true, true, true, false, 0, 200, reason);
        }

        static PostOutcome permanentFail(int attempts, Integer httpStatus, String err) {
            return new PostOutcome(false, false, true, false, attempts, httpStatus, err);
        }

        static PostOutcome softFail(int attempts, Integer httpStatus, String err) {
            return new PostOutcome(false, false, false, true, attempts, httpStatus, err);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markMwOutboundSent(String trnId, String statusSent) {
        pgTrnsctnRepository.findById(trnId).ifPresent(x -> {
            x.setMwOutboundLastSentStatus(statusSent != null ? statusSent.trim() : null);
            pgTrnsctnRepository.save(x);
        });
    }

    private static Map<String, Object> buildPayload(PgTrnsctn t, PgNotifyInbound inbound, String notifyChannel) {
        Map<String, Object> m = new LinkedHashMap<>();
        /* 통합 API 가맹 통보 — PG명·레거시 별칭 미포함 */
        m.put("event", "pg.payment.status");
        m.put("compId", t.getMerchantId());
        m.put("trnId", t.getTrnId());
        m.put("orderNo", t.getOrderNo());
        m.put("status", t.getStatus());
        m.put("paymentStatus", mapUnifiedPaymentStatus(t.getStatus()));
        if (t.getChillTransactionId() != null && !t.getChillTransactionId().isBlank()) {
            m.put("pgTxnId", t.getChillTransactionId().trim());
        }
        m.put("amount", t.getAmtKrw() != null ? t.getAmtKrw().stripTrailingZeros().toPlainString() : null);
        m.put("currency", t.getCurType());
        m.put("paidAt", t.getPaidAt() != null ? t.getPaidAt().toString() : null);
        /* ICOPAY 단독 실패(만료·복귀타임아웃 등) — 가맹 전산 맞춤용 */
        if (t.getOutcomeReasonCode() != null && !t.getOutcomeReasonCode().isBlank()) {
            m.put("outcomeReasonCode", t.getOutcomeReasonCode().trim());
        }
        if (t.getOutcomeReason() != null && !t.getOutcomeReason().isBlank()) {
            String or = t.getOutcomeReason().trim();
            m.put("outcomeReason", or.length() > 500 ? or.substring(0, 500) : or);
        }
        if (notifyChannel != null && !notifyChannel.isBlank()) {
            m.put("notifyChannel", notifyChannel.trim());
        }
        m.put("ts", System.currentTimeMillis());
        return m;
    }

    private static String mapUnifiedPaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return switch (status.trim()) {
            case "10", "00" -> "PAID";
            case "08" -> "PENDING";
            case "99", "F0" -> "FAILED";
            case "30", "42" -> "REFUNDED";
            default -> status.trim();
        };
    }

    private PostOutcome postWithRetries(String url, String bodyJson, String secret) {
        if (PgNotifyIngressPaths.isForbiddenMerchantOutboundUrl(url)) {
            String reason = PgNotifyIngressPaths.isNotiBrowserResultUrl(url)
                    ? "NOTI_BROWSER_RESULT_SKIPPED"
                    : "ICOPAY_INGRESS_LOOPBACK_SKIPPED";
            log.warn("결제통보 URL 스킵({}) url={}", reason, url);
            return PostOutcome.skipped(reason);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (secret != null && !secret.isBlank()) {
            String sig = hmacSha256Hex(secret.trim(), bodyJson);
            if (sig != null) {
                headers.set("X-Icopay-Signature", "v1=" + sig);
            }
        }
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
        int attempts = 0;
        Integer lastHttp = null;
        String lastErr = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            attempts = attempt + 1;
            if (BACKOFF_MS[attempt] > 0) {
                try {
                    Thread.sleep(BACKOFF_MS[attempt]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return PostOutcome.softFail(attempts, lastHttp, "interrupted");
                }
            }
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                lastHttp = resp.getStatusCode().value();
                if (resp.getStatusCode().is2xxSuccessful()) {
                    return PostOutcome.ok(attempts, lastHttp);
                }
                lastErr = "HTTP " + lastHttp;
                if (isPermanentHttp(lastHttp)) {
                    log.warn("미들웨어 아웃바운드 영구 실패 HTTP {} — 재시도 안 함 url={}", lastHttp, url);
                    return PostOutcome.permanentFail(attempts, lastHttp, lastErr);
                }
                log.warn("미들웨어 아웃바운드 비성공 HTTP {} (시도 {}/{})", lastHttp, attempt + 1, MAX_ATTEMPTS);
            } catch (HttpStatusCodeException e) {
                lastHttp = e.getStatusCode().value();
                lastErr = e.getMessage();
                if (isPermanentHttp(lastHttp)) {
                    log.warn("미들웨어 아웃바운드 영구 실패 HTTP {} — 재시도 안 함 url={}", lastHttp, url);
                    return PostOutcome.permanentFail(attempts, lastHttp, lastErr);
                }
                log.warn("미들웨어 아웃바운드 실패 (시도 {}/{}): {}", attempt + 1, MAX_ATTEMPTS, e.getMessage());
            } catch (Exception e) {
                lastErr = e.getMessage();
                log.warn("미들웨어 아웃바운드 실패 (시도 {}/{}): {}", attempt + 1, MAX_ATTEMPTS, e.getMessage());
            }
        }
        return PostOutcome.softFail(attempts, lastHttp, lastErr);
    }

    private static boolean isPermanentHttp(Integer http) {
        if (http == null) {
            return false;
        }
        return http == 400 || http == 401 || http == 403 || http == 404 || http == 405
                || http == 410 || http == 422;
    }

    private static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean shouldSendForInternalStatus(String st) {
        if (st == null || st.isBlank()) {
            return false;
        }
        String u = st.trim();
        return "08".equals(u) || "10".equals(u) || "20".equals(u) || "30".equals(u) || "99".equals(u);
    }

    private static boolean yn(String v) {
        return v == null || !"N".equalsIgnoreCase(v.trim());
    }
}
