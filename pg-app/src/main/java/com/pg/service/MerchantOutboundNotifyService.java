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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * PG 노티 또는 DirectCredit 승인 적재 후(커밋 이후) 가맹점 {@code tb_merchant_notify_url} 로 JSON POST.
 * <ul>
 *   <li>{@link MerchantNotifyUrl#URL_TYPE_JPAY_NOTIFY}/{@link MerchantNotifyUrl#URL_TYPE_JPAY_CALLBACK}
 *       — 업체 「수신통보」(기본·우선). 만료·실패·성공 공통.</li>
 *   <li>{@link #URL_TYPE_MIDDLEWARE} · {@link #URL_TYPE_BACKGROUND}/{@link #URL_TYPE_RESULT}
 *       — 「결제통보」. URL결제 예외·기존 등록 시 병행.</li>
 * </ul>
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

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final MerchantNotifyOutboundLogService merchantNotifyOutboundLogService;
    private final RestTemplate restTemplate = new RestTemplate();

    public MerchantOutboundNotifyService(PgTrnsctnRepository pgTrnsctnRepository,
                                         OrgUnitRepository orgUnitRepository,
                                         MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                         MerchantNotifyOutboundLogService merchantNotifyOutboundLogService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.merchantNotifyOutboundLogService = merchantNotifyOutboundLogService;
    }

    /**
     * 트랜잭션 커밋 뒤 비동기와 동일하게 한 번 실행 — 실패는 로그만 (PG 수신은 이미 완료).
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
        Runnable job = () -> deliverIfConfigured(trnId, inbound, notifyChannel);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
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
        boolean anyRecvConfigured = false;

        /* 1) 수신통보 우선 (Notify / Callback) */
        Optional<MerchantNotifyUrl> recvN = usable(recvNotifyOpt);
        if (recvN.isPresent()) {
            anyRecvConfigured = true;
            anySuccess |= postOne(merchantCodeTrim, orgUnitId, t, notifyCh,
                    MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY, recvN.get(), basePayload, true);
        }
        Optional<MerchantNotifyUrl> recvC = usable(recvCallbackOpt);
        if (recvC.isPresent()) {
            anyRecvConfigured = true;
            anySuccess |= postOne(merchantCodeTrim, orgUnitId, t, notifyCh,
                    MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK, recvC.get(), basePayload, true);
        }

        /* 2) 결제통보 — 등록된 경우만 병행 (URL결제 예외·레거시) */
        Optional<MerchantNotifyUrl> mw = usable(mwOpt);
        if (mw.isPresent()) {
            anySuccess |= postOne(merchantCodeTrim, orgUnitId, t, notifyCh, URL_TYPE_MIDDLEWARE, mw.get(), basePayload, false);
        }
        Optional<MerchantNotifyUrl> bg = usable(bgOpt);
        if (bg.isPresent()) {
            anySuccess |= postOne(merchantCodeTrim, orgUnitId, t, notifyCh, URL_TYPE_BACKGROUND, bg.get(),
                    withMerchantNotifyTarget(basePayload, URL_TYPE_BACKGROUND), true);
        }
        Optional<MerchantNotifyUrl> rs = usable(rsOpt);
        if (rs.isPresent()) {
            anySuccess |= postOne(merchantCodeTrim, orgUnitId, t, notifyCh, URL_TYPE_RESULT, rs.get(),
                    withMerchantNotifyTarget(basePayload, URL_TYPE_RESULT), true);
        }

        if (!anyRecvConfigured && mw.isEmpty() && bg.isEmpty() && rs.isEmpty()) {
            log.warn("가맹 통보 URL 없음(수신통보·결제통보) mid={} trnId={} orderNo={} channel={} — 업체관리 수신통보 URL 등록 필요",
                    merchantCodeTrim, trnId, t.getOrderNo(), notifyCh);
            return;
        }
        if (anySuccess) {
            markMwOutboundSent(trnId, st);
        }
    }

    private static Optional<MerchantNotifyUrl> usable(Optional<MerchantNotifyUrl> opt) {
        return opt.filter(r -> yn(r.getUseYn()) && r.getNotiUrl() != null && !r.getNotiUrl().isBlank());
    }

    private boolean postOne(String merchantCodeTrim, Long orgUnitId, PgTrnsctn t, String notifyCh,
                            String urlType, MerchantNotifyUrl row, Map<String, Object> payload, boolean plainOrSigned) {
        try {
            String body = OM.writeValueAsString(payload);
            PostOutcome out = plainOrSigned
                    ? postPlainPayNotify(row, body)
                    : postWithRetries(row.getNotiUrl().trim(), body, row.getSignSecret());
            recordOutboundLog(merchantCodeTrim, orgUnitId, t, notifyCh, urlType, row.getNotiUrl().trim(), body, out);
            if (!out.success) {
                log.warn("{} 가맹 통보 전송 실패 (재시도 소진): {}", urlType, row.getNotiUrl());
            }
            return out.success;
        } catch (Exception e) {
            log.warn("{} 가맹 통보 직렬화 실패 trnId={}: {}", urlType, t.getTrnId(), e.getMessage());
            return false;
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
        private final int attempts;
        private final Integer httpStatus;
        private final String errorMessage;

        private PostOutcome(boolean success, int attempts, Integer httpStatus, String errorMessage) {
            this.success = success;
            this.attempts = attempts;
            this.httpStatus = httpStatus;
            this.errorMessage = errorMessage;
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
        if (PgNotifyIngressPaths.isIcopayNotifyIngressUrl(url)) {
            log.warn("결제통보 URL이 ICOPAY 노티 ingress — 루프 방지 스킵 url={}", url);
            return new PostOutcome(true, 0, 200, "ICOPAY_INGRESS_LOOPBACK_SKIPPED");
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
                    return new PostOutcome(false, attempts, lastHttp, "interrupted");
                }
            }
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                lastHttp = resp.getStatusCode().value();
                if (resp.getStatusCode().is2xxSuccessful()) {
                    return new PostOutcome(true, attempts, lastHttp, null);
                }
                lastErr = "HTTP " + lastHttp;
                log.warn("미들웨어 아웃바운드 비성공 HTTP {} (시도 {}/{})", resp.getStatusCode(), attempt + 1, MAX_ATTEMPTS);
            } catch (Exception e) {
                lastErr = e.getMessage();
                log.warn("미들웨어 아웃바운드 실패 (시도 {}/{}): {}", attempt + 1, MAX_ATTEMPTS, e.getMessage());
            }
        }
        return new PostOutcome(false, attempts, lastHttp, lastErr);
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
