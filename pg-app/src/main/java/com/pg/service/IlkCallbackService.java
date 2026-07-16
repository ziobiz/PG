package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.dto.NotifyReceiveOutcome;
import com.pg.entity.MerchantIlkSubscription;
import com.pg.entity.PgAgency;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.ilk.IlkCredentials;
import com.pg.integration.pg.ilk.IlkCryptoUtil;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.repository.MerchantIlkSubscriptionRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.splitpay.SplitPayPaymentHookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ILK Back Noti(JSON) 동기 ACK — ElementPay 콜백과 동일하게 ingress 선처리.
 */
@Service
public class IlkCallbackService {

    private static final Logger log = LoggerFactory.getLogger(IlkCallbackService.class);

    private final HqNotifyEnvService hqNotifyEnvService;
    private final IlkSaleRecordService ilkSaleRecordService;
    private final PgNotifyInboundPersistService inboundPersistService;
    private final SettlementCalcService settlementCalcService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final TransactionReceiptEmailService transactionReceiptEmailService;
    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantIlkSubscriptionRepository ilkSubscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IlkCallbackService(HqNotifyEnvService hqNotifyEnvService,
                              IlkSaleRecordService ilkSaleRecordService,
                              PgNotifyInboundPersistService inboundPersistService,
                              SettlementCalcService settlementCalcService,
                              MerchantOutboundNotifyService merchantOutboundNotifyService,
                              SplitPayPaymentHookService splitPayPaymentHookService,
                              TransactionReceiptEmailService transactionReceiptEmailService,
                              PgAgencyRepository pgAgencyRepository,
                              MerchantIlkSubscriptionRepository ilkSubscriptionRepository) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.ilkSaleRecordService = ilkSaleRecordService;
        this.inboundPersistService = inboundPersistService;
        this.settlementCalcService = settlementCalcService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
        this.pgAgencyRepository = pgAgencyRepository;
        this.ilkSubscriptionRepository = ilkSubscriptionRepository;
    }

    public Optional<NotifyReceiveOutcome> tryHandleSyncCallback(String pathToken,
                                                                String notifyTargetCode,
                                                                String rawBody,
                                                                String clientIp,
                                                                HttpServletRequest request) {
        if (rawBody == null || rawBody.isBlank()) {
            return Optional.empty();
        }
        String target = notifyTargetCode != null ? notifyTargetCode.trim().toUpperCase(Locale.ROOT) : "";
        boolean looksIlkPath = PgVendor.isIlkVendorCode(target) || "ILK".equals(target);
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            return Optional.empty();
        }
        if (!looksIlkPath && !looksLikeIlkJson(root)) {
            return Optional.empty();
        }
        String expected = hqNotifyEnvService.getOrCreate().getIngressToken();
        if (expected == null || pathToken == null || !expected.equals(pathToken.trim())) {
            return Optional.of(NotifyReceiveOutcome.json(
                    ackJson("DECLINED", "", "", "", null), HttpStatus.UNAUTHORIZED));
        }
        return Optional.of(handleBackNoti(root, rawBody, clientIp, notifyTargetCode));
    }

    @Transactional
    protected NotifyReceiveOutcome handleBackNoti(JsonNode root, String rawBody, String clientIp,
                                                  String notifyTargetCode) {
        String mid = text(root, "merchantInformation.merchantId");
        String siteId = text(root, "merchantInformation.merchantSiteId");
        String orderNo = text(root, "clientReferenceInformation.code");
        String status = text(root, "status");
        String id = text(root, "id");
        String transType = text(root, "orderInformation.transType");

        Optional<IlkCredentials> credOpt = resolveCredByMerchantId(mid);
        if (credOpt.isPresent() && !IlkCryptoUtil.verifySign(toMap(root), credOpt.get().seedKey())) {
            log.warn("ILK Back Noti 서명 불일치 orderNo={}", orderNo);
            return NotifyReceiveOutcome.json(
                    ackJson("DECLINED", mid, siteId, orderNo, credOpt.get()), HttpStatus.OK);
        }

        persistInbound(rawBody, clientIp, notifyTargetCode, mid);

        Optional<PgTrnsctn> txn = ilkSaleRecordService.findAnyByOrder(orderNo);
        String compCode = txn.map(PgTrnsctn::getMerchantId).orElse("");
        boolean paid = "SUCCESS".equalsIgnoreCase(status)
                && (transType.isBlank() || "PAY".equalsIgnoreCase(transType));
        boolean cancel = "CANCEL".equalsIgnoreCase(transType) || "REFUND".equalsIgnoreCase(transType);

        if (!compCode.isBlank()) {
            if (cancel) {
                ilkSaleRecordService.applyCancel(compCode, orderNo, id, status);
            } else {
                Optional<PgTrnsctn> saved = ilkSaleRecordService.applyOutcome(
                        compCode, orderNo, paid, id, status);
                if (saved.isPresent() && paid) {
                    PgTrnsctn t = saved.get();
                    try {
                        splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
                    } catch (Exception ignored) {
                    }
                    try {
                        transactionReceiptEmailService.scheduleAfterPaid(t);
                    } catch (Exception e) {
                        log.warn("ILK 영수증: {}", e.getMessage());
                    }
                    try {
                        settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
                    } catch (Exception e) {
                        log.warn("ILK 정산: {}", e.getMessage());
                    }
                    try {
                        merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "CALLBACK");
                    } catch (Exception e) {
                        log.warn("ILK 아웃바운드: {}", e.getMessage());
                    }
                    try {
                        activateIlkSubscriptionOnPaid(compCode, orderNo, id);
                    } catch (Exception e) {
                        log.debug("ILK 구독 활성화: {}", e.getMessage());
                    }
                }
            }
        }

        String ackStatus = "SUCCESS".equalsIgnoreCase(status) ? "SUCCESS" : "DECLINED";
        return NotifyReceiveOutcome.json(
                ackJson(ackStatus, mid, siteId, orderNo, credOpt.orElse(null)), HttpStatus.OK);
    }

    private void activateIlkSubscriptionOnPaid(String compCode, String orderNo, String authId) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        Optional<MerchantIlkSubscription> sub = Optional.empty();
        if (compCode != null && !compCode.isBlank()) {
            sub = ilkSubscriptionRepository.findByCompIdAndSubscriptionNo(compCode, orderNo);
        }
        if (sub.isEmpty()) {
            sub = ilkSubscriptionRepository.findFirstBySubscriptionNoOrderByIdDesc(orderNo);
        }
        if (sub.isEmpty()) {
            sub = ilkSubscriptionRepository.findFirstByFirstOrderNoOrderByIdDesc(orderNo);
        }
        sub.ifPresent(s -> {
            s.setStatus(MerchantIlkSubscription.STATUS_ACTIVE);
            if (authId != null && !authId.isBlank()) {
                s.setFirstAuthId(authId);
            }
            if (s.getChargeCount() == null || s.getChargeCount() < 1) {
                s.setChargeCount(1);
            }
            s.setLastChargeAt(java.time.LocalDateTime.now());
            if (s.getNextChargeAt() == null) {
                s.setNextChargeAt(java.time.LocalDateTime.now().plusDays(30));
            }
            ilkSubscriptionRepository.save(s);
        });
    }

    private void persistInbound(String rawBody, String clientIp, String notifyTargetCode, String mid) {
        try {
            PgNotifyInbound in = new PgNotifyInbound();
            in.setRawBody(rawBody != null && rawBody.length() > 500_000
                    ? rawBody.substring(0, 500_000) + "...(truncated)" : rawBody);
            in.setContentType("application/json");
            in.setClientIp(clientIp);
            in.setNotifyChannelType("CALLBACK");
            in.setNotifyTargetCode(notifyTargetCode != null ? notifyTargetCode.trim() : "ILK");
            in.setProcessStatus("PARSED");
            in.setMid(mid);
            inboundPersistService.saveInbound(in);
        } catch (Exception e) {
            log.debug("ILK inbound 저장 생략: {}", e.getMessage());
        }
    }

    private Optional<IlkCredentials> resolveCredByMerchantId(String merchantId) {
        Optional<PgAgency> byCd = pgAgencyRepository.findByPgCd(PgVendor.ILK);
        if (merchantId == null || merchantId.isBlank()) {
            return byCd.map(IlkCredentials::from);
        }
        if (byCd.isPresent() && merchantId.equals(byCd.get().getMerchantMid())) {
            return byCd.map(IlkCredentials::from);
        }
        return pgAgencyRepository.findAllByOrderByPgCdAsc().stream()
                .filter(a -> PgVendor.isIlkFamily(a.getPgCd()))
                .filter(a -> merchantId.equals(a.getMerchantMid()))
                .findFirst()
                .map(IlkCredentials::from)
                .or(() -> byCd.map(IlkCredentials::from));
    }

    private String ackJson(String status, String mid, String siteId, String orderNo, IlkCredentials cred) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchantInformation.merchantId", mid != null ? mid : "");
        body.put("merchantInformation.merchantSiteId", siteId != null ? siteId : "");
        body.put("clientReferenceInformation.code", orderNo != null ? orderNo : "");
        body.put("status", status != null ? status : "DECLINED");
        if (cred != null && cred.isConfigured()) {
            Map<String, Object> withoutSign = new LinkedHashMap<>(body);
            body.put("sign", IlkCryptoUtil.signCompactJson(withoutSign, cred.seedKey()));
        } else {
            body.put("sign", "");
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return "{\"status\":\"" + status + "\"}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode root) {
        try {
            return objectMapper.convertValue(root, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static boolean looksLikeIlkJson(JsonNode root) {
        if (root == null || !root.isObject()) {
            return false;
        }
        return root.has("merchantInformation.merchantId")
                || root.has("clientReferenceInformation.code")
                || (root.has("merchantInformation") && root.has("clientReferenceInformation"));
    }

    private static String text(JsonNode root, String key) {
        if (root == null || key == null) {
            return "";
        }
        JsonNode n = root.get(key);
        if (n != null && !n.isNull() && !n.asText("").isBlank()) {
            return n.asText("").trim();
        }
        if (key.contains(".")) {
            JsonNode cur = root;
            for (String part : key.split("\\.")) {
                if (cur == null) {
                    return "";
                }
                cur = cur.get(part);
            }
            if (cur != null && !cur.isNull()) {
                return cur.asText("").trim();
            }
        }
        return "";
    }
}
