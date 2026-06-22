package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.notify.NotifyIdempotencyLock;
import com.pg.integration.pg.notify.PgNotifyInboundTxnHandler;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.util.JpayDisputeNotifyStatusResolver;
import com.pg.util.JpaySignatureUtil;
import com.pg.util.JpayTransactionIdApplier;
import com.pg.util.NotifyToTxnStatusMerge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * JPAY Dispute webhook(Refund·Chargeback) — 가맹 포털 Dispute URL 로 수신한
 * {@code alert_type}·{@code alert_status} 노티를 {@link PgTrnsctn}에 반영합니다.
 */
@Service
public class JpayDisputeNotifyToTrnsctnService implements PgNotifyInboundTxnHandler {

    public static final String ICOPAY_DISPUTE_INBOUND_FLAG = "_icopay_dispute_inbound";

    private static final Logger log = LoggerFactory.getLogger(JpayDisputeNotifyToTrnsctnService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ORIGIN_URL = "URL";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final SettlementArrearsService settlementArrearsService;
    private final NotifyIdempotencyLock notifyIdempotencyLock;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public JpayDisputeNotifyToTrnsctnService(PgTrnsctnRepository pgTrnsctnRepository,
                                             PgAgencyRepository pgAgencyRepository,
                                             SettlementArrearsService settlementArrearsService,
                                             NotifyIdempotencyLock notifyIdempotencyLock,
                                             OrgUnitRepository orgUnitRepository,
                                             MerchantNotifyUrlRepository merchantNotifyUrlRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.settlementArrearsService = settlementArrearsService;
        this.notifyIdempotencyLock = notifyIdempotencyLock;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
    }

    @Override
    public int order() {
        return -25;
    }

    @Override
    @Transactional
    public boolean tryRecord(PgNotifyInbound in, String notifyChannel) {
        try {
            return doTry(in, notifyChannel);
        } catch (Exception e) {
            log.warn("JPAY Dispute 노티 적재 예외: {}", e.getMessage());
            return true;
        }
    }

    private boolean doTry(PgNotifyInbound in, String notifyChannel) {
        if (in == null || !"PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
            return false;
        }
        String raw = in.getRawBody();
        if (raw == null || raw.isBlank()) {
            return false;
        }
        Map<String, String> form = parseFields(raw.trim());
        if (!JpayDisputeNotifyStatusResolver.looksLikeDisputeWebhook(form)) {
            return false;
        }
        if ("Y".equalsIgnoreCase(first(form, ICOPAY_DISPUTE_INBOUND_FLAG))
                || "Y".equalsIgnoreCase(first(form, JpayManualFollowUpNotifyService.ICOPAY_MANUAL_FOLLOWUP_FLAG))) {
            log.debug("JPAY Dispute echo 스킵 orderid={}", first(form, "orderid"));
            return true;
        }
        String memberid = first(form, "memberid");
        String orderid = first(form, "orderid");
        String alertType = first(form, "alert_type");
        String alertStatus = first(form, "alert_status");
        Optional<PgAgency> agOpt = findJpayAgencyByMerchantMid(memberid);
        if (agOpt.isEmpty()) {
            log.debug("JPAY Dispute MID 없음 memberid={}", memberid);
            return false;
        }
        PgAgency ag = agOpt.get();
        String apiKey = ag.getApiKey() != null ? ag.getApiKey().trim() : "";
        String sign = first(form, "sign");
        if (!apiKey.isEmpty() && !sign.isBlank()) {
            if (!JpaySignatureUtil.verifyDisputeWebhookSign(form, apiKey, sign)) {
                if (!allowTrustedIngressWithoutSign(in)) {
                    log.warn("JPAY Dispute 서명 불일치 orderid={} alert_type={}", orderid, alertType);
                    return false;
                }
                log.warn("JPAY Dispute 서명 불일치 — ingress 가맹 확정 건 반영 orderid={}", orderid);
            }
        } else if (!allowTrustedIngressWithoutSign(in)) {
            log.warn("JPAY Dispute sign·apiKey 없음 orderid={}", orderid);
            return false;
        }
        String next = JpayDisputeNotifyStatusResolver.resolveInternalStatus(form);
        if (next == null || next.isBlank()) {
            log.info("JPAY Dispute 미반영 alert_status={} alert_type={} orderid={}", alertStatus, alertType, orderid);
            return true;
        }
        String merchantId = resolveMerchantId(in, orderid);
        if (merchantId == null || merchantId.isBlank()) {
            log.warn("JPAY Dispute 가맹점 미해석 orderid={}", orderid);
            return false;
        }
        notifyIdempotencyLock.lock("JPAY_DISPUTE", "ORD:" + merchantId.trim() + "|" + orderid.trim());
        String on = orderid.trim();
        if (on.length() > 64) {
            on = on.substring(0, 64);
        }
        Optional<PgTrnsctn> ex = findJpayTxn(merchantId.trim(), on);
        if (ex.isEmpty()) {
            log.warn("JPAY Dispute 대상 결제내역 없음 merchantId={} orderNo={}", merchantId, on);
            return false;
        }
        PgTrnsctn t = ex.get();
        String ch = notifyChannel == null || notifyChannel.isBlank() ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        String prevStatus = t.getStatus();
        String prevSettledYn = t.getSettledYn();
        String merged = NotifyToTxnStatusMerge.merge(t.getStatus(), next, ch);
        if (merged == null || merged.isBlank()) {
            merged = next;
        }
        if (merged.equals(prevStatus)) {
            log.info("JPAY Dispute 멱등 스킵 trnId={} status={} alert_type={}", t.getTrnId(), merged, alertType);
            return true;
        }
        JpayTransactionIdApplier.apply(t, first(form, "transaction_id"));
        applyAmountIfPresent(t, form);
        t.setNotifyChannelType(ch);
        t.setStatus(merged);
        t.setChillPaymentStatus(JpayDisputeNotifyStatusResolver.chillPaymentStatusLabel(merged, alertType));
        if (t.getVan() == null || t.getVan().isBlank()) {
            t.setVan(PgVendor.JPAY.length() > 10 ? PgVendor.JPAY.substring(0, 10) : PgVendor.JPAY);
        }
        pgTrnsctnRepository.save(t);
        try {
            settlementArrearsService.registerPostSettlementRecoveryIfDue(prevStatus, prevSettledYn, t);
        } catch (Exception recoveryEx) {
            log.warn("JPAY Dispute 환수금 등록 실패 trnId={}: {}", t.getTrnId(), recoveryEx.getMessage());
        }
        log.info("JPAY Dispute 반영 trnId={} merchantId={} orderNo={} alert_type={} status {}→{}",
                t.getTrnId(), merchantId, on, alertType, prevStatus, merged);
        scheduleMerchantRelay(t.getTrnId(), form, merchantId.trim());
        return true;
    }

    private void scheduleMerchantRelay(String trnId, Map<String, String> form, String merchantCode) {
        if (trnId == null || trnId.isBlank()) {
            return;
        }
        Map<String, String> relayForm = new LinkedHashMap<>(form);
        Runnable job = () -> relayToMerchantNotify(trnId, relayForm, merchantCode);
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

    private void relayToMerchantNotify(String trnId, Map<String, String> form, String merchantCode) {
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCodeIgnoreCase(merchantCode);
        if (ouOpt.isEmpty()) {
            return;
        }
        Optional<MerchantNotifyUrl> notifyRow = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(
                ouOpt.get().getId(), MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY);
        if (notifyRow.isEmpty() || !"Y".equalsIgnoreCase(String.valueOf(notifyRow.get().getUseYn()).trim())) {
            return;
        }
        String targetUrl = notifyRow.get().getNotiUrl() != null ? notifyRow.get().getNotiUrl().trim() : "";
        if (targetUrl.isBlank()) {
            return;
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            if (ICOPAY_DISPUTE_INBOUND_FLAG.equalsIgnoreCase(e.getKey().trim())) {
                continue;
            }
            body.add(e.getKey(), e.getValue().trim());
        }
        body.add(ICOPAY_DISPUTE_INBOUND_FLAG, "Y");
        body.add("icopaycompid", merchantCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(targetUrl, new HttpEntity<>(body, headers), String.class);
            log.info("JPAY Dispute 가맹 노티 릴레이 trnId={} http={} url={}",
                    trnId, resp.getStatusCode().value(), maskUrl(targetUrl));
        } catch (Exception e) {
            log.warn("JPAY Dispute 가맹 노티 릴레이 실패 trnId={}: {}", trnId, e.getMessage());
        }
    }

    private static String maskUrl(String url) {
        if (url == null || url.length() <= 48) {
            return url;
        }
        return url.substring(0, 32) + "…";
    }

    private static boolean allowTrustedIngressWithoutSign(PgNotifyInbound in) {
        return in != null && in.getMerchantId() != null && !in.getMerchantId().isBlank();
    }

    private void applyAmountIfPresent(PgTrnsctn t, Map<String, String> form) {
        String amtStr = first(form, "refund_amount");
        if (amtStr.isBlank()) {
            amtStr = first(form, "amount");
        }
        if (amtStr.isBlank()) {
            return;
        }
        try {
            BigDecimal a = new BigDecimal(amtStr.replace(",", "").trim());
            if (a.compareTo(BigDecimal.ZERO) > 0) {
                t.setAmtKrw(a);
            }
        } catch (Exception ignored) {
            /* keep */
        }
    }

    private String resolveMerchantId(PgNotifyInbound in, String orderid) {
        if (in != null && in.getMerchantId() != null && !in.getMerchantId().isBlank()) {
            return in.getMerchantId().trim();
        }
        if (orderid == null || orderid.isBlank()) {
            return "";
        }
        String on = orderid.trim();
        Optional<PgTrnsctn> any = pgTrnsctnRepository.findFirstByOrderNoOrderByCreatedAtDesc(on);
        if (any.isPresent() && any.get().getMerchantId() != null && !any.get().getMerchantId().isBlank()) {
            return any.get().getMerchantId().trim();
        }
        return "";
    }

    private Optional<PgTrnsctn> findJpayTxn(String merchantId, String orderNo) {
        for (String origin : new String[]{"SUBSCRIPTION", "MERCHANT_API", ORIGIN_URL, "API"}) {
            Optional<PgTrnsctn> hit = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                    merchantId, orderNo, origin);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }

    private Optional<PgAgency> findJpayAgencyByMerchantMid(String memberid) {
        String m = memberid.trim();
        return pgAgencyRepository.findByMerchantMidOrderByIdAsc(m).stream()
                .filter(a -> PgVendor.isJpayFamily(a.getPgCd()))
                .findFirst();
    }

    private static Map<String, String> parseFields(String body) {
        if (body.startsWith("{")) {
            return parseJsonLowerKeys(body);
        }
        return parseFormLowerKeys(body);
    }

    private static Map<String, String> parseJsonLowerKeys(String json) {
        Map<String, String> m = new LinkedHashMap<>();
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root != null && root.isObject()) {
                Iterator<String> it = root.fieldNames();
                while (it.hasNext()) {
                    String k = it.next();
                    JsonNode v = root.get(k);
                    if (v != null && !v.isNull() && v.isValueNode()) {
                        String val = v.asText();
                        if (val != null && !val.isBlank()) {
                            m.put(k.toLowerCase(Locale.ROOT), val.trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        return m;
    }

    private static Map<String, String> parseFormLowerKeys(String body) {
        Map<String, String> m = new LinkedHashMap<>();
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
                    m.put(k, v);
                }
            }
        } catch (Exception ignored) {
        }
        return m;
    }

    private static String first(Map<String, String> m, String key) {
        String v = m.get(key.toLowerCase(Locale.ROOT));
        return v != null ? v.trim() : "";
    }
}
