package com.pg.service;

import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerSmtpMailServiceTest {

    @Test
    void normalizeBareEmail_plain() {
        assertEquals("noreply@icopay.co.kr", LedgerSmtpMailService.normalizeBareEmail("noreply@icopay.co.kr"));
    }

    @Test
    void normalizeBareEmail_rfc822Display() {
        assertEquals("noreply@icopay.co.kr",
                LedgerSmtpMailService.normalizeBareEmail("ICOPAY Japan <noreply@icopay.co.kr>"));
    }

    @Test
    void buildFromAddress_japaneseDisplayName() throws Exception {
        InternetAddress from = LedgerSmtpMailService.buildFromAddress(
                "noreply@icopay.co.kr", "ICOPAY 日本");
        assertEquals("noreply@icopay.co.kr", from.getAddress());
        assertTrue(from.getPersonal().contains("日本"));
    }

    @Test
    void buildFromAddress_addressFieldContainsDisplayWrapper() throws Exception {
        InternetAddress from = LedgerSmtpMailService.buildFromAddress(
                "On the Line <mail@jpjp.icopay.co.kr>", "");
        assertEquals("mail@jpjp.icopay.co.kr", from.getAddress());
    }

    @Test
    void sanitizeDisplayName_stripsAngleBrackets() {
        assertEquals("ICOPAY()", LedgerSmtpMailService.sanitizeDisplayName("ICOPAY<>"));
    }
}
