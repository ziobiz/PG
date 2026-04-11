package com.pg.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

/**
 * 노티 수신 요청이 최초 실시간(LIVE)인지, NOTI 등에서 재전송(RETRY)인지 구분합니다.
 * <p>
 * NOTI·연동사는 아래 헤더 중 하나를 내면 됩니다(대소문자 무시).
 * <ul>
 *   <li>{@code X-Icopay-Notify-Delivery}: {@code LIVE} 또는 {@code RETRY} (권장)</li>
 *   <li>{@code X-Noti-Delivery}: 동일</li>
 *   <li>{@code X-Noti-Attempt}, {@code X-Delivery-Attempt}, {@code X-Retry-Attempt}: 정수,
 *       1 이하 → LIVE, 2 이상 → RETRY</li>
 * </ul>
 * 헤더가 없거나 해석 불가면 {@link #UNKNOWN} 저장됩니다.
 */
public final class NotifyIngressDeliveryKindResolver {

    public static final String LIVE = "LIVE";
    public static final String RETRY = "RETRY";
    public static final String UNKNOWN = "UNKNOWN";

    private NotifyIngressDeliveryKindResolver() {
    }

    public static String resolve(HttpServletRequest req) {
        if (req == null) {
            return UNKNOWN;
        }
        String explicit = firstNonBlankHeader(req,
                "X-Icopay-Notify-Delivery",
                "X-Noti-Delivery",
                "X-Notify-Delivery");
        if (explicit != null) {
            String u = explicit.trim().toUpperCase(Locale.ROOT);
            if ("LIVE".equals(u) || "PRIMARY".equals(u) || "FIRST".equals(u)) {
                return LIVE;
            }
            if ("RETRY".equals(u) || "RESEND".equals(u) || "DUPLICATE".equals(u)) {
                return RETRY;
            }
        }
        String attempt = firstNonBlankHeader(req,
                "X-Noti-Attempt",
                "X-Delivery-Attempt",
                "X-Retry-Attempt",
                "X-Icopay-Notify-Attempt");
        if (attempt != null) {
            try {
                int n = Integer.parseInt(attempt.trim());
                if (n <= 1) {
                    return LIVE;
                }
                return RETRY;
            } catch (NumberFormatException ignored) {
                /* fall through */
            }
        }
        return UNKNOWN;
    }

    private static String firstNonBlankHeader(HttpServletRequest req, String... names) {
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String v = req.getHeader(name);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
