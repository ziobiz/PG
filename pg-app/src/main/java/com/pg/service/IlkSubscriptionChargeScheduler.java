package com.pg.service;

import com.pg.entity.MerchantIlkSubscription;
import com.pg.integration.pg.ilk.IlkCredentials;
import com.pg.integration.pg.ilk.IlkCryptoUtil;
import com.pg.repository.MerchantIlkSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ILK 구독 MIT 회차 청구 — {@code next_charge_at} 도래 건을 주기적으로 Payment(cofType=MIT).
 */
@Component
public class IlkSubscriptionChargeScheduler {

    private static final Logger log = LoggerFactory.getLogger(IlkSubscriptionChargeScheduler.class);

    private final MerchantIlkSubscriptionRepository subscriptionRepository;
    private final IlkPaymentService ilkPaymentService;

    public IlkSubscriptionChargeScheduler(MerchantIlkSubscriptionRepository subscriptionRepository,
                                          IlkPaymentService ilkPaymentService) {
        this.subscriptionRepository = subscriptionRepository;
        this.ilkPaymentService = ilkPaymentService;
    }

    @Scheduled(cron = "${app.ilk.subscriptionCharge.cron:0 */3 * * * *}", zone = "Asia/Seoul")
    public void chargeDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<MerchantIlkSubscription> due = subscriptionRepository
                .findTop50ByStatusAndNextChargeAtLessThanEqualOrderByNextChargeAtAsc(
                        MerchantIlkSubscription.STATUS_ACTIVE, now);
        for (MerchantIlkSubscription sub : due) {
            try {
                chargeOne(sub);
            } catch (Exception e) {
                log.warn("ILK MIT 청구 실패 subId={}: {}", sub.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    protected void chargeOne(MerchantIlkSubscription sub) {
        if (sub.getCardTokenEnc() == null || sub.getCardTokenEnc().isBlank()
                || sub.getCardExpMonthEnc() == null || sub.getCardExpYearEnc() == null) {
            log.warn("ILK MIT 스킵(카드 시드 없음) subId={}", sub.getId());
            sub.setNextChargeAt(LocalDateTime.now().plusDays(1));
            subscriptionRepository.save(sub);
            return;
        }
        Optional<IlkCredentials> credOpt = ilkPaymentService.resolveCredentials(sub.getOrgUnitId());
        if (credOpt.isEmpty() || !credOpt.get().isConfigured()) {
            log.warn("ILK MIT 스킵(자격증명 없음) org={}", sub.getOrgUnitId());
            return;
        }
        IlkCredentials cred = credOpt.get();
        String pan;
        String expM;
        String expY;
        try {
            pan = IlkCryptoUtil.decryptAesBase64(sub.getCardTokenEnc(), cred.seedKey(), cred.seedIv());
            expM = IlkCryptoUtil.decryptAesBase64(sub.getCardExpMonthEnc(), cred.seedKey(), cred.seedIv());
            expY = IlkCryptoUtil.decryptAesBase64(sub.getCardExpYearEnc(), cred.seedKey(), cred.seedIv());
        } catch (Exception e) {
            log.warn("ILK MIT 카드 복호화 실패 subId={}: {}", sub.getId(), e.getMessage());
            return;
        }
        int nextCount = (sub.getChargeCount() != null ? sub.getChargeCount() : 0) + 1;
        String orderNo = clamp(sub.getSubscriptionNo() + "-R" + nextCount, 32);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderNo", orderNo);
        body.put("amount", sub.getAmount() != null ? sub.getAmount().toPlainString() : "0");
        body.put("currency", sub.getCurrency() != null ? sub.getCurrency() : "KRW");
        body.put("productName", "Subscription renewal");
        body.put("cardNo", pan);
        body.put("cardMonth", expM);
        body.put("cardYear", expY);
        body.put("cardBrand", sub.getCardBrand());
        body.put("buyerName", "SUBSCRIBER");
        body.put("buyerEmail", "noreply@icopay.co.kr");
        body.put("txnOrigin", "SUB_MIT");
        body.put("clientIp", "0.0.0.0");
        if (sub.getFirstOrderNo() != null && !sub.getFirstOrderNo().isBlank()) {
            body.put("firstOrderNo", sub.getFirstOrderNo());
        }
        if (sub.getFirstAuthId() != null && !sub.getFirstAuthId().isBlank()) {
            body.put("firstAuthId", sub.getFirstAuthId());
        }

        Map<String, Object> result = ilkPaymentService.executeSubscriptionMitCharge(sub.getOrgUnitId(), body);
        boolean ok = Boolean.TRUE.equals(result.get("success"))
                && "SUCCESS".equalsIgnoreCase(String.valueOf(result.get("status")));
        if (ok) {
            sub.setChargeCount(nextCount);
            sub.setLastChargeAt(LocalDateTime.now());
            sub.setNextChargeAt(LocalDateTime.now().plusDays(resolveIntervalDays(sub)));
            // firstAuthId 는 초회 CIT 승인 ID 유지(매뉴얼상 MIT 필수 체인 필드는 별도 명시 없음)
            subscriptionRepository.save(sub);
            log.info("ILK MIT 성공 subId={} orderNo={} ilkId={}", sub.getId(), orderNo, result.get("id"));
        } else {
            log.warn("ILK MIT 거절 subId={} msg={}", sub.getId(), result.get("message"));
            sub.setNextChargeAt(LocalDateTime.now().plusHours(6));
            subscriptionRepository.save(sub);
        }
    }

    private static int resolveIntervalDays(MerchantIlkSubscription sub) {
        String plan = sub.getPlanJson();
        if (plan != null && plan.contains("\"intervalDays\"")) {
            try {
                int i = plan.indexOf("\"intervalDays\"");
                String tail = plan.substring(i);
                String num = tail.replaceAll("[^0-9]", " ").trim().split("\\s+")[0];
                int d = Integer.parseInt(num);
                if (d > 0 && d < 400) {
                    return d;
                }
            } catch (Exception ignored) {
            }
        }
        return 30;
    }

    private static String clamp(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
