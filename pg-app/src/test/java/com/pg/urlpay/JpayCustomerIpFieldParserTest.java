package com.pg.urlpay;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpayCustomerIpFieldParserTest {

    @Test
    void parseIpAndLocationSuffix() {
        Optional<JpayCustomerIpFieldParser.ParsedCustomerIp> p =
                JpayCustomerIpFieldParser.parse("103.5.140.132|日本-千葉縣");
        assertTrue(p.isPresent());
        assertEquals("103.5.140.132", p.get().ip());
        assertEquals("日本-千葉縣", p.get().locationSuffix());
    }

    @Test
    void parseIpOnly() {
        Optional<JpayCustomerIpFieldParser.ParsedCustomerIp> p =
                JpayCustomerIpFieldParser.parse("60.239.74.124");
        assertTrue(p.isPresent());
        assertEquals("60.239.74.124", p.get().ip());
        assertEquals("", p.get().locationSuffix());
    }
}
