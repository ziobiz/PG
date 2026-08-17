package com.pg.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElementPay Callback {@code order} + LightAPI {@code data.merchantOrder} 후보.
 * Tidem LightAPI pending check 는 {@code order} 를 시스템 값으로 두고
 * 가맹 주문번호는 {@code data} JSON 의 {@code merchantOrder} 에 넣는 경우가 있다.
 */
public final class ElementPayCallbackOrderUtil {

    private static final Pattern MERCHANT_ORDER_JSON = Pattern.compile(
            "\"merchantOrder\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern MERCHANT_ORDER_QS = Pattern.compile(
            "(?:^|[?&])merchantOrder=([^&]*)", Pattern.CASE_INSENSITIVE);

    private ElementPayCallbackOrderUtil() {
    }

    public static List<String> orderCandidates(String orderNo, Map<String, String> fields) {
        Set<String> ids = new LinkedHashSet<>();
        addIfPresent(ids, orderNo);
        if (fields != null) {
            ids.addAll(extractMerchantOrders(fields.get("data")));
            addIfPresent(ids, fields.get("merchant_order"));
            addIfPresent(ids, fields.get("merchantOrder"));
        }
        return new ArrayList<>(ids);
    }

    public static List<String> extractMerchantOrders(String data) {
        Set<String> ids = new LinkedHashSet<>();
        if (data == null || data.isBlank()) {
            return List.of();
        }
        String decoded = decodeRepeated(data.trim());
        Matcher json = MERCHANT_ORDER_JSON.matcher(decoded);
        while (json.find()) {
            addIfPresent(ids, json.group(1));
        }
        Matcher qs = MERCHANT_ORDER_QS.matcher(decoded);
        while (qs.find()) {
            addIfPresent(ids, decodeOnce(qs.group(1)));
        }
        if (ids.isEmpty() && decoded.matches("[A-Za-z0-9_.-]{4,64}")) {
            addIfPresent(ids, decoded);
        }
        return new ArrayList<>(ids);
    }

    public static boolean matchesLocalOrder(String localOrder, List<String> candidates) {
        if (localOrder == null || localOrder.isBlank() || candidates == null || candidates.isEmpty()) {
            return false;
        }
        String local = localOrder.trim();
        for (String c : candidates) {
            if (local.equals(c)) {
                return true;
            }
        }
        return false;
    }

    private static String decodeRepeated(String raw) {
        String s = raw;
        for (int i = 0; i < 2; i++) {
            String next = decodeOnce(s);
            if (next.equals(s)) {
                break;
            }
            s = next;
        }
        return s;
    }

    private static String decodeOnce(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return raw;
        }
    }

    private static void addIfPresent(Set<String> ids, String v) {
        if (v == null) {
            return;
        }
        String t = v.trim();
        if (!t.isBlank()) {
            ids.add(t);
        }
    }
}
