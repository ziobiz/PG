package com.pg.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PG(JPAY·ChillPay·ElementPay 및 추가 PG) 로 나가는 결제 전문에는 <b>가맹점의 도메인·주소가 절대 포함되지 않도록</b>
 * 우리(운영사)·노티미들웨어 도메인만 허용하는 아웃바운드 URL 정책.
 *
 * <p>notifyUrl·callbackUrl·returnUrl·pay_url 등 PG 로 전송되는 모든 URL 은 이 정책을 거쳐,
 * 호스트가 우리 허용 도메인이 아니면 안전한 기본값(우리 ingress/NOTI 결과 URL)으로 대체한다.
 * ICOPAY·NOTI({@code noti.icopay.net}) 노출은 허용하고, 가맹 쇼핑몰 도메인만 차단한다.
 * 업체코드({@code compId})는 이 정책 대상이 아니다(노티 라우팅용으로 유지).
 */
public final class PgOutboundUrlPolicy {

    /** 운영 노티미들웨어 기본 베이스 */
    public static final String DEFAULT_NOTI_BASE = "https://noti.icopay.net";
    /** 테스트 노티미들웨어 */
    public static final String DEFAULT_NOTI_TEST_BASE = "https://test.noti.icopay.net";
    /** 고객·관리 사이트 기본 */
    public static final String DEFAULT_SITE_BASE = "https://icopay.co.kr";

    private PgOutboundUrlPolicy() {
    }

    /**
     * PG 아웃바운드 허용 베이스: public API + NOTI 프로비저닝 베이스 + 잘 알려진 ICOPAY/NOTI 호스트.
     * {@code extra} 에 관리자/고객 사이트 URL 등을 추가할 수 있다.
     */
    public static String[] allowedIcopayAndNotiBases(String publicApiBase, String notiProvisionBase, String... extra) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        addBase(set, publicApiBase);
        addBase(set, notiProvisionBase);
        addBase(set, DEFAULT_NOTI_BASE);
        addBase(set, DEFAULT_NOTI_TEST_BASE);
        addBase(set, DEFAULT_SITE_BASE);
        if (extra != null) {
            for (String e : extra) {
                addBase(set, e);
            }
        }
        return set.toArray(new String[0]);
    }

    private static void addBase(java.util.Set<String> set, String base) {
        if (base != null && !base.isBlank()) {
            set.add(base.trim());
        }
    }

    /**
     * {@code candidate} 의 호스트가 허용 도메인(우리 것)일 때만 그대로 사용하고,
     * 아니면(가맹/외부 도메인·상대경로·파싱불가) {@code safeDefault} 로 대체한다.
     *
     * @param candidate        검사할 후보 URL (가맹 설정·요청 body 등에서 온 값)
     * @param safeDefault      허용되지 않을 때 대체할 우리 도메인 기본 URL
     * @param allowedBaseUrls  우리 도메인으로 인정할 기준 URL 들(publicApiBaseUrl·publicAdminSiteUrl 등)
     */
    public static String enforceOwnDomain(String candidate, String safeDefault, String... allowedBaseUrls) {
        if (candidate == null || candidate.isBlank()) {
            return safeDefault;
        }
        String candHost = hostOf(candidate);
        if (candHost == null) {
            return safeDefault;
        }
        List<String> allowedHosts = new ArrayList<>();
        if (allowedBaseUrls != null) {
            for (String base : allowedBaseUrls) {
                String h = hostOf(base);
                if (h != null) {
                    allowedHosts.add(h);
                }
            }
        }
        // safeDefault 자체는 항상 우리 도메인이어야 하므로 그 호스트도 허용에 포함
        String defHost = hostOf(safeDefault);
        if (defHost != null) {
            allowedHosts.add(defHost);
        }
        return isOwnHost(candHost, allowedHosts) ? candidate.trim() : safeDefault;
    }

    /** {@code candidate} 호스트가 허용 도메인이면 true. (같은 호스트 또는 그 하위 서브도메인) */
    public static boolean isOwnDomain(String candidate, String... allowedBaseUrls) {
        String candHost = hostOf(candidate);
        if (candHost == null) {
            return false;
        }
        List<String> allowedHosts = new ArrayList<>();
        if (allowedBaseUrls != null) {
            for (String base : allowedBaseUrls) {
                String h = hostOf(base);
                if (h != null) {
                    allowedHosts.add(h);
                }
            }
        }
        return isOwnHost(candHost, allowedHosts);
    }

    private static boolean isOwnHost(String candHost, List<String> allowedHosts) {
        for (String allowed : allowedHosts) {
            if (candHost.equals(allowed) || candHost.endsWith("." + allowed)) {
                return true;
            }
        }
        return false;
    }

    /** URL 문자열에서 소문자 호스트만 추출(스킴/호스트가 없으면 null). */
    public static String hostOf(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI u = URI.create(url.trim());
            String h = u.getHost();
            if (h == null || h.isBlank()) {
                return null;
            }
            return h.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }
}
