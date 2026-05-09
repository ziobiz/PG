package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.util.ChatbotProductPricingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 챗봇 상품 등록 플랜 월 이용료를 미수금으로 등록(후불·정산 시 FIFO 차감).
 */
@Service
public class ChatbotProductMonthlyBillingService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotProductMonthlyBillingService.class);

    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final HqChatbotAiSettingsService hqChatbotAiSettingsService;
    private final SettlementArrearsService settlementArrearsService;

    public ChatbotProductMonthlyBillingService(MerchantProfileRepository merchantProfileRepository,
                                               OrgUnitRepository orgUnitRepository,
                                               MerchantReceivableRepository merchantReceivableRepository,
                                               HqChatbotAiSettingsService hqChatbotAiSettingsService,
                                               SettlementArrearsService settlementArrearsService) {
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.settlementArrearsService = settlementArrearsService;
    }

    /** 전월(기준 존) 이용료 미수금 — 매월 1회 스케줄에서 호출 */
    public void runScheduledPreviousMonthBilling() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        YearMonth ym = YearMonth.now(zone).minusMonths(1);
        int n = runForMonth(ym);
        log.info("Chatbot monthly billing for {} posted {} merchant receivable(s).", ym, n);
    }

    /**
     * @return 신규 등록된 미수금 건 수
     */
    @Transactional
    public int runForMonth(YearMonth ym) {
        Map<String, Object> hq = hqChatbotAiSettingsService.rawConfigForServerUse();
        List<MerchantProfile> rows = merchantProfileRepository.findProfilesForChatbotMonthlyBilling();
        String memoKey = ChatbotProductPricingUtil.memoKeyForBillingMonth(ym);
        int created = 0;
        for (MerchantProfile mp : rows) {
            Integer slot = mp.getChatbotProductSlotLimit();
            if (slot == null || slot <= 0) {
                continue;
            }
            OrgUnit ou = orgUnitRepository.findById(mp.getOrgUnitId()).orElse(null);
            if (ou == null || ou.getCode() == null || ou.getCode().isBlank()) {
                continue;
            }
            String billCcy = resolveChatbotMonthlyBillingCurrency(mp.getOrgUnitId());
            if (billCcy == null || !ChatbotProductPricingUtil.isSupportedBillingCurrency(billCcy)) {
                log.warn("Chatbot monthly billing skip: unsupported or missing billing currency for merchantOrg={} slot={} (use 총판·가맹 base_currency among JPY,KRW,USD,CNY,THB)",
                        mp.getOrgUnitId(), slot);
                continue;
            }
            BigDecimal fee = ChatbotProductPricingUtil.monthlyFeeForSlotAndCurrency(hq, slot, billCcy);
            if (fee == null || fee.signum() <= 0) {
                continue;
            }
            String mid = ou.getCode().trim();
            if (merchantReceivableRepository.existsByMerchantIdAndReasonCodeAndMemo(
                    mid, ChatbotProductPricingUtil.RECEIVABLE_REASON_CHATBOT_MONTHLY, memoKey)) {
                continue;
            }
            String title = "챗봇 상품등록 월이용료 " + ym + " (" + slot + "건·" + billCcy + ")";
            settlementArrearsService.createReceivable(mid, fee, title,
                    ChatbotProductPricingUtil.RECEIVABLE_REASON_CHATBOT_MONTHLY, memoKey, "SYSTEM");
            created++;
        }
        return created;
    }

    /**
     * 소속 총판(MASTER_DIST) {@code base_currency} 첫 통화 우선; 없으면 가맹점 프로필 첫 통화.
     */
    private String resolveChatbotMonthlyBillingCurrency(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return null;
        }
        Optional<Long> mdId = findNearestMasterDistAncestorId(merchantOrgUnitId);
        if (mdId.isPresent()) {
            String fromMd = merchantProfileRepository.findByOrgUnitId(mdId.get())
                    .map(MerchantProfile::getBaseCurrency)
                    .map(ChatbotProductPricingUtil::firstIsoCurrencyToken)
                    .orElse(null);
            if (ChatbotProductPricingUtil.isSupportedBillingCurrency(fromMd)) {
                return fromMd;
            }
        }
        return merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId)
                .map(MerchantProfile::getBaseCurrency)
                .map(ChatbotProductPricingUtil::firstIsoCurrencyToken)
                .filter(ChatbotProductPricingUtil::isSupportedBillingCurrency)
                .orElse(null);
    }

    /** 상위 체인에서 가장 가까운 총판 — {@link CompService} 와 동일 규칙 */
    private Optional<Long> findNearestMasterDistAncestorId(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        Long cur = orgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            OrgUnit ou = orgUnitRepository.findById(cur).orElse(null);
            if (ou == null) {
                break;
            }
            if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
                return Optional.of(ou.getId());
            }
            cur = ou.getParentId();
        }
        return Optional.empty();
    }
}
