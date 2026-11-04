package com.pg.util;

/**
 * PG·구매자 공개 표면에 나가는 <b>사업자 상호·주소·사이트</b>는 가맹점이 아니라
 * 항상 ICOPAY 집계 정체성만 쓴다.
 *
 * <p>가맹 {@code MerchantProfile.addr*} · 쇼핑몰 도메인은 PG API·HPP·descriptor·
 * return_url·Referer 에 넣지 않는다. 주소 필드가 필요하면 {@link #addressLine(String)} 만 사용.
 */
public final class IcopayPublicIdentity {

    public static final String DISPLAY_NAME = "ICOPAY";
    public static final String SITE_URL = PgOutboundUrlPolicy.DEFAULT_SITE_BASE;
    public static final String API_BASE_URL = "https://api.icopay.co.kr";
    /** 설정·본사 프로필이 비었을 때 PG 자리값용 (가맹 주소 절대 사용 금지). */
    public static final String FALLBACK_ADDRESS = "ICOPAY, https://icopay.co.kr";

    private IcopayPublicIdentity() {
    }

    /** 비어 있으면 {@link #FALLBACK_ADDRESS}. 가맹 주소 문자열을 넘기지 말 것. */
    public static String addressLine(String configuredOrHq) {
        if (configuredOrHq != null && !configuredOrHq.isBlank()) {
            return configuredOrHq.trim();
        }
        return FALLBACK_ADDRESS;
    }

    public static String displayName(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return DISPLAY_NAME;
    }

    public static String siteUrl(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim().replaceAll("/+$", "");
        }
        return SITE_URL;
    }
}
