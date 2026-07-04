package com.pg.urlpay;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * PG 무관 중립 결제창 경로. 가맹점·구매자에게 실제 결제 대행사(ChillPay/JPAY/Eximbay)를
 * 노출하지 않도록, 모든 통합 결제창 진입은 {@code /checkout/{compId}} 로 통일한다.
 * 서버가 운영 PG를 확인해 실제 결제 페이지로 내부 forward 하므로 브라우저 URL·응답에는 PG가 드러나지 않는다.
 */
public final class NeutralCheckoutRoute {

    /** 중립 결제창 경로 프리픽스 (뒤에 compId 가 붙는다). */
    public static final String PATH = "/checkout/";

    /** 중립 구독(정기결제) 결제창 경로 프리픽스. */
    public static final String PATH_SUBSCRIPTION = "/checkout-subscribe/";

    /** 통합 인라인 embed 스크립트 경로 프리픽스 (뒤에 compId). */
    public static final String EMBED_SCRIPT_PATH = "/v1/embed-checkout/";

    /** 통합 구독 embed 스크립트 경로 프리픽스 (뒤에 compId). */
    public static final String EMBED_SUBSCRIBE_SCRIPT_PATH = "/v1/embed-checkout-subscribe/";

    private NeutralCheckoutRoute() {
    }

    public static String buildEmbedScriptUrl(String base, String compId) {
        return buildResourceUrl(base, EMBED_SCRIPT_PATH, compId);
    }

    public static String buildEmbedSubscribeScriptUrl(String base, String compId) {
        return buildResourceUrl(base, EMBED_SUBSCRIBE_SCRIPT_PATH, compId);
    }

    private static String buildResourceUrl(String base, String pathPrefix, String compId) {
        if (compId == null || compId.isBlank()) {
            return "";
        }
        String path = pathPrefix + compId.trim();
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b.isEmpty() ? path : b + path;
    }

    /**
     * 중립 결제창 URL 생성.
     *
     * @param base         공개 고객 사이트 베이스(없으면 상대경로)
     * @param compId       가맹 업체코드
     * @param sessionToken prepare 세션 토큰(없으면 생략)
     * @param langCode     결제창 언어(없으면 생략)
     * @param embed        iframe 임베드면 true (embed=1 추가)
     */
    public static String buildPayUrl(String base, String compId, String sessionToken, String langCode, boolean embed) {
        return buildNeutralUrl(PATH, base, compId, sessionToken, langCode, embed);
    }

    /** 중립 구독(정기결제) 결제창 URL — {@link #PATH_SUBSCRIPTION}. */
    public static String buildSubscribeUrl(String base, String compId, String sessionToken, String langCode, boolean embed) {
        return buildNeutralUrl(PATH_SUBSCRIPTION, base, compId, sessionToken, langCode, embed);
    }

    private static String buildNeutralUrl(String pathPrefix, String base, String compId,
                                          String sessionToken, String langCode, boolean embed) {
        if (compId == null || compId.isBlank()) {
            return "";
        }
        String id = compId.trim();
        StringBuilder q = new StringBuilder();
        q.append(pathPrefix).append(enc(id));
        q.append("?entry=merchant_api");
        if (embed) {
            q.append("&embed=1");
        }
        if (sessionToken != null && !sessionToken.isBlank()) {
            q.append("&session=").append(enc(sessionToken.trim()));
        }
        q.append("&m=").append(enc(id));
        if (langCode != null && !langCode.isBlank()) {
            q.append("&lang=").append(enc(langCode.trim()));
        }
        String path = q.toString();
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b.isEmpty() ? path : b + path;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }
}
