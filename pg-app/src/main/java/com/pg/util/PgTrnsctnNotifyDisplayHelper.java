package com.pg.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.pg.entity.PgTrnsctn;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

/**
 * ChillPay 노티 JSON에서 고객금액·고객통화를 읽어 {@link PgTrnsctn}에 반영합니다.
 * PG 청구 필드({@code cur_type}/{@code amt_krw})와 별도 키가 올 때만 설정합니다.
 */
public final class PgTrnsctnNotifyDisplayHelper {

    private PgTrnsctnNotifyDisplayHelper() {
    }

    /**
     * 노티 본문에 DisplayAmount·DisplayCurrency 계열이 있으면 설정합니다.
     * 키가 없으면 변경하지 않습니다.
     */
    public static void mergeFromChillPayJson(JsonNode root, PgTrnsctn t) {
        if (root == null || !root.isObject() || t == null) {
            return;
        }
        String amtRaw = firstNonBlankDeep(root,
                "DisplayAmount", "displayAmount",
                "ShopperAmount", "shopperAmount",
                "CustomerFacingAmount", "customerFacingAmount");
        if (amtRaw == null || amtRaw.isBlank()) {
            return;
        }
        Optional<BigDecimal> amt = NotifyAmountParse.parsePlain(amtRaw);
        if (!NotifyAmountParse.isPositive(amt)) {
            return;
        }
        String curRaw = firstNonBlankDeep(root,
                "DisplayCurrency", "displayCurrency",
                "ShopperCurrency", "shopperCurrency",
                "OrderDisplayCurrency", "orderDisplayCurrency");
        if (curRaw == null || curRaw.isBlank()) {
            return;
        }
        String cur = curRaw.trim().toUpperCase(Locale.ROOT);
        if (cur.length() > 10) {
            cur = cur.substring(0, 10);
        }
        t.setDisplayAmt(amt.get());
        t.setDisplayCurType(cur);
    }

    private static String firstNonBlankDeep(JsonNode root, String... names) {
        for (String n : names) {
            String v = textDeep(root, n);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static String textDeep(JsonNode root, String name) {
        String t = textAt(root, name);
        if (t != null) {
            return t;
        }
        JsonNode d = root.get("data");
        if (d != null && d.isObject()) {
            return textAt(d, name);
        }
        return null;
    }

    private static String textAt(JsonNode n, String field) {
        if (n == null || !n.isObject()) {
            return null;
        }
        JsonNode x = n.get(field);
        if (x == null || x.isNull()) {
            return null;
        }
        if (x.isTextual()) {
            String s = x.asText().trim();
            return s.isEmpty() ? null : s;
        }
        if (x.isNumber()) {
            return x.asText();
        }
        return null;
    }
}
