package com.pg.splitpay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitPayContractChannelTest {

    @Test
    void resolveContractChannel_chatbotEntry() {
        assertEquals("CHATBOT", SplitPayContractService.resolveContractChannel("chatbot"));
        assertEquals("URL", SplitPayContractService.resolveContractChannel(null));
    }

    @Test
    void appendChatbotPayEntry_onlyForChatbotChannel() {
        String url = "https://pay.example/split-pay.html?token=abc";
        assertEquals(url + "&entry=chatbot",
                SplitPayContractService.appendChatbotPayEntryIfNeeded(url, "CHATBOT"));
        assertEquals(url, SplitPayContractService.appendChatbotPayEntryIfNeeded(url, "URL"));
    }

    @Test
    void appendChatbotPayEntry_idempotent() {
        String url = "https://pay.example/pay.html?m=X&entry=chatbot";
        assertTrue(SplitPayContractService.appendChatbotPayEntryIfNeeded(url, "CHATBOT").contains("entry=chatbot"));
        assertEquals(url, SplitPayContractService.appendChatbotPayEntryIfNeeded(url, "CHATBOT"));
    }
}
