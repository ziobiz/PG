package com.pg.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Eximbay 결제수단(UI 키) → Eximbay {@code payment_method}(paymethod) 코드 매핑.
 *
 * <p>ICOPAY 기본 노출: 신용카드({@code P000}) · PayPay · 일본 편의점·은행({@code P006}) · UnionPay({@code P002}).
 * 코드는 Eximbay 연동 가이드 Appendix C 기준이며, 가맹 온보딩별로 다를 수 있어
 * {@code tb_pg_agency.credentials_extra_json.eximbayMethodCodes}(JSON: UI키→코드) 로 오버라이드한다.
 */
public final class EximbayPaymentMethodCatalog {

    public static final String KEY_CARD = "CARD";
    public static final String KEY_PAYPAY = "PAYPAY";
    /** 일본 편의점·인터넷뱅킹(eContext). Appendix C {@code P006}. */
    public static final String KEY_JPCONVBANK = "JPCONVBANK";
    public static final String KEY_UNIONPAY = "UNIONPAY";
    public static final String KEY_WECHAT = "WECHAT";
    public static final String KEY_ALIPAY = "ALIPAY";
    public static final String KEY_APPLEPAY = "APPLEPAY";
    public static final String KEY_GRABPAY = "GRABPAY";
    public static final String KEY_KAKAOPAY = "KAKAOPAY";
    public static final String KEY_LINEPAY = "LINEPAY";

    private static final Map<String, String> DEFAULT_CODES = new LinkedHashMap<>();

    static {
        DEFAULT_CODES.put(KEY_CARD, "P000");
        DEFAULT_CODES.put(KEY_UNIONPAY, "P002");
        DEFAULT_CODES.put(KEY_JPCONVBANK, "P006");
        /* PayPay: OpenAPI·샌드박스 기본 P201. MID별 코드는 extra JSON eximbayMethodCodes.PAYPAY 로 교체. */
        DEFAULT_CODES.put(KEY_PAYPAY, "P201");
        DEFAULT_CODES.put(KEY_ALIPAY, "P003");
        DEFAULT_CODES.put(KEY_WECHAT, "P141");
        DEFAULT_CODES.put(KEY_APPLEPAY, "P198");
        DEFAULT_CODES.put(KEY_GRABPAY, "P185");
        DEFAULT_CODES.put(KEY_LINEPAY, "P186");
        DEFAULT_CODES.put(KEY_KAKAOPAY, "");
    }

    private EximbayPaymentMethodCatalog() {
    }

    public static String normalizeKey(String uiKey) {
        if (uiKey == null) {
            return "";
        }
        String k = uiKey.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        return switch (k) {
            case "ECONTEXT", "KONBINI", "JPCONV", "JPCVS", "JPCVSBANK", "JPBANK", "JAPANCVS" -> KEY_JPCONVBANK;
            case "CUP", "UNION" -> KEY_UNIONPAY;
            default -> k;
        };
    }

    public static String resolveCode(String uiKey, Map<String, String> overrides) {
        String key = normalizeKey(uiKey);
        if (key.isEmpty() || KEY_CARD.equals(key)) {
            String ov = overrides != null ? overrides.get(KEY_CARD) : null;
            if (ov != null) {
                return ov.trim();
            }
            return DEFAULT_CODES.getOrDefault(KEY_CARD, "");
        }
        if (overrides != null && overrides.containsKey(key)) {
            String ov = overrides.get(key);
            return ov != null ? ov.trim() : "";
        }
        return DEFAULT_CODES.getOrDefault(key, "");
    }

    public static Map<String, String> defaultCodes() {
        return new LinkedHashMap<>(DEFAULT_CODES);
    }

    /** 결제창 기본 노출: 신용카드 · PayPay · 일본 편의점·은행 · UnionPay. */
    public static List<String> displayOrder() {
        return Arrays.asList(KEY_CARD, KEY_PAYPAY, KEY_JPCONVBANK, KEY_UNIONPAY);
    }

    public static final String DEFAULT_VISIBLE_CSV = "CARD,PAYPAY,JPCONVBANK,UNIONPAY";

    /**
     * 본사 결제 라우팅 CSV → 노출 키. 비어 있으면 기본 4종. 알 수 없는 키는 무시.
     * 하나도 안 남으면 신용카드만(기존 카드 처리).
     */
    public static List<String> resolveVisible(String csv) {
        List<String> order = displayOrder();
        if (csv == null || csv.isBlank()) {
            return new ArrayList<>(order);
        }
        Set<String> wanted = new LinkedHashSet<>();
        for (String part : csv.split("[,;\\s]+")) {
            String k = normalizeKey(part);
            if (!k.isEmpty()) {
                wanted.add(k);
            }
        }
        List<String> out = new ArrayList<>();
        for (String key : order) {
            if (wanted.contains(key)) {
                out.add(key);
            }
        }
        if (out.isEmpty()) {
            out.add(KEY_CARD);
        }
        return out;
    }

    public static String toCsv(List<String> keys) {
        List<String> vis = keys == null || keys.isEmpty() ? resolveVisible(null) : resolveVisible(String.join(",", keys));
        return String.join(",", vis);
    }

    public static boolean isCardOnly(List<String> visible) {
        return visible != null && visible.size() == 1 && KEY_CARD.equals(visible.get(0));
    }
}
