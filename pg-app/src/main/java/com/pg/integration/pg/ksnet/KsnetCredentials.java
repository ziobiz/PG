package com.pg.integration.pg.ksnet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgAgency;

/**
 * KSNET(KSPAY) 해외 USD 결제창 자격증명 — ICOPAY↔KSNET 1:1 (가맹 MID 미전달, ICOPAY 집계 storeid 만).
 * <ul>
 *   <li>{@code merchant_mid} → KSPAY {@code storeid}</li>
 *   <li>{@code api_key} → REST 취소·거래조회 {@code Authorization: pgapi {apiKey}}</li>
 *   <li>{@code md5_secret_key} / extra {@code reconPassword} → KSTA(대사) 상점 비밀번호</li>
 *   <li>{@code sandbox_yn} → N 이 아니면 TEST 호스트(pgdev.ksnet.co.kr)</li>
 *   <li>{@code credentials_extra_json}: {@code showCard}(기본 {@code C(25)} 해외카드), {@code sdkUrlPc}, {@code sdkUrlMobile},
 *       {@code approveUrlPc}, {@code approveUrlMobile}, {@code cancelUrl}, {@code tradeUrl}, {@code storeid},
 *       {@code reconStoreId}, {@code reconPassword}</li>
 * </ul>
 */
public final class KsnetCredentials {

    public static final String TEST_HOST = "https://pgdev.ksnet.co.kr";
    public static final String LIVE_HOST = "https://kspay.ksnet.to";
    public static final String TEST_CANCEL_URL = "https://paydev.ksnet.co.kr/kspay/webfep/api/v1/card/cancel";
    public static final String LIVE_CANCEL_URL = "https://pay.ksnet.co.kr/kspay/webfep/api/v1/card/cancel";
    public static final String TEST_TRADE_URL = "https://paydev.ksnet.co.kr/kspay/webfep/api/v1/card/trade";
    public static final String LIVE_TRADE_URL = "https://pay.ksnet.co.kr/kspay/webfep/api/v1/card/trade";

    private static final String SDK_PC_PATH = "/store/KSPayWebV1.4/mall/js/kspay_api_ssl.js";
    private static final String SDK_MOBILE_PATH = "/store/KSPayMobileV1.4/mall/js/kspay_api_ssl.js";
    private static final String APPROVE_PC_PATH = "/store/KSPayWebV1.4/web_host/recv_post.jsp";
    private static final String APPROVE_MOBILE_PATH = "/store/KSPayMobileV1.4/web_host/recv_post.jsp";

    /** 해외카드 전용 — KSNET 통합모듈 카드사 코드표 {@code C(25)}. */
    public static final String DEFAULT_SHOW_CARD = "C(25)";

    private static final ObjectMapper OM = new ObjectMapper();

    private final String storeId;
    private final String apiKey;
    private final String reconPassword;
    private final boolean sandbox;
    private final String showCard;
    private final String sdkUrlPc;
    private final String sdkUrlMobile;
    private final String approveUrlPc;
    private final String approveUrlMobile;
    private final String cancelUrl;
    private final String tradeUrl;

    public KsnetCredentials(String storeId, String apiKey, boolean sandbox, String showCard,
                            String sdkUrlPc, String sdkUrlMobile,
                            String approveUrlPc, String approveUrlMobile, String cancelUrl) {
        this(storeId, apiKey, "", sandbox, showCard, sdkUrlPc, sdkUrlMobile, approveUrlPc, approveUrlMobile,
                cancelUrl, "");
    }

    public KsnetCredentials(String storeId, String apiKey, String reconPassword, boolean sandbox, String showCard,
                            String sdkUrlPc, String sdkUrlMobile,
                            String approveUrlPc, String approveUrlMobile, String cancelUrl, String tradeUrl) {
        this.storeId = nz(storeId);
        this.apiKey = nz(apiKey);
        this.reconPassword = nz(reconPassword);
        this.sandbox = sandbox;
        this.showCard = nz(showCard).isEmpty() ? DEFAULT_SHOW_CARD : nz(showCard);
        String host = sandbox ? TEST_HOST : LIVE_HOST;
        this.sdkUrlPc = firstNonBlank(sdkUrlPc, host + SDK_PC_PATH);
        this.sdkUrlMobile = firstNonBlank(sdkUrlMobile, host + SDK_MOBILE_PATH);
        this.approveUrlPc = firstNonBlank(approveUrlPc, host + APPROVE_PC_PATH);
        this.approveUrlMobile = firstNonBlank(approveUrlMobile, host + APPROVE_MOBILE_PATH);
        this.cancelUrl = firstNonBlank(cancelUrl, sandbox ? TEST_CANCEL_URL : LIVE_CANCEL_URL);
        this.tradeUrl = firstNonBlank(tradeUrl, sandbox ? TEST_TRADE_URL : LIVE_TRADE_URL);
    }

    public static KsnetCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new KsnetCredentials("", "", "", true, "", "", "", "", "", "", "");
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String storeId = nz(agency.getMerchantMid());
        String apiKey = nz(agency.getApiKey());
        String reconPassword = nz(agency.getMd5SecretKey());
        String showCard = "";
        String sdkPc = "";
        String sdkMo = "";
        String apprPc = "";
        String apprMo = "";
        String cancel = "";
        String trade = "";
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                if (storeId.isBlank()) {
                    storeId = text(n, "storeid", "storeId", "store_id", "mid", "reconStoreId",
                            "recon_store_id", "kstaStoreId");
                }
                if (apiKey.isBlank()) {
                    apiKey = text(n, "apiKey", "api_key", "pgapi");
                }
                String reconPw = text(n, "reconPassword", "recon_password", "kstaPassword", "password");
                if (!reconPw.isBlank()) {
                    reconPassword = reconPw;
                }
                showCard = text(n, "showCard", "show_card");
                sdkPc = text(n, "sdkUrlPc", "sdkUrl");
                sdkMo = text(n, "sdkUrlMobile");
                apprPc = text(n, "approveUrlPc", "approveUrl");
                apprMo = text(n, "approveUrlMobile");
                cancel = text(n, "cancelUrl");
                trade = text(n, "tradeUrl", "trade_url");
            } catch (Exception ignored) {
                /* ignore */
            }
        }
        return new KsnetCredentials(storeId, apiKey, reconPassword, sandbox, showCard, sdkPc, sdkMo,
                apprPc, apprMo, cancel, trade);
    }

    /** 결제창·승인에는 storeid 만 필요(apiKey 는 REST 취소·거래조회). */
    public boolean isUsable() {
        return !storeId.isBlank();
    }

    /** 통합내역 거래조회(WebFEP) 가능 여부. */
    public boolean isTradeUsable() {
        return !storeId.isBlank() && !apiKey.isBlank();
    }

    /** KSTA 대사 비밀번호가 있는지. */
    public boolean hasReconPassword() {
        return !reconPassword.isBlank();
    }

    public String storeId() {
        return storeId;
    }

    public String apiKey() {
        return apiKey;
    }

    public String reconPassword() {
        return reconPassword;
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

    public String tradeUrl() {
        return tradeUrl;
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
