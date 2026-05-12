package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.repository.MerchantProfileRepository;
import com.pg.util.ChatbotProductPricingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * 챗봇 플랜 예약(다음 달 적용): 서울 달력 기준 적용 월에 도달하면 {@code chatbot_product_slot_limit_pending} 을 본 한도로 이관.
 */
@Service
public class ChatbotSlotPendingApplyService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotSlotPendingApplyService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final MerchantProfileRepository merchantProfileRepository;

    public ChatbotSlotPendingApplyService(MerchantProfileRepository merchantProfileRepository) {
        this.merchantProfileRepository = merchantProfileRepository;
    }

    @Scheduled(cron = "0 20 1 * * *", zone = "Asia/Seoul")
    @Transactional
    public void applyDuePendingChatbotSlotLimits() {
        YearMonth nowYm = YearMonth.now(SEOUL);
        List<MerchantProfile> list = merchantProfileRepository.findByChatbotProductSlotLimitPendingIsNotNull();
        int n = 0;
        for (MerchantProfile mp : list) {
            Integer pend = mp.getChatbotProductSlotLimitPending();
            String ymRaw = mp.getChatbotProductSlotPendingApplyYm();
            if (pend == null || pend <= 0 || ymRaw == null || ymRaw.isBlank()) {
                mp.setChatbotProductSlotLimitPending(null);
                mp.setChatbotProductSlotPendingApplyYm(null);
                merchantProfileRepository.save(mp);
                continue;
            }
            YearMonth applyYm;
            try {
                applyYm = YearMonth.parse(ymRaw.trim());
            } catch (Exception e) {
                log.warn("Skip chatbot pending slot: bad applyYm={} orgUnitId={}", ymRaw, mp.getOrgUnitId());
                mp.setChatbotProductSlotLimitPending(null);
                mp.setChatbotProductSlotPendingApplyYm(null);
                merchantProfileRepository.save(mp);
                continue;
            }
            if (nowYm.isBefore(applyYm)) {
                continue;
            }
            if (!ChatbotProductPricingUtil.isAllowedSlot(pend)) {
                mp.setChatbotProductSlotLimitPending(null);
                mp.setChatbotProductSlotPendingApplyYm(null);
                merchantProfileRepository.save(mp);
                continue;
            }
            mp.setChatbotProductSlotLimit(pend);
            mp.setChatbotProductSlotLimitPending(null);
            mp.setChatbotProductSlotPendingApplyYm(null);
            merchantProfileRepository.save(mp);
            n++;
        }
        if (n > 0) {
            log.info("Applied {} deferred chatbot plan reservation(s).", n);
        }
    }
}
