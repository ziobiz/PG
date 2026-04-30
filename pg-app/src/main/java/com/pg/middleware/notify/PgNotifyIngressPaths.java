package com.pg.middleware.notify;

/**
 * PG·NOTI 등에서 호출하는 <strong>공개 노티 수신</strong> URL 경로 접두.
 * 레거시({@value com.pg.middleware.notify.PgNotifyIngressPaths#OPEN_PREFIX})와
 * 권장 미들웨어({@value com.pg.middleware.notify.PgNotifyIngressPaths#MIDDLEWARE_PREFIX})는 동일 핸들러를 씁니다.
 */
public final class PgNotifyIngressPaths {

    /** 레거시 베이스 — {@code …/api/open/pg-notify/{token}/…} */
    public static final String OPEN_PREFIX = "/api/open/pg-notify/";

    /** 권장 베이스 — {@code …/api/middleware/notify/v1/pg-notify/{token}/…} */
    public static final String MIDDLEWARE_PREFIX = "/api/middleware/notify/v1/pg-notify/";

    private PgNotifyIngressPaths() {
    }

    /**
     * @param publicBaseUrl 끝 슬래시 없이 트림된 공개 API 베이스
     * @param ingressToken  노티 환경 토큰
     */
    public static String buildIngressBase(String publicBaseUrl, String ingressToken) {
        String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "";
        String tok = ingressToken != null ? ingressToken.trim() : "";
        return base + MIDDLEWARE_PREFIX + tok;
    }

    public static String buildIngressBaseOpen(String publicBaseUrl, String ingressToken) {
        String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "";
        String tok = ingressToken != null ? ingressToken.trim() : "";
        return base + OPEN_PREFIX + tok;
    }
}
