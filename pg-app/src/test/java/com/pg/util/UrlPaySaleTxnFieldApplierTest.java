package com.pg.util;

import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlPaySaleTxnFieldApplierTest {

    @Test
    void appliesNameCardCountryDeviceLikeJpay() {
        PgTrnsctn t = new PgTrnsctn();
        t.setOrigin("URL");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payFirstname", "Hong");
        body.put("payLastname", "Gildong");
        body.put("payEmailAddress", "buyer@example.com");
        body.put("payTelephone", "1012345678");
        body.put("payCardno", "4205200012343714");
        body.put("payCountryIsoCode2", "KR");
        body.put("_payerDeviceCategory", "MOBILE_ANDROID");
        body.put("_payerClientIp", "203.0.113.10");
        body.put("_payerCountryIso2", "KR");
        UrlPaySaleTxnFieldApplier.apply(t, body);
        UrlPaySaleTxnFieldApplier.ensureUrlWebDevice(t, "URL");
        assertEquals("Hong Gildong", t.getCustomerNm());
        assertEquals("buyer@example.com", t.getCustomerId());
        assertEquals("420520***3714", t.getCardPanDisplay());
        assertEquals("KR", t.getPayerCountryIso2());
        assertEquals("MOBILE_ANDROID", t.getPayerDeviceCategory());
        assertEquals("203.0.113.10", t.getPayerClientIp());
        assertNotNull(t.getCardPanHash());
        assertTrue(t.getCardPanHash().length() >= 32);
        assertEquals("buyer@example.com | Hong Gildong", PayerContactDisplayUtil.formatChillCustomer(t));
    }

    @Test
    void urlWebWithoutUaDefaultsToPc() {
        PgTrnsctn t = new PgTrnsctn();
        t.setOrigin("URL");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payEmailAddress", "a@b.c");
        UrlPaySaleTxnFieldApplier.apply(t, body);
        UrlPaySaleTxnFieldApplier.ensureUrlWebDevice(t, "URL");
        assertEquals(PayerDeviceCategoryUtil.PC, t.getPayerDeviceCategory());
    }
}
