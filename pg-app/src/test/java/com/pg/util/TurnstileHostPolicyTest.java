package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnstileHostPolicyTest {

    @Test
    void trustedAdminHostsIncludeApexAndHqPortals() {
        assertTrue(TurnstileHostPolicy.isTrustedAdminHost("icopay.co.kr"));
        assertTrue(TurnstileHostPolicy.isTrustedAdminHost("www.icopay.co.kr"));
        assertTrue(TurnstileHostPolicy.isTrustedAdminHost("api.icopay.co.kr"));
        assertTrue(TurnstileHostPolicy.isTrustedAdminHost("hqth.icopay.co.kr"));
        assertTrue(TurnstileHostPolicy.isTrustedAdminHost("HQTH.ICOPAY.CO.KR"));
        assertTrue(TurnstileHostPolicy.isTrustedAdminHost("jpjp.icopay.co.kr"));
        assertTrue(TurnstileHostPolicy.isTrustedAdminHost("jp.icopay.co.kr"));
        assertTrue(TurnstileHostPolicy.isTrustedAdminHost("any-portal.icopay.co.kr"));
    }

    @Test
    void untrustedHostsRejected() {
        assertFalse(TurnstileHostPolicy.isTrustedAdminHost(null));
        assertFalse(TurnstileHostPolicy.isTrustedAdminHost(""));
        assertFalse(TurnstileHostPolicy.isTrustedAdminHost("evil.com"));
        assertFalse(TurnstileHostPolicy.isTrustedAdminHost("icopay.co.kr.evil.com"));
        assertFalse(TurnstileHostPolicy.isTrustedAdminHost("noticopay.co.kr"));
    }

    @Test
    void publicIpSkipsPrivateAndLoopback() {
        assertTrue(TurnstileHostPolicy.isPublicIp("203.0.113.10"));
        assertFalse(TurnstileHostPolicy.isPublicIp("127.0.0.1"));
        assertFalse(TurnstileHostPolicy.isPublicIp("10.0.0.8"));
        assertFalse(TurnstileHostPolicy.isPublicIp("192.168.0.1"));
        assertFalse(TurnstileHostPolicy.isPublicIp("172.16.1.2"));
        assertFalse(TurnstileHostPolicy.isPublicIp("::1"));
        assertFalse(TurnstileHostPolicy.isPublicIp(""));
    }
}
