package com.pg.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TurnstileVerificationServiceTest {

    @Test
    void requestHostPrefersOriginThenForwardedHost() {
        MockHttpServletRequest originReq = new MockHttpServletRequest();
        originReq.addHeader("Origin", "https://hqth.icopay.co.kr");
        originReq.addHeader("Host", "api.icopay.co.kr");
        assertEquals("hqth.icopay.co.kr", TurnstileVerificationService.requestHost(originReq));

        MockHttpServletRequest xfReq = new MockHttpServletRequest();
        xfReq.addHeader("X-Forwarded-Host", "jpjp.icopay.co.kr, api.icopay.co.kr");
        xfReq.addHeader("Host", "127.0.0.1:8080");
        assertEquals("jpjp.icopay.co.kr", TurnstileVerificationService.requestHost(xfReq));
    }
}
