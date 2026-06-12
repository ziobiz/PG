package com.pg.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * JPAY(J-Pay) MD5 서명 — <a href="https://docs.j-pay.net/docs/http">문서</a> 의
 * 「non-empty 파라미터 ASCII 정렬 → key=value&… → 끝에 key=APIKEY → MD5 대문자」 규칙.
 */
public final class JpaySignatureUtil {

    private JpaySignatureUtil() {
    }

    /** 결제 요청: {@code pay_md5sign} 제외한 비어 있지 않은 값만 참여 */
    public static String signRequestParams(Map<String, String> params, String apiKey) {
        if (apiKey == null) {
            apiKey = "";
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            if (k == null) {
                continue;
            }
            if ("pay_md5sign".equalsIgnoreCase(k.trim())) {
                continue;
            }
            String v = e.getValue();
            if (v == null || v.isBlank()) {
                continue;
            }
            sorted.put(k.trim(), v.trim());
        }
        return md5Upper(buildSignPlain(sorted, apiKey.trim()));
    }

    /**
     * 비동기 노티 등: {@code sign} 필드 제외, 나머지 non-empty 정렬 후 검증.
     *
     * @return 서명 일치 여부
     */
    public static boolean verifyNotifySign(Map<String, String> formFields, String apiKey, String signReceived) {
        return verifyNotifySign(formFields, apiKey, signReceived, null);
    }

    /**
     * @param returnCodeOverride 미들웨어가 {@code returncode} 를 바꿔 보낸 경우 JPAY 원문(예: {@code 00})으로 재검증
     */
    public static boolean verifyNotifySign(Map<String, String> formFields, String apiKey, String signReceived,
                                           String returnCodeOverride) {
        if (signReceived == null || signReceived.isBlank() || apiKey == null) {
            return false;
        }
        TreeMap<String, String> sorted = buildNotifySignFieldMap(formFields, returnCodeOverride);
        String expect = md5Upper(buildSignPlain(sorted, apiKey.trim()));
        return constantTimeEquals(expect, signReceived.trim().toUpperCase(Locale.ROOT));
    }

    /** 노티미들웨어 보강 필드 제외 후 검증, 필요 시 JPAY 원문 {@code returncode=00} 으로 재시도 */
    public static boolean verifyNotifySignWithMiddlewareRetry(Map<String, String> formFields, String apiKey,
                                                              String signReceived) {
        if (verifyNotifySign(formFields, apiKey, signReceived)) {
            return true;
        }
        if (!JpayNotifyStatusResolver.hasMiddlewareManualFollowup(formFields)) {
            return false;
        }
        return verifyNotifySign(formFields, apiKey, signReceived, "00");
    }

    private static TreeMap<String, String> buildNotifySignFieldMap(Map<String, String> formFields,
                                                                   String returnCodeOverride) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> e : formFields.entrySet()) {
            String k = e.getKey();
            if (k == null) {
                continue;
            }
            String kl = k.trim().toLowerCase(Locale.ROOT);
            if ("sign".equals(kl)) {
                continue;
            }
            if (kl.startsWith("_middleware_")) {
                continue;
            }
            String v = e.getValue();
            if (v == null || v.isBlank()) {
                continue;
            }
            sorted.put(k.trim(), v.trim());
        }
        if (returnCodeOverride != null && !returnCodeOverride.isBlank()) {
            sorted.put("returncode", returnCodeOverride.trim());
        }
        return sorted;
    }

    private static String buildSignPlain(TreeMap<String, String> sorted, String apiKey) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        sb.append("&key=").append(apiKey);
        return sb.toString();
    }

    public static String md5Upper(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int d = 0;
        for (int i = 0; i < a.length(); i++) {
            d |= a.charAt(i) ^ b.charAt(i);
        }
        return d == 0;
    }
}
