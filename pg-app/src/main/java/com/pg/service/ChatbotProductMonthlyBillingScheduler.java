package com.pg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매월 1일 새벽 서울 시간 — 전월 챗봇 상품등록 플랜 이용료 미수금 자동등록.
 * {@code app.chatbot.monthlyBillingEnabled=false} 로 끕니다.
 */
@Component
@ConditionalOnProperty(name = "app.chatbot.monthlyBillingEnabled", havingValue = "true", matchIfMissing = true)
public class ChatbotProductMonthlyBillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChatbotProductMonthlyBillingScheduler.class);

    private final ChatbotProductMonthlyBillingService chatbotProductMonthlyBillingService;

    public ChatbotProductMonthlyBillingScheduler(ChatbotProductMonthlyBillingService chatbotProductMonthlyBillingService) {
        this.chatbotProductMonthlyBillingService = chatbotProductMonthlyBillingService;
    }

    @Scheduled(cron = "${app.chatbot.monthlyBillingCron:0 45 7 1 * *}", zone = "Asia/Seoul")
    public void monthlyChatbotFees() {
        try {
            chatbotProductMonthlyBillingService.runScheduledPreviousMonthBilling();
        } catch (Exception e) {
            log.warn("Chatbot monthly billing scheduler failed: {}", e.getMessage());
        }
    }
}
