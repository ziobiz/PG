package com.pg.merchantdeploy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REDIRECT checkout prepare — 가맹 returnUrl/cancelUrl body 금지·NOTI 경유 안내.
 */
public final class MerchantRedirectCheckoutPrepareUtil {

    private MerchantRedirectCheckoutPrepareUtil() {
    }

    public static Optional<Map<String, Object>> rejectMerchantReturnUrlsInBody(Map<String, Object> body) {
        if (body == null) {
            return Optional.empty();
        }
        if (!str(body.get("returnUrl")).isBlank() || !str(body.get("cancelUrl")).isBlank()
                || !str(body.get("return_url")).isBlank() || !str(body.get("cancel_url")).isBlank()) {
            return Optional.of(fail(
                    "returnUrl/cancelUrl은 prepare body에 넣지 않습니다. "
                            + "브라우저 복귀는 NOTI Result → 가맹 설정, 서버 확정은 webhook·status API를 사용하세요.",
                    "MERCHANT_RETURN_URL_NOT_ALLOWED"));
        }
        return Optional.empty();
    }

    public static String redirectUsageHintKo() {
        return "가맹 서버에서 prepare 호출 후 payUrl로 브라우저를 리다이렉트하세요. "
                + "브라우저 복귀는 NOTI Result 경유(가맹 URL은 ICOPAY·PG에 노출하지 않음). "
                + "결제 확정은 webhook·status API로 서버에서 확인하세요.";
    }

    public static String redirectUsageHintEn() {
        return "Call prepare on your server, then redirect the browser to payUrl. "
                + "Browser return goes via NOTI Result (merchant URL is not exposed to ICOPAY or the PG). "
                + "Confirm payment on the server via webhook or Status API.";
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
