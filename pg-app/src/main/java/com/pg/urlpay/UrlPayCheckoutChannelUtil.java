package com.pg.urlpay;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Set;

/**
 * 결제창이동은 <b>공개 URL 결제</b>에만 적용한다.
 * <p>
 * 가맹 API 인라인·리다이렉트·WooCommerce는 자체 쇼핑몰/솔루션에 prepare 세션으로 연동하므로
 * 구매자에게 「다른 결제 URL로 이동」할 이유가 없다. {@code /checkout/{compId}} 가 공통 중립 경로라서
 * API payUrl 도 같은 경로를 타므로, 활성화 시 <b>반드시 채널을 판별</b>해 API 계열은 건너뛴다.
 */
public final class UrlPayCheckoutChannelUtil {

    private static final Set<String> MERCHANT_API_ENTRIES = Set.of(
            "merchant_api", "merchantapi", "api", "inline", "redirect",
            "woocommerce", "woo", "wordpress", "wp", "wc");

    private UrlPayCheckoutChannelUtil() {
    }

    /**
     * 공개 URL 결제(구매자에게 공유하는 /checkout 링크, 챗봇 URL)이면 true.
     * 가맹 API 인라인·리다이렉트·임베드·WooCommerce 이면 false.
     */
    public static boolean isPublicUrlPayCheckout(HttpServletRequest req) {
        if (req == null) {
            return false;
        }
        String path = req.getRequestURI();
        if (path != null) {
            String p = path.toLowerCase(Locale.ROOT);
            if (p.contains("/v1/embed-checkout")
                    || p.contains("/embed-checkout")
                    || p.contains("/checkout-subscribe")) {
                return false;
            }
        }
        if (isMerchantApiEntry(param(req, "entry"))
                || isMerchantApiEntry(param(req, "channel"))
                || isMerchantApiEntry(param(req, "source"))
                || isMerchantApiEntry(param(req, "src"))) {
            return false;
        }
        String embed = param(req, "embed");
        if ("1".equals(embed) || "true".equalsIgnoreCase(embed)) {
            return false;
        }
        if (truthy(param(req, "woo")) || truthy(param(req, "woocommerce")) || truthy(param(req, "wp"))) {
            return false;
        }
        return true;
    }

    private static boolean isMerchantApiEntry(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String e = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return MERCHANT_API_ENTRIES.contains(e);
    }

    private static boolean truthy(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String v = raw.trim();
        return !"0".equals(v) && !"false".equalsIgnoreCase(v) && !"n".equalsIgnoreCase(v);
    }

    private static String param(HttpServletRequest req, String name) {
        try {
            return req.getParameter(name);
        } catch (Exception e) {
            return null;
        }
    }
}
