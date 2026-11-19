package com.pg.integration.pg.ksnet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgAgency;

/**
 * KSNET(KSPAY) 해외 USD 결제창 자격증명 — ICOPAY↔KSNET 1:1 (가맹 MID 미전달, ICOPAY 집계 storeid 만).
 * <ul>
 *   <li>{@code merchant_mid} → KSPAY {@code storeid}</li>
 *   <li>{@code api_key} → REST 취소 {@code Authorization: pgapi {apiKey}}</li>
 *   <li>{@code sandbox_yn} → N 이 아니면 TEST 호스트(pgdev.ksnet.co.kr)</li>
 *   <li>{@code credentials_extra_json}: {@code showCard}(기본 {@code C(25)} 해외카드), {@code sdkUrlPc}, {@code sdkUrlMobile},
 *       {@code approveUrlPc}, {@code approveUrlMobile}, {@code cancelUrl}, {@code storeid}</li>
 * </ul>
 */
public final class KsnetCredentials {

    public static final String TEST_HOST = "https://pgdev.ksnet.co.kr";
    public static final String LIVE_HOST = "https://kspay.ksnet.to";
    public static final String TEST_CANCEL_URL = "https://paydev.ksnet.co.kr/kspay/webfep/api/v1/card/cancel";
    public static final String LIVE_CANCEL_URL = "https://pay.ksnet.co.kr/kspay/webfep/api/v1/card/cancel";

    private static final String SDK_PC_PATH = "/store/KSPayWebV1.4/mall/js/kspay_api_ssl.js";
    private static final String SDK_MOBILE_PATH = "/store/KSPayMobileV1.4/mall/js/kspay_api_ssl.js";
    private static final String APPROVE_PC_PATH = "/store/KSPayWebV1.4/web_host/recv_post.jsp";
    private static final String APPROVE_MOBILE_PATH = "/store/KSPayMobileV1.4/web_host/recv_post.jsp";

    /** 해외카드 전용 — KSNET 통합모듈 카드사 코드표 {@code C(25)}. */
    public static final String DEFAULT_SHOW_CARD = "C(25)";

    private static final ObjectMapper OM = new ObjectMapper();

    private final String storeId;
    private final String apiKey;
    private final boolean sandbox;
    private final String showCard;
    private final String sdkUrlPc;
    private final String sdkUrlMobile;
    private final String approveUrlPc;
    private final String approveUrlMobile;
    private final String cancelUrl;

    public KsnetCredentials(String storeId, String apiKey, boolean sandbox, String showCard,
                            String sdkUrlPc, String sdkUrlMobile,
                            String approveUrlPc, String approveUrlMobile, String cancelUrl) {
        this.storeId = nz(storeId);
        this.apiKey = nz(apiKey);
        this.sandbox = sandbox;
        this.showCard = nz(showCard).isEmpty() ? DEFAULT_SHOW_CARD : nz(showCard);
        String host = sandbox ? TEST_HOST : LIVE_HOST;
        this.sdkUrlPc = firstNonBlank(sdkUrlPc, host + SDK_PC_PATH);
        this.sdkUrlMobile = firstNonBlank(sdkUrlMobile, host + SDK_MOBILE_PATH);
        this.approveUrlPc = firstNonBlank(approveUrlPc, host + APPROVE_PC_PATH);
        this.approveUrlMobile = firstNonBlank(approveUrlMobile, host + APPROVE_MOBILE_PATH);
        this.cancelUrl = firstNonBlank(cancelUrl, sandbox ? TEST_CANCEL_URL : LIVE_CANCEL_URL);
    }

    public static KsnetCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new KsnetCredentials("", "", true, "", "", "", "", "", "");
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String storeId = nz(agency.getMerchantMid());
        String apiKey = nz(agency.getApiKey());
        String showCard = "";
        String sdkPc = "";
        String sdkMo = "";
        String apprPc = "";
        String apprMo = "";
        String cancel = "";
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                if (storeId.isBlank()) {
                    storeId = text(n, "storeid", "storeId", "store_id", "mid");
                }
                if (apiKey.isBlank()) {
                    apiKey = text(n, "apiKey", "api_key");
                }
                showCard = text(n, "showCard", "show_card");
                sdkPc = text(n, "sdkUrlPc", "sdkUrl");
                sdkMo = text(n, "sdkUrlMobile");
                apprPc = text(n, "approveUrlPc", "approveUrl");
                apprMo = text(n, "approveUrlMobile");
                cancel = text(n, "cancelUrl");
            } catch (Exception ignored) {
                /* ignore */
            }
        }
        return new KsnetCredentials(storeId, apiKey, sandbox, showCard, sdkPc, sdkMo, apprPc, apprMo, cancel);
    }

    /** 결제창·승인에는 storeid 만 필요(apiKey 는 REST 취소 전용). */
    public boolean isUsable() {
        return !storeId.isBlank();
    }

    public String storeId() {
        return storeId;
    }

    public String apiKey() {
        return apiKey;
    }

    public boolean sandbox() {
        return sandbox;
    }

    public String showCard() {
        return showCard;
    }

    public String sdkUrl(boolean mobile) {
        return mobile ? sdkUrlMobile : sdkUrlPc;
    }

    public String approveUrl(boolean mobile) {
        return mobile ? approveUrlMobile : approveUrlPc;
    }

    public String cancelUrl() {
        return cancelUrl;
    }

    private static String text(JsonNode n, String... keys) {
        if (n == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            JsonNode v = n.get(k);
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText("");
                if (s != null && !s.isBlank()) {
                    return s.trim();
                }
            }
        }
        return "";
    }

    private static String firstNonBlank(String a, String b) {
        return nz(a).isEmpty() ? nz(b) : nz(a);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
