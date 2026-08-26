package com.pg.service;

import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.elementpay.ElementPayCredentials;
import com.pg.noti.NotiProvisionClient;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.ElementPayHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ElementPay URL/INLINE 결제가 getStatus 등으로 ICOPAY에만 승인되고 EP Cabinet→NOTI 웹훅이
 * 오지 않은 경우, ICOPAY가 NOTI {@code /noti/elementpay} 로 EP형 pay 미러를 보내
 * 노티로그·가맹 Callback/Dealmai 릴레이가 쌓이게 합니다.
 * <p>미러 본문은 Webhook Signing Secret 으로 서명하며, NOTI→ICOPAY 재전달 시
 * 이미 승인된 주문은 205 + {@code X-Icopay-Comp-Id} 로 가맹 매칭됩니다.
 * 본사 「미러 재전송」은 {@link #remirrorByOrderOrTrnId} 로 기존 성공 건을 재결제 없이 송신합니다.
 */
@Service
public class ElementPayNotiMiddlewareMirrorService {

    private static final Logger log = LoggerFactory.getLogger(ElementPayNotiMiddlewareMirrorService.class);
    public static final String FIELD_ICOPAY_SOURCE = "icopay_source";
    public static final String SOURCE_URL_PAY_STATUS_SYNC = "url_pay_status_sync";
    public static final String SOURCE_HQ_MANUAL_RESEND = "hq_manual_resend";
    private static final String ST_PAID = "10";

    private final HqNotifyEnvService hqNotifyEnvService;
    private final PgAgencyRepository pgAgencyRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final ConcurrentHashMap<String, Long> recentlyMirrored = new ConcurrentHashMap<>();

    public ElementPayNotiMiddlewareMirrorService(HqNotifyEnvService hqNotifyEnvService,
                                                   PgAgencyRepository pgAgencyRepository,
                                                   PgTrnsctnRepository pgTrnsctnRepository) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.pgAgencyRepository = pgAgencyRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    /**
     * 트랜잭션 커밋 후 NOTI 로 pay 미러 POST. 실패는 로그만 (결제 자체는 이미 확정).
     */
    public void scheduleUrlPayMirrorAfterCommit(PgTrnsctn txn, boolean paid) {
        if (txn == null || txn.getOrderNo() == null || txn.getOrderNo().isBlank()) {
            return;
        }
        if (!PgVendor.isElementPayFamily(txn.getVan())) {
            return;
        }
        final String trnId = txn.getTrnId();
        final String orderNo = txn.getOrderNo().trim();
        final String merchantId = txn.getMerchantId() != null ? txn.getMerchantId().trim() : "";
        final String paymentId = firstNonBlank(txn.getChillTransactionId(), txn.getApprovalNo());
        final String amount = txn.getAmtKrw() != null
                ? txn.getAmtKrw().stripTrailingZeros().toPlainString() : "";
        final String currency = txn.getCurType() != null ? txn.getCurType().trim() : "THB";
        final String van = txn.getVan();
        final boolean paidFlag = paid;

        Runnable job = () -> mirrorPayToNoti(trnId, orderNo, merchantId, paymentId, amount, currency, van,
                paidFlag, false, SOURCE_URL_PAY_STATUS_SYNC);
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

    /**
     * 본사 미러 재전송 — 주문번호 또는 거래번호(trnId / EP payment id)로 성공 ElementPay 건 조회 후 NOTI 송신.
     */
    public Map<String, Object> remirrorByOrderOrTrnId(String orderNo, String trnOrPaymentId, boolean force) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<PgTrnsctn> found = resolveTxn(orderNo, trnOrPaymentId);
        if (found.isEmpty()) {
            out.put("success", false);
            out.put("errorCode", "TXN_NOT_FOUND");
            out.put("message", "거래를 찾을 수 없습니다. 주문번호 또는 거래번호를 확인하세요.");
            return out;
        }
        PgTrnsctn t = found.get();
        out.put("trnId", t.getTrnId());
        out.put("orderNo", t.getOrderNo());
        out.put("merchantId", t.getMerchantId());
        out.put("van", t.getVan());
        out.put("status", t.getStatus());
        out.put("amount", t.getAmtKrw() != null ? t.getAmtKrw().stripTrailingZeros().toPlainString() : null);
        out.put("currency", t.getCurType());
        if (!PgVendor.isElementPayFamily(t.getVan())) {
            out.put("success", false);
            out.put("errorCode", "NOT_ELEMENTPAY");
            out.put("message", "ElementPay 거래만 미러 재전송할 수 있습니다.");
            return out;
        }
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        if (!ST_PAID.equals(st) && !"00".equals(st) && !"0000".equals(st)) {
            out.put("success", false);
            out.put("errorCode", "NOT_PAID");
            out.put("message", "성공(승인) 상태의 거래만 재전송할 수 있습니다. status=" + st);
            return out;
        }
        if (t.getOrderNo() == null || t.getOrderNo().isBlank()) {
            out.put("success", false);
            out.put("errorCode", "ORDER_MISSING");
            out.put("message", "주문번호가 없습니다.");
            return out;
        }
        Map<String, Object> post = mirrorPayToNoti(
                t.getTrnId(),
                t.getOrderNo().trim(),
                t.getMerchantId() != null ? t.getMerchantId().trim() : "",
                firstNonBlank(t.getChillTransactionId(), t.getApprovalNo()),
                t.getAmtKrw() != null ? t.getAmtKrw().stripTrailingZeros().toPlainString() : "",
                t.getCurType() != null ? t.getCurType().trim() : "THB",
                t.getVan(),
                true,
                force,
                SOURCE_HQ_MANUAL_RESEND);
        out.putAll(post);
        return out;
    }

    /** 조회만 — 재전송 전 미리보기. */
    public Map<String, Object> lookupTxn(String orderNo, String trnOrPaymentId) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<PgTrnsctn> found = resolveTxn(orderNo, trnOrPaymentId);
        if (found.isEmpty()) {
            out.put("success", false);
            out.put("errorCode", "TXN_NOT_FOUND");
            out.put("message", "거래를 찾을 수 없습니다.");
            return out;
        }
        PgTrnsctn t = found.get();
        out.put("success", true);
        out.put("trnId", t.getTrnId());
        out.put("orderNo", t.getOrderNo());
        out.put("merchantId", t.getMerchantId());
        out.put("van", t.getVan());
        out.put("status", t.getStatus());
        out.put("amount", t.getAmtKrw() != null ? t.getAmtKrw().stripTrailingZeros().toPlainString() : null);
        out.put("currency", t.getCurType());
        out.put("paymentId", firstNonBlank(t.getChillTransactionId(), t.getApprovalNo()));
        out.put("elementPay", PgVendor.isElementPayFamily(t.getVan()));
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        out.put("paid", ST_PAID.equals(st) || "00".equals(st) || "0000".equals(st));
        return out;
    }

    private Optional<PgTrnsctn> resolveTxn(String orderNo, String trnOrPaymentId) {
        String order = orderNo != null ? orderNo.trim() : "";
        String key = trnOrPaymentId != null ? trnOrPaymentId.trim() : "";
        if (!order.isBlank()) {
            List<PgTrnsctn> byOrder = pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(order);
            if (!byOrder.isEmpty()) {
                Optional<PgTrnsctn> ep = byOrder.stream()
                        .filter(t -> PgVendor.isElementPayFamily(t.getVan()))
                        .findFirst();
                return ep.isPresent() ? ep : Optional.of(byOrder.get(0));
            }
        }
        if (!key.isBlank()) {
            Optional<PgTrnsctn> byId = pgTrnsctnRepository.findById(key);
            if (byId.isPresent()) {
                return byId;
            }
            return pgTrnsctnRepository.findFirstByChillTransactionIdOrderByCreatedAtDesc(key);
        }
        return Optional.empty();
    }

    private Map<String, Object> mirrorPayToNoti(String trnId, String orderNo, String merchantId, String paymentId,
                                                String amount, String currency, String van, boolean paid,
                                                boolean force, String icopaySource) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("notiUrl", "");
        String dedupeKey = orderNo + "|" + (paid ? "10" : "99");
        long now = System.currentTimeMillis();
        if (!force) {
            Long prev = recentlyMirrored.putIfAbsent(dedupeKey, now);
            if (prev != null && now - prev < 120_000L) {
                log.debug("ElementPay NOTI mirror skip duplicate order={}", orderNo);
                result.put("success", false);
                result.put("errorCode", "DEDUPE_SKIP");
                result.put("message", "120초 이내 동일 주문 미러가 있어 생략했습니다. 강제 재전송을 사용하세요.");
                return result;
            }
        }
        recentlyMirrored.put(dedupeKey, now);
        pruneMirrorCache(now);

        Optional<PgAgency> agOpt = resolveAgency(van);
        if (agOpt.isEmpty()) {
            log.warn("ElementPay NOTI mirror: agency 없음 van={} order={}", van, orderNo);
            result.put("success", false);
            result.put("errorCode", "AGENCY_MISSING");
            result.put("message", "ElementPay 결제대행사 설정을 찾을 수 없습니다.");
            return result;
        }
        ElementPayCredentials cred = ElementPayCredentials.from(agOpt.get());
        String secret = cred.webhookSecretKey();
        if (secret == null || secret.isBlank()) {
            secret = cred.apiSecretKey();
        }
        if (secret == null || secret.isBlank()) {
            log.warn("ElementPay NOTI mirror: signing secret 없음 order={}", orderNo);
            result.put("success", false);
            result.put("errorCode", "SECRET_MISSING");
            result.put("message", "ElementPay 서명 키가 없습니다.");
            return result;
        }

        String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
        String url = trimSlash(notiBase) + "/noti/elementpay";
        result.put("notiUrl", url);

        String source = icopaySource != null && !icopaySource.isBlank()
                ? icopaySource.trim() : SOURCE_URL_PAY_STATUS_SYNC;

        Map<String, String> form = new LinkedHashMap<>();
        form.put("method", "pay");
        form.put("id", paymentId != null ? paymentId : "");
        form.put("order", orderNo);
        form.put("amount", amount != null ? amount : "");
        form.put("currency", currency != null && !currency.isBlank() ? currency : "THB");
        form.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        form.put("status", paid ? "205" : "204");
        form.put("status_message", paid ? "Payment success" : "Payment failed");
        if (cred.merchantKey() != null && !cred.merchantKey().isBlank()) {
            form.put("key", cred.merchantKey().trim());
        }
        form.put(FIELD_ICOPAY_SOURCE, source);
        if (merchantId != null && !merchantId.isBlank()) {
            form.put("compId", merchantId);
            form.put("merchantId", merchantId);
        }
        String hash = ElementPayHashUtil.signCallbackRequestPhpHttpBuildQuery(secret, form);
        form.put("hash", hash);
        String body = ElementPayHashUtil.buildApiQueryString(form);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("X-Icopay-Ep-Mirror", source);
        if (merchantId != null && !merchantId.isBlank()) {
            headers.set("X-Icopay-Comp-Id", merchantId);
        }
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);
            int http = resp.getStatusCode().value();
            log.info("ElementPay NOTI mirror posted order={} trnId={} http={} noti={} source={}",
                    orderNo, trnId, http, url, source);
            result.put("success", http >= 200 && http < 400);
            result.put("httpStatus", http);
            result.put("icopaySource", source);
            result.put("message", result.get("success").equals(Boolean.TRUE)
                    ? "NOTI /noti/elementpay 미러 전송 완료"
                    : "NOTI 응답 HTTP " + http);
            String respBody = resp.getBody();
            if (respBody != null && respBody.length() > 400) {
                respBody = respBody.substring(0, 400) + "…";
            }
            result.put("responsePreview", respBody);
            if (!Boolean.TRUE.equals(result.get("success"))) {
                result.put("errorCode", "NOTI_HTTP_" + http);
            }
            return result;
        } catch (Exception e) {
            log.warn("ElementPay NOTI mirror 실패 order={} url={}: {}", orderNo, url, e.getMessage());
            result.put("success", false);
            result.put("errorCode", "NOTI_POST_FAILED");
            result.put("message", "NOTI 전송 실패: " + e.getMessage());
            return result;
        }
    }

    private Optional<PgAgency> resolveAgency(String van) {
        String pg = van != null ? van.trim() : "";
        if (!pg.isBlank()) {
            Optional<PgAgency> byCd = pgAgencyRepository.findByPgCd(pg);
            if (byCd.isPresent() && PgVendor.isElementPayFamily(byCd.get().getPgCd())) {
                return byCd;
            }
        }
        return pgAgencyRepository.findAllByOrderByPgCdAsc().stream()
                .filter(a -> PgVendor.isElementPayFamily(a.getPgCd()))
                .filter(a -> a.getUseYn() == null || "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .findFirst();
    }

    private void pruneMirrorCache(long now) {
        if (recentlyMirrored.size() < 500) {
            return;
        }
        recentlyMirrored.entrySet().removeIf(e -> now - e.getValue() > 600_000L);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }

    private static String trimSlash(String u) {
        if (u == null) {
            return "";
        }
        String s = u.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
