package com.pg.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PgOutboundUrlPolicyTest {

    private static final String SAFE = "https://api.icopay.co.kr/noti/tok/cbJpay";

    @Test
    void keepsSameHostUrl() {
        String cand = "https://api.icopay.co.kr/noti/tok/cbJpay";
        assertEquals(cand, PgOutboundUrlPolicy.enforceOwnDomain(cand, SAFE, "https://api.icopay.co.kr"));
    }

    @Test
    void keepsSubdomainOfAllowedRootDomain() {
        String cand = "https://noti.icopay.co.kr/in/x";
        assertEquals(cand, PgOutboundUrlPolicy.enforceOwnDomain(cand, SAFE, "https://icopay.co.kr"));
    }

    @Test
    void replacesMerchantDomainWithSafeDefault() {
        String cand = "https://shop.merchant-example.com/webhook";
        assertEquals(SAFE, PgOutboundUrlPolicy.enforceOwnDomain(cand, SAFE, "https://api.icopay.co.kr"));
    }

    @Test
    void replacesLookalikeDomain() {
        // icopay.co.kr.evil.com 은 우리 도메인이 아니다.
        String cand = "https://api.icopay.co.kr.evil.com/webhook";
        assertEquals(SAFE, PgOutboundUrlPolicy.enforceOwnDomain(cand, SAFE, "https://icopay.co.kr"));
    }

    @Test
    void replacesBlankAndRelative() {
        assertEquals(SAFE, PgOutboundUrlPolicy.enforceOwnDomain("", SAFE, "https://api.icopay.co.kr"));
        assertEquals(SAFE, PgOutboundUrlPolicy.enforceOwnDomain(null, SAFE, "https://api.icopay.co.kr"));
        assertEquals(SAFE, PgOutboundUrlPolicy.enforceOwnDomain("/relative/path", SAFE, "https://api.icopay.co.kr"));
    }

    @Test
    void safeDefaultHostIsAlwaysAllowed() {
        // 허용 목록을 안 줘도 safeDefault 와 같은 호스트면 통과한다.
        String cand = "https://api.icopay.co.kr/x";
        assertEquals(cand, PgOutboundUrlPolicy.enforceOwnDomain(cand, SAFE));
    }

    @Test
    void isOwnDomainChecks() {
        assertTrue(PgOutboundUrlPolicy.isOwnDomain("https://api.icopay.co.kr/x", "https://icopay.co.kr"));
        assertFalse(PgOutboundUrlPolicy.isOwnDomain("https://evil.com/x", "https://icopay.co.kr"));
        assertFalse(PgOutboundUrlPolicy.isOwnDomain("not a url", "https://icopay.co.kr"));
    }
}
