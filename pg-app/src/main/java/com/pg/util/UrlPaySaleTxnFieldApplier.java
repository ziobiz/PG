package com.pg.util;

import com.pg.entity.PgTrnsctn;

import java.util.Locale;
import java.util.Map;

/**
 * URL 결제 요청 본문 → {@link PgTrnsctn} 고객명·마스킹 카드·위치·단말기.
 * JPAY 적재와 동일 필드를 다른 운영 PG(ElementPay 등)에도 적용한다.
 */
public final class UrlPaySaleTxnFieldApplier {

    private UrlPaySaleTxnFieldApplier() {
    }

    public static void apply(PgTrnsctn t, Map<String, Object> saleBody) {
        if (t == null || saleBody == null || saleBody.isEmpty()) {
            return;
        }
        JpayBuyerContactApplier.applyFromSaleBody(t, saleBody);
        applyCardPanHash(t, saleBody);
        applyPayerContext(t, saleBody);
    }

    /** URL 웹 결제인데 UA가 없으면 PC 로 둔다(그리드 단말기가 '-' 가 되지 않게). */
    public static void ensureUrlWebDevice(PgTrnsctn t, String txnOrigin) {
        if (t == null) {
            return;
        }
        String origin = txnOrigin != null ? txnOrigin.trim().toUpperCase(Locale.ROOT) : "";
        if (!"URL".equals(origin) && !"MERCHANT_API".equals(origin) && !"CHATBOT".equals(origin)) {
            origin = t.getOrigin() != null ? t.getOrigin().trim().toUpperCase(Locale.ROOT) : "";
        }
        if (!"URL".equals(origin) && !"MERCHANT_API".equals(origin) && !"CHATBOT".equals(origin)) {
            return;
        }
        String d = t.getPayerDeviceCategory();
        if (d == null || d.isBlank() || PayerDeviceCategoryUtil.UNKNOWN.equalsIgnoreCase(d)) {
            t.setPayerDeviceCategory(PayerDeviceCategoryUtil.PC);
        }
    }

    private static void applyCardPanHash(PgTrnsctn t, Map<String, Object> body) {
        Object raw = body.get("payCardno");
        if (raw == null) {
            raw = body.get("pay_cardno");
        }
        if (raw == null) {
            return;
        }
        String pan = PayCardBrandDetector.normalizePan(raw.toString());
        if (pan.length() >= 10) {
            t.setCardPanHash(PayCardPanHashUtil.hashPan(pan));
        }
    }

    private static void applyPayerContext(PgTrnsctn t, Map<String, Object> body) {
        Object ip = body.get("_payerClientIp");
        if (ip != null && !ip.toString().isBlank()) {
            String v = ip.toString().trim();
            t.setPayerClientIp(v.length() > 64 ? v.substring(0, 64) : v);
        }
        Object dev = body.get("_payerDeviceCategory");
        if (dev != null && !dev.toString().isBlank()) {
            String v = dev.toString().trim().toUpperCase(Locale.ROOT);
            t.setPayerDeviceCategory(v.length() > 32 ? v.substring(0, 32) : v);
        }
        Object iso = body.get("_payerCountryIso2");
        if (iso == null || iso.toString().isBlank()) {
            iso = body.get("payCountryIsoCode2");
        }
        if (iso != null && !iso.toString().isBlank()) {
            String v = PayerCountryIso2Util.normalize(iso.toString());
            if (!v.isBlank()) {
                t.setPayerCountryIso2(v);
            }
        }
        Object city = body.get("_payerCity");
        if (city == null || city.toString().isBlank()) {
            city = body.get("payCity");
        }
        if (city == null || city.toString().isBlank()) {
            city = body.get("pay_city");
        }
        if (city != null && !city.toString().isBlank()) {
            String v = city.toString().trim();
            t.setPayerCity(v.length() > 128 ? v.substring(0, 128) : v);
        }
    }
}
