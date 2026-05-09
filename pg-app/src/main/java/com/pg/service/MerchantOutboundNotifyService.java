package com.pg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
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
 *   <li>{@link #URL_TYPE_MIDDLEWARE} — PG중계 동일 페이로드(+선택 HMAC).</li>
 *   <li>{@link #URL_TYPE_BACKGROUND},{@link #URL_TYPE_RESULT} — 업체등록 「결제통보 URL」(URL·챗봇·노티·인라인 DirectCredit 공통 ChillPay 플로우).</li>
 * </ul>
 * 칠페이 계열({@link PgVendor#isChillPayVendorCode})만 1차 활성화.
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
    private final RestTemplate restTemplate = new RestTemplate();

    public MerchantOutboundNotifyService(PgTrnsctnRepository pgTrnsctnRepository,
                                         OrgUnitRepository orgUnitRepository,
                                         MerchantNotifyUrlRepository merchantNotifyUrlRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
    }

    /**
     * 트랜잭션 커밋 뒤 비동기와 동일하게 한 번 실행 — 실패는 로그만 (PG 수신은 이미 완료).
     */
    public void scheduleAfterTxnCommit(PgTrnsctn savedTxn, PgNotifyInbound inbound, String notifyChannel) {
        if (savedTxn == null || savedTxn.getTrnId() == null || savedTxn.getTrnId().isBlank()) {
            return;
        }
        if (!PgVendor.isChillPayVendorCode(savedTxn.getVan())) {
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
        Optional<MerchantNotifyUrl> mwOpt = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, URL_TYPE_MIDDLEWARE);
        Optional<MerchantNotifyUrl> bgOpt = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, URL_TYPE_BACKGROUND);
        Optional<MerchantNotifyUrl> rsOpt = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, URL_TYPE_RESULT);

        Map<String, Object> basePayload = buildPayload(t, inbound, notifyChannel);

        boolean anySuccess = false;

        Optional<MerchantNotifyUrl> mw = mwOpt.filter(r -> yn(r.getUseYn()) && r.getNotiUrl() != null && !r.getNotiUrl().isBlank());
        if (mw.isPresent()) {
            try {
                String bodyMw = OM.writeValueAsString(basePayload);
                boolean okMw = postWithRetries(mw.get().getNotiUrl().trim(), bodyMw, mw.get().getSignSecret());
                if (okMw) {
                    anySuccess = true;
                }
            } catch (Exception e) {
                log.warn("MIDDLEWARE 결제통보 직렬화 실패 trnId={}: {}", trnId, e.getMessage());
            }
        }

        Optional<MerchantNotifyUrl> bg = bgOpt.filter(r -> yn(r.getUseYn()) && r.getNotiUrl() != null && !r.getNotiUrl().isBlank());
        if (bg.isPresent()) {
            try {
                String bodyBg = OM.writeValueAsString(withMerchantNotifyTarget(basePayload, URL_TYPE_BACKGROUND));
                if (postPlainPayNotify(bg.get(), bodyBg, URL_TYPE_BACKGROUND)) {
                    anySuccess = true;
                }
            } catch (Exception e) {
                log.warn("BACKGROUND 결제통보 직렬화 실패 trnId={}: {}", trnId, e.getMessage());
            }
        }

        Optional<MerchantNotifyUrl> rs = rsOpt.filter(r -> yn(r.getUseYn()) && r.getNotiUrl() != null && !r.getNotiUrl().isBlank());
        if (rs.isPresent()) {
            try {
                String bodyRs = OM.writeValueAsString(withMerchantNotifyTarget(basePayload, URL_TYPE_RESULT));
                if (postPlainPayNotify(rs.get(), bodyRs, URL_TYPE_RESULT)) {
                    anySuccess = true;
                }
            } catch (Exception e) {
                log.warn("RESULT 결제통보 직렬화 실패 trnId={}: {}", trnId, e.getMessage());
            }
        }

        if (!mw.isPresent() && !bg.isPresent() && !rs.isPresent()) {
            return;
        }
        if (anySuccess) {
            markMwOutboundSent(trnId, st);
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
    private boolean postPlainPayNotify(MerchantNotifyUrl row, String bodyJson, String kindForLog) {
        String sig = row.getSignSecret();
        boolean ok = postWithRetries(row.getNotiUrl().trim(), bodyJson, sig);
        if (!ok) {
            log.warn("{} 결제통보 전송 실패 (재시도 소진): {}", kindForLog, row.getNotiUrl());
        }
        return ok;
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
        m.put("event", "pg.payment.status");
        m.put("compId", t.getMerchantId());
        m.put("trnId", t.getTrnId());
        m.put("orderNo", t.getOrderNo());
        m.put("status", t.getStatus());
        m.put("pgTxnId", t.getChillTransactionId());
        m.put("amount", t.getAmtKrw() != null ? t.getAmtKrw().stripTrailingZeros().toPlainString() : null);
        m.put("currency", t.getCurType());
        m.put("van", t.getVan());
        m.put("origin", t.getOrigin());
        m.put("notifyChannel", notifyChannel);
        m.put("chillPaymentStatus", t.getChillPaymentStatus());
        m.put("paidAt", t.getPaidAt() != null ? t.getPaidAt().toString() : null);
        m.put("ts", System.currentTimeMillis());
        if (inbound != null && inbound.getId() != null) {
            m.put("inboundId", inbound.getId());
        }
        return m;
    }

    private boolean postWithRetries(String url, String bodyJson, String secret) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (secret != null && !secret.isBlank()) {
            String sig = hmacSha256Hex(secret.trim(), bodyJson);
            if (sig != null) {
                headers.set("X-Icopay-Signature", "v1=" + sig);
            }
        }
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (BACKOFF_MS[attempt] > 0) {
                try {
                    Thread.sleep(BACKOFF_MS[attempt]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
                if (resp.getStatusCode().is2xxSuccessful()) {
                    return true;
                }
                log.warn("미들웨어 아웃바운드 비성공 HTTP {} (시도 {}/{})", resp.getStatusCode(), attempt + 1, MAX_ATTEMPTS);
            } catch (Exception e) {
                log.warn("미들웨어 아웃바운드 실패 (시도 {}/{}): {}", attempt + 1, MAX_ATTEMPTS, e.getMessage());
            }
        }
        return false;
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
