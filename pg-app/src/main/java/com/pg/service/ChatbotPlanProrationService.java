package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.util.ChatbotProductPricingUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/**
 * 챗봇 상품등록 플랜 상향 시: 달력월(서울) 잔여일에 대해 (상위 플랜 일할 − 기존 플랜 일할)을 미수금으로 등록.
 * 정산·과금 기간(해당 월 말일)은 바꾸지 않습니다.
 */
@Service
public class ChatbotPlanProrationService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final HqChatbotAiSettingsService hqChatbotAiSettingsService;
    private final ChatbotProductMonthlyBillingService chatbotProductMonthlyBillingService;
    private final SettlementArrearsService settlementArrearsService;
    private final MerchantReceivableRepository merchantReceivableRepository;

    public ChatbotPlanProrationService(HqChatbotAiSettingsService hqChatbotAiSettingsService,
                                       ChatbotProductMonthlyBillingService chatbotProductMonthlyBillingService,
                                       SettlementArrearsService settlementArrearsService,
                                       MerchantReceivableRepository merchantReceivableRepository) {
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.chatbotProductMonthlyBillingService = chatbotProductMonthlyBillingService;
        this.settlementArrearsService = settlementArrearsService;
        this.merchantReceivableRepository = merchantReceivableRepository;
    }

    /**
     * @param merchantCode 가맹점 코드(compId)
     * @param oldSlot 이전 플랜(건), null·0 이면 무제한으로 간주하여 일할 차감 없음
     * @param newSlot 새 플랜(건), oldSlot 보다 커야 함
     */
    @Transactional
    public void postUpgradeDeltaIfNeeded(String merchantCode, MerchantProfile mp, Integer oldSlot, int newSlot) {
        if (merchantCode == null || merchantCode.isBlank() || mp == null || mp.getOrgUnitId() == null) {
            return;
        }
        int old = oldSlot != null ? oldSlot : 0;
        if (newSlot <= old || old <= 0) {
            return;
        }
        if (!ChatbotProductPricingUtil.isAllowedSlot(old) || !ChatbotProductPricingUtil.isAllowedSlot(newSlot)) {
            return;
        }
        LocalDate today = LocalDate.now(SEOUL);
        int dim = today.lengthOfMonth();
        int dom = today.getDayOfMonth();
        int remainingDays = dim - dom;
        if (remainingDays <= 0) {
            return;
        }
        String billCcy = chatbotProductMonthlyBillingService.resolveChatbotMonthlyBillingCurrency(mp.getOrgUnitId());
        if (billCcy == null || !ChatbotProductPricingUtil.isSupportedBillingCurrency(billCcy)) {
            return;
        }
        Map<String, Object> hq = hqChatbotAiSettingsService.rawConfigForServerUse();
        BigDecimal feeOld = ChatbotProductPricingUtil.monthlyFeeForSlotAndCurrency(hq, old, billCcy);
        BigDecimal feeNew = ChatbotProductPricingUtil.monthlyFeeForSlotAndCurrency(hq, newSlot, billCcy);
        if (feeNew == null || feeNew.compareTo(feeOld == null ? BigDecimal.ZERO : feeOld) <= 0) {
            return;
        }
        BigDecimal oldPart = scaleMoney(prorate(feeOld, remainingDays, dim));
        BigDecimal newPart = scaleMoney(prorate(feeNew, remainingDays, dim));
        BigDecimal delta = newPart.subtract(oldPart);
        if (delta.signum() <= 0) {
            return;
        }
        String mid = merchantCode.trim();
        String memo = ChatbotProductPricingUtil.memoKeyForPlanUpgrade(today, old, newSlot);
        if (merchantReceivableRepository.existsByMerchantIdAndReasonCodeAndMemo(
                mid, ChatbotProductPricingUtil.RECEIVABLE_REASON_CHATBOT_UPGRADE, memo)) {
            return;
        }
        String title = "챗봇 플랜 업그레이드 차액 " + today.getYear() + "-" + String.format("%02d", today.getMonthValue())
                + " (" + old + "→" + newSlot + "건·잔여" + remainingDays + "일·" + billCcy + ")";
        settlementArrearsService.createReceivable(mid, delta, title,
                ChatbotProductPricingUtil.RECEIVABLE_REASON_CHATBOT_UPGRADE, memo, "SYSTEM", billCcy);
    }

    private static BigDecimal prorate(BigDecimal monthlyFee, int remainingDays, int daysInMonth) {
        BigDecimal fee = monthlyFee != null ? monthlyFee : BigDecimal.ZERO;
        if (fee.signum() <= 0 || remainingDays <= 0 || daysInMonth <= 0) {
            return BigDecimal.ZERO;
        }
        return fee.multiply(BigDecimal.valueOf(remainingDays))
                .divide(BigDecimal.valueOf(daysInMonth), 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleMoney(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
