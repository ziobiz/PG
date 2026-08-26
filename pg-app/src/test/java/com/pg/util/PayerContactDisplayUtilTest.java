package com.pg.util;

import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayerContactDisplayUtilTest {

    @Test
    void chillCustomerShowsEmailNotGuest() {
        PgTrnsctn t = new PgTrnsctn();
        t.setCustomerId("guest");
        t.setCustomerNm("諒 武藤");
        assertEquals("諒 武藤", PayerContactDisplayUtil.formatChillCustomer(t));

        t.setCustomerId("r1030.m2003@gmail.com");
        assertEquals("r1030.m2003@gmail.com | 諒 武藤", PayerContactDisplayUtil.formatChillCustomer(t));
    }

    @Test
    void applyEmailOverwritesGuestNotPlaceholder() {
        PgTrnsctn t = new PgTrnsctn();
        t.setCustomerId("guest");
        PayerContactDisplayUtil.applyEmailIfUsable(t, "noreply@icopay.co.kr", 100);
        assertEquals("guest", t.getCustomerId());
        PayerContactDisplayUtil.applyEmailIfUsable(t, "buyer@example.com", 100);
        assertEquals("buyer@example.com", t.getCustomerId());
        PayerContactDisplayUtil.applyEmailIfUsable(t, "other@example.com", 100);
        assertEquals("buyer@example.com", t.getCustomerId());
    }
}
