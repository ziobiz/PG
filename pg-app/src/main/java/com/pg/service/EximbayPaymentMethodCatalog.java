package com.pg.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Eximbay 결제수단(UI 키) → Eximbay {@code payment_method}(paymethod) 코드 매핑.
 *
 * <p>Eximbay 는 카드·PayPay·UnionPay·WeChat·Alipay·GrabPay·LinePay·ApplePay 등 모든 수단을
 * 동일한 {@code /v1/payments/ready} + JS SDK 결제창 플로우의 {@code payment_method} 코드로 처리한다.
 * 아래 기본 코드는 Eximbay 연동 가이드(Appendix C) 기준이며, 가맹 온보딩별로 코드가 다를 수 있어
 * {@code tb_pg_agency.credentials_extra_json.eximbayMethodCodes}(JSON: UI키→코드) 로 오버라이드할 수 있다.
 *
 * <p>코드가 비어 있으면(예: PayPay 를 가맹 온보딩 코드로 아직 지정하지 않은 경우)
 * {@code payment_method} 를 보내지 않아 <b>Eximbay 통합 결제창(가맹 MID 에 활성화된 모든 수단 노출)</b> 으로
 * 안전하게 진입한다 — 이 경우에도 PayPay 는 결제창에서 선택 가능하다.
 */
public final class EximbayPaymentMethodCatalog {

    /** UI 에서 사용하는 결제수단 키 (프론트 버튼 값과 동일). */
    public static final String KEY_CARD = "CARD";
    public static final String KEY_PAYPAY = "PAYPAY";
    public static final String KEY_UNIONPAY = "UNIONPAY";
    public static final String KEY_WECHAT = "WECHAT";
    public static final String KEY_ALIPAY = "ALIPAY";
    public static final String KEY_APPLEPAY = "APPLEPAY";
    public static final String KEY_GRABPAY = "GRABPAY";
    public static final String KEY_KAKAOPAY = "KAKAOPAY";
    public static final String KEY_LINEPAY = "LINEPAY";

    private static final Map<String, String> DEFAULT_CODES = new LinkedHashMap<>();

    static {
        DEFAULT_CODES.put(KEY_CARD, "P000");      // CreditCard (통합)
        DEFAULT_CODES.put(KEY_UNIONPAY, "P002");  // CUP (UnionPay)
        DEFAULT_CODES.put(KEY_ALIPAY, "P003");    // Alipay Plus
        DEFAULT_CODES.put(KEY_WECHAT, "P141");    // WeChat
        DEFAULT_CODES.put(KEY_APPLEPAY, "P198");  // Apple Pay
        DEFAULT_CODES.put(KEY_GRABPAY, "P185");   // grabPay(SGD)
        DEFAULT_CODES.put(KEY_LINEPAY, "P186");   // linePay
        // PayPay·KakaoPay: 가맹 온보딩별 코드 상이 → 기본 미지정(통합 결제창). 필요 시 credentials_extra_json 로 지정.
        DEFAULT_CODES.put(KEY_PAYPAY, "");
        DEFAULT_CODES.put(KEY_KAKAOPAY, "");
    }

    private EximbayPaymentMethodCatalog() {
    }

    public static String normalizeKey(String uiKey) {
        if (uiKey == null) {
            return "";
        }
        return uiKey.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
    }

    /**
     * UI 결제수단 키를 Eximbay paymethod 코드로 변환한다.
     *
     * @param uiKey       프론트에서 넘어온 결제수단 키 (예: {@code PAYPAY})
     * @param overrides   {@code credentials_extra_json.eximbayMethodCodes} 오버라이드 맵(없으면 null)
     * @return Eximbay paymethod 코드. 매핑이 없거나 비어 있으면 빈 문자열(통합 결제창).
     */
    public static String resolveCode(String uiKey, Map<String, String> overrides) {
        String key = normalizeKey(uiKey);
        if (key.isEmpty() || KEY_CARD.equals(key)) {
            // 카드 기본 — 오버라이드 우선, 없으면 통합(P000). 카드 미지정도 통합 결제창.
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

    /** 프론트 노출용 기본 결제수단 순서(카드 → PayPay 우선 → 그 외). */
    public static Map<String, String> defaultCodes() {
        return new LinkedHashMap<>(DEFAULT_CODES);
    }

    /**
     * 결제창 결제수단 버튼 노출 순서. 신용카드 다음에 <b>PayPay(필수)</b> 를 우선 배치한다.
     */
    public static List<String> displayOrder() {
        return Arrays.asList(
                KEY_CARD, KEY_PAYPAY, KEY_ALIPAY, KEY_WECHAT, KEY_UNIONPAY,
                KEY_GRABPAY, KEY_LINEPAY, KEY_KAKAOPAY, KEY_APPLEPAY);
    }
}
