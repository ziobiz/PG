package com.pg.util;

import com.pg.entity.PgTrnsctn;

/**
 * ElementPay 거래의 SYSTEM payment_id.
 * initRefund·getStatus 는 이 값이 있어야 한다.
 */
public final class ElementPayPaymentIdUtil {

    private ElementPayPaymentIdUtil() {
    }

    public static String fromTxn(PgTrnsctn t) {
        if (t == null) {
            return "";
        }
        String fromChill = digitsOrRaw(t.getChillTransactionId());
        if (!fromChill.isBlank()) {
            return fromChill;
        }
        return digitsOrRaw(t.getApprovalNo());
    }

    public static String fromCallbackFields(java.util.Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        /* refund.* 웹훅의 id 는 환불건 ID. 결제 ID 는 payment_id. pay/check 는 id 만 오는 경우가 많다. */
        String id = first(fields, "payment_id", "paymentid", "id");
        return id == null ? "" : id.trim();
    }

    private static String first(java.util.Map<String, String> fields, String... keys) {
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            String v = fields.get(k);
            if (v == null) {
                v = fields.get(k.toLowerCase(java.util.Locale.ROOT));
            }
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String digitsOrRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        String digits = s.replaceAll("\\D", "");
        if (digits.length() >= 4 && digits.length() <= 20) {
            return digits;
        }
        return s.length() > 64 ? s.substring(0, 64) : s;
    }
}
