package com.pg.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementPayWebhookIpUtilTest {

    @Test
    void normalize_stripsIpv4MappedAndPort() {
        assertEquals("3.0.36.253", ElementPayWebhookIpUtil.normalize("::ffff:3.0.36.253"));
        assertEquals("3.1.29.20", ElementPayWebhookIpUtil.normalize("3.1.29.20:443"));
        assertEquals("3.0.36.253", ElementPayWebhookIpUtil.normalize("[3.0.36.253]"));
    }

    @Test
    void isKnownCabinetIp_sandboxAndLive() {
        assertTrue(ElementPayWebhookIpUtil.isKnownCabinetIp(ElementPayWebhookIpUtil.SANDBOX_IP));
        assertTrue(ElementPayWebhookIpUtil.isKnownCabinetIp(ElementPayWebhookIpUtil.LIVE_IP));
        assertFalse(ElementPayWebhookIpUtil.isKnownCabinetIp("8.8.8.8"));
        assertFalse(ElementPayWebhookIpUtil.isKnownCabinetIp(""));
    }

    @Test
    void extractAndAudit_fromHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(ElementPayWebhookIpUtil.SOURCE_IP_HEADER, ElementPayWebhookIpUtil.SANDBOX_IP);
        ElementPayWebhookIpUtil.AuditResult a = ElementPayWebhookIpUtil.audit(req, "203.0.113.10");
        assertEquals(ElementPayWebhookIpUtil.SANDBOX_IP, a.elementPaySourceIp());
        assertTrue(a.knownCabinetIp());
        assertFalse(a.headerMissing());
        assertEquals(
                "3.0.36.253 via 203.0.113.10",
                ElementPayWebhookIpUtil.composeInboundClientIp(a.elementPaySourceIp(), a.notiPeerIp()));
    }

    @Test
    void audit_missingHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        ElementPayWebhookIpUtil.AuditResult a = ElementPayWebhookIpUtil.audit(req, "203.0.113.10");
        assertTrue(a.headerMissing());
        assertEquals("203.0.113.10", ElementPayWebhookIpUtil.composeInboundClientIp("", "203.0.113.10"));
    }
}
