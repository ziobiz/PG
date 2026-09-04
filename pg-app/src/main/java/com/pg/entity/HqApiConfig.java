package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 본사 API 구성 세팅 (가맹점 발급 API 기본 URL/인증/타임아웃 등) - 단일 행
 */
@Entity
@Table(name = "tb_hq_api_config")
public class HqApiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_url", length = 255)
    private String baseUrl;

    @Column(name = "auth_type", length = 50)
    private String authType;

    @Column(name = "timeout_sec")
    private Integer timeoutSec;

    @Column(name = "memo", length = 500)
    private String memo;

    /** ChillPay (칠리페이) - Merchant Code */
    @Column(name = "chillpay_merchant_code", length = 50)
    private String chillpayMerchantCode;
    /** ChillPay API Key */
    @Column(name = "chillpay_api_key", length = 255)
    private String chillpayApiKey;
    /** ChillPay MD5 Secret Key (CheckSum 생성용) */
    @Column(name = "chillpay_md5_key", length = 255)
    private String chillpayMd5Key;
    /** ChillPay Route No */
    @Column(name = "chillpay_route_no")
    private Integer chillpayRouteNo;
    /** ChillPay Sandbox 사용 여부 */
    @Column(name = "chillpay_sandbox", length = 1)
    private String chillpaySandbox;

    /**
     * ChillPay 호스티드 결제 ResultUrl 경로 (publicBaseUrl 기준).
     * 예: /pay-result.html — 비우면 기본 /pay-result.html
     */
    @Column(name = "chillpay_url_result_path", length = 255)
    private String chillpayUrlResultPath;

    /**
     * ChillPay CallbackUrl 전체 (서버 노티). 비우면 전산노티 환경의 PG 노티 수신 URL을 사용합니다.
     * ziobiz/NOTI 등 미들웨어 URL을 ChillPay에 등록한 경우 그 URL을 입력합니다.
     */
    @Column(name = "chillpay_url_callback_url", length = 1024)
    private String chillpayUrlCallbackUrl;

    @Column(name = "api_broker_default_flow_type", length = 20)
    private String apiBrokerDefaultFlowType = "INLINE";

    @Column(name = "url_pay_default_flow_type", length = 20)
    private String urlPayDefaultFlowType = "REDIRECT";

    @Column(name = "url_pay_path_template", length = 255)
    private String urlPayPathTemplate = "/pay/{compCode}";

    @Column(name = "api_broker_inline_enabled_yn", length = 1)
    private String apiBrokerInlineEnabledYn = "Y";

    @Column(name = "api_broker_redirect_enabled_yn", length = 1)
    private String apiBrokerRedirectEnabledYn = "Y";

    @Column(name = "url_pay_inline_enabled_yn", length = 1)
    private String urlPayInlineEnabledYn = "Y";

    @Column(name = "url_pay_redirect_enabled_yn", length = 1)
    private String urlPayRedirectEnabledYn = "Y";

    /**
     * 모바일·embed 결제창 기본 — {@link com.pg.urlpay.MobileCheckoutModeUtil}.
     * EMBED(기본) | MOBILE_REDIRECT | ALWAYS_REDIRECT
     */
    @Column(name = "mobile_checkout_mode_default", length = 32)
    private String mobileCheckoutModeDefault = "EMBED";

    /**
     * 공개 URL·챗봇·분할 URL 결제창 입력방식 본사 기본 —
     * {@link com.pg.urlpay.UrlPayInputModeUtil}. 가맹 FOLLOW_HQ 시 URL 채널.
     */
    @Column(name = "url_pay_input_mode_default", nullable = false, length = 16)
    private String urlPayInputModeDefault = "GENERAL";

    /**
     * API 인라인(entry=merchant_api) 결제창 입력방식 본사 기본.
     * 가맹 FOLLOW_HQ 시 API 채널.
     */
    @Column(name = "api_url_pay_input_mode_default", nullable = false, length = 16)
    private String apiUrlPayInputModeDefault = "TYPE_BA";

    /** JPAY URL 결제창 카드 유효기간 입력 본사 기본 — {@link com.pg.urlpay.UrlPayCardExpiryModeUtil} */
    @Column(name = "url_pay_card_expiry_mode_default", nullable = false, length = 16)
    private String urlPayCardExpiryModeDefault = "DROPDOWN";

    /** 일반결제(URL·API) 카드 인증 본사 기본 — {@link com.pg.urlpay.CardAuthModeUtil} THREE_DS|NONE3D */
    @Column(name = "card_auth_mode_default", nullable = false, length = 16)
    private String cardAuthModeDefault = "THREE_DS";

    /** 결제창 가맹점명 노출 본사 기본 — Y/N. 가맹 FOLLOW_HQ 시 */
    @Column(name = "url_pay_company_name_show_default_yn", nullable = false, length = 1)
    private String urlPayCompanyNameShowDefaultYn = "Y";

    /** 결제창 다국어 메뉴 본사 기본 — Y/N */
    @Column(name = "url_pay_lang_menu_use_default_yn", nullable = false, length = 1)
    private String urlPayLangMenuUseDefaultYn = "Y";

    /** 결제창 연락처 자동기억 본사 기본 — Y/N */
    @Column(name = "checkout_contact_remember_default_yn", nullable = false, length = 1)
    private String checkoutContactRememberDefaultYn = "Y";

    /** 결제창 로고설정 본사 기본 — {@link com.pg.urlpay.WebPaymentHeaderLogoModeUtil} */
    @Column(name = "web_payment_header_logo_mode_default", nullable = false, length = 16)
    private String webPaymentHeaderLogoModeDefault = "DEFAULT";

    /** 결제창 경고메세지 본사 기본 — {@link com.pg.urlpay.CheckoutHeaderSubtitleModeUtil} */
    @Column(name = "web_payment_header_subtitle_mode_default", nullable = false, length = 16)
    private String webPaymentHeaderSubtitleModeDefault = "DEFAULT";

    /** 결제창 배송주소 본사 기본 — Y/N */
    @Column(name = "url_pay_shipping_address_use_default_yn", nullable = false, length = 1)
    private String urlPayShippingAddressUseDefaultYn = "N";

    /** 결제창 이메일 입력 본사 기본 — Y/N (전 PG 공통) */
    @Column(name = "url_pay_buyer_email_use_default_yn", nullable = false, length = 1)
    private String urlPayBuyerEmailUseDefaultYn = "Y";

    /** 결제창 국가코드 입력 본사 기본 — Y/N (전 PG 공통) */
    @Column(name = "url_pay_buyer_country_use_default_yn", nullable = false, length = 1)
    private String urlPayBuyerCountryUseDefaultYn = "Y";

    /** 결제창 전화번호 입력 본사 기본 — Y/N (전 PG 공통) */
    @Column(name = "url_pay_buyer_phone_use_default_yn", nullable = false, length = 1)
    private String urlPayBuyerPhoneUseDefaultYn = "Y";

    /** 결제창 상품명 사용 본사 기본 — Y/N */
    @Column(name = "url_pay_product_name_use_default_yn", nullable = false, length = 1)
    private String urlPayProductNameUseDefaultYn = "Y";

    /** 본사 기본 상품명 — 상품명 사용=Y 이고 가맹 FOLLOW_HQ 시 */
    @Column(name = "url_pay_default_product_name", length = 200)
    private String urlPayDefaultProductName;

    @Column(name = "url_pay_default_product_code", length = 50)
    private String urlPayDefaultProductCode;

    @Column(name = "url_pay_default_product_amount", precision = 18, scale = 2)
    private java.math.BigDecimal urlPayDefaultProductAmount;

    @Column(name = "url_pay_default_product_desc", length = 500)
    private String urlPayDefaultProductDesc;

    /** WordPress/WooCommerce 플러그인 ZIP·REST webhook 채널 전역 제공 */
    @Column(name = "api_wordpress_plugin_enabled_yn", length = 1)
    private String apiWordpressPluginEnabledYn = "Y";

    /** URL 재결제(저장 카드) 공개 제공 여부 */
    @Column(name = "url_pay_repay_enabled_yn", length = 1)
    private String urlPayRepayEnabledYn = "N";

    /** URL 재결제 공개 경로 템플릿. 예: /pay-repay/{compCode} */
    @Column(name = "url_pay_repay_path_template", length = 255)
    private String urlPayRepayPathTemplate = "/pay-repay/{compCode}";

    /**
     * URL 분할결제 계약취소 — 가맹 {@code FOLLOW_HQ} 시 기본 부여(Y/N). 기본 N.
     */
    @Column(name = "split_pay_contract_cancel_default_yn", nullable = false, length = 1)
    private String splitPayContractCancelDefaultYn = "Y";

    /**
     * URL 분할결제 계약취소 — 본사(REGIONAL)·총판(MASTER_DIST) 운영 권한(Y/N). 기본 N.
     * 총본사(HEADQUARTERS)·ADMIN은 항상 가능.
     */
    @Column(name = "split_pay_contract_cancel_org_op_yn", nullable = false, length = 1)
    private String splitPayContractCancelOrgOpYn = "N";

    /** JPAY 가맹 API 구독(정기) 전역 제공 여부 — ③ 인라인 전용 */
    @Column(name = "jpay_subscription_enabled_yn", length = 1)
    private String jpaySubscriptionEnabledYn = "N";

    /**
     * 멀티 결제대행사 라우팅 — Y: 가맹 {@code card_brand_scope}·통화 힌트로 운영 PG 선택.
     * N: 단일 운영 PG(정렬 우선 1건, 기존 동작).
     */
    @Column(name = "multi_pg_routing_enabled_yn", nullable = false, length = 1)
    private String multiPgRoutingEnabledYn = "Y";

    /**
     * 멀티 PG 라우팅 차원 — {@link com.pg.util.MultiPgRoutingModeUtil}.
     * BRAND | CURRENCY | BRAND_AND_CURRENCY(기본).
     */
    @Column(name = "multi_pg_routing_mode", nullable = false, length = 32)
    private String multiPgRoutingMode = "BRAND_AND_CURRENCY";

    /**
     * 엑심베이 URL 결제창 노출 수단 — 본사 결제 라우팅.
     * CSV: CARD,PAYPAY,JPCONVBANK,UNIONPAY. 가맹은 본사설정 따름. 신용카드만이면 ICOPAY 카드입력 UI.
     */
    @Column(name = "eximbay_methods_visible", length = 200)
    private String eximbayMethodsVisible = "CARD,PAYPAY,JPCONVBANK,UNIONPAY";

    /** JPAY 구독 인라인 결제창(jpay-subscribe.html) 제공 여부 */
    @Column(name = "jpay_subscription_inline_enabled_yn", length = 1)
    private String jpaySubscriptionInlineEnabledYn = "N";

    /** JPAY 구독 공개 경로 템플릿. 예: /jpay-subscribe/{compCode} */
    @Column(name = "jpay_subscription_path_template", length = 128)
    private String jpaySubscriptionPathTemplate = "/jpay-subscribe/{compCode}";

    /** JPAY 구독 기본 plan attempts·interval_time 등 JSON */
    @Column(name = "jpay_subscription_config_json", columnDefinition = "TEXT")
    private String jpaySubscriptionConfigJson;

    /**
     * URL 공개 결제 페이지(/pay/…) 입력 필드 구성.
     * FULL: 배송지·성명 분리 등 전체, SIMPLE: 상품·연락·금액 중심 간편 폼.
     */
    @Column(name = "url_pay_form_mode", length = 20)
    private String urlPayFormMode = "FULL";

    /**
     * JPAY URL 결제창(jpay-pay.html) 입력 필드 본사 기본값 —
     * {@link com.pg.urlpay.JpayCheckoutFieldModeUtil}.
     * FULL / CARD_ONLY / CARD_PREFILL. 가맹 오버라이드가 없으면 전체 가맹에 적용됩니다.
     */
    @Column(name = "jpay_checkout_field_mode", length = 20)
    private String jpayCheckoutFieldMode = "FULL";

    /** JPAY 결제창 전화 국가번호 드롭다운 — Y/N. 가맹 오버라이드 없으면 적용. */
    @Column(name = "jpay_phone_dial_code_yn", length = 1)
    private String jpayPhoneDialCodeYn = "N";

    /**
     * URL 공개 결제 페이지 탭 제목 — 언어코드→문자열 JSON (예: {@code {"KOR":"…","ENG":"…"}}).
     * 결제구문설정(PG별) JSON의 구 {@code tabTitle} 은 비어 있을 때만 폴백으로 사용됩니다.
     */
    @Column(name = "url_pay_tab_title_json", columnDefinition = "TEXT")
    private String urlPayTabTitleJson;

    /**
     * URL 공개 결제 페이지 파비콘 — {@code /uploads/hq/url-pay/…} 경로.
     * 결제구문 JSON의 구 {@code faviconUrl} 은 비어 있을 때만 폴백.
     */
    @Column(name = "url_pay_favicon_url", length = 500)
    private String urlPayFaviconUrl;

    @Column(name = "payment_provider_registry_json", columnDefinition = "TEXT")
    private String paymentProviderRegistryJson;

    /**
     * 결제통화로직설정 JSON. 예: {@code {"rules":[{"pgCd":"CHILLPAY","currency":"JPY","mode":"MULTIPLY_100"}]}}
     * mode: SAME | MULTIPLY_100 | DIVIDE_100 — URL 결제 폼 입력 금액 대비 ChillPay 등 API 금액.
     */
    @Column(name = "pay_currency_scale_rules_json", columnDefinition = "TEXT")
    private String payCurrencyScaleRulesJson;

    /**
     * 결제구문설정 JSON. {@code {"entries":[{"id":"…","pgCd":"CHILLPAY","activeYn":"Y",
     * "amountScaleNoticeShowYn":"Y","amountScaleNotice":{…},
     * "title":{…},"body1":{…},"body2":{…},"body3":{…},"tabTitle":{…},"faviconUrl":"/uploads/hq/url-pay/…"}]}}
     * (레거시) {@code tabTitle}/{@code faviconUrl}: 본사 「URL 결제 폼 설정」 전역값이 비어 있을 때만 탭·아이콘 폴백.
     * {@code resultSuccessMain}/{@code resultSuccessFoot}/{@code resultFailMain}/{@code resultFailFoot}: URL 결제 결과 화면 문구(언어별 맵, 선택).
     */
    @Column(name = "url_pay_card_copy_config_json", columnDefinition = "TEXT")
    private String urlPayCardCopyConfigJson;

    /**
     * URL 결제 「표시통화 → 실결제 THB」 본사 설정 JSON.
     * 예: {@code {"enabled":true,"refreshSeconds":600,"marginByCurrency":{"JPY":0.01,"USD":0.02,"KRW":0,"THB":0}}}
     */
    @Column(name = "url_pay_display_fx_json", columnDefinition = "TEXT")
    private String urlPayDisplayFxJson;

    /** BOT 일평균 환율 API 키(포털 Client ID 등). 비우면 환경변수·yml */
    @Column(name = "bot_thailand_api_key", length = 512)
    private String botThailandApiKey;

    /** BOT API Base URL(끝 슬래시 없이). 비우면 환경변수·yml */
    @Column(name = "bot_thailand_base_url", length = 512)
    private String botThailandBaseUrl;

    /** 일평균 경로(앞 슬래시 포함 권장). 비우면 환경변수·yml */
    @Column(name = "bot_thailand_daily_avg_path", length = 255)
    private String botThailandDailyAvgPath;

    /** 인증 헤더 이름: Authorization(api 포털 v2) 또는 api-key(iAPI). 비우면 환경변수·yml */
    @Column(name = "bot_thailand_api_key_header", length = 64)
    private String botThailandApiKeyHeader;

    /** 환수금에서 수수료 포함 여부 (Y/N) */
    @Column(name = "recall_include_fee_yn", length = 1)
    private String recallIncludeFeeYn = "N";

    /** 정산 관련 VAT 부과 여부 (Y/N) */
    @Column(name = "settlement_vat_apply_yn", length = 1)
    private String settlementVatApplyYn = "Y";

    /** 본사 영업일 설정 목록 JSON */
    @Column(name = "business_day_settings_json", columnDefinition = "TEXT")
    private String businessDaySettingsJson;

    /** 총본사 기준 영업일 프로필 ID (business_day_settings_json 항목 id) */
    @Column(name = "hq_default_business_day_profile_id", length = 64)
    private String hqDefaultBusinessDayProfileId;

    /** 관리자(웹) 공개 URL — 안내·문서용 */
    @Column(name = "public_admin_site_url", length = 500)
    private String publicAdminSiteUrl;

    /** API 공개 베이스 URL — 안내용 */
    @Column(name = "public_api_base_url", length = 500)
    private String publicApiBaseUrl;

    /** 서버운영관리: 모니터링할 fullchain.pem 경로(비우면 ENV 또는 LE 자동) */
    @Column(name = "server_manage_ssl_cert_path", length = 500)
    private String serverManageSslCertPath;

    /** 서버운영관리: Let's Encrypt live 폴더명(다중 도메인 시) */
    @Column(name = "server_manage_ssl_le_domain", length = 255)
    private String serverManageSslLeDomain;

    @Column(name = "server_manage_contract_disk_mb")
    private Integer serverManageContractDiskMb;

    @Column(name = "server_manage_contract_traffic_mb")
    private Integer serverManageContractTrafficMb;

    @Column(name = "server_manage_contract_start")
    private LocalDate serverManageContractStart;

    @Column(name = "server_manage_contract_end")
    private LocalDate serverManageContractEnd;

    @Column(name = "server_manage_traffic_used_mb")
    private Integer serverManageTrafficUsedMb;

    /** 서버운영관리 대시보드 자동 갱신 간격(초). NULL이면 application.yml app.serverManage.uiAutoRefreshSeconds */
    @Column(name = "server_manage_ui_refresh_sec")
    private Integer serverManageUiRefreshSec;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public Integer getTimeoutSec() { return timeoutSec; }
    public void setTimeoutSec(Integer timeoutSec) { this.timeoutSec = timeoutSec; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getChillpayMerchantCode() { return chillpayMerchantCode; }
    public void setChillpayMerchantCode(String chillpayMerchantCode) { this.chillpayMerchantCode = chillpayMerchantCode; }
    public String getChillpayApiKey() { return chillpayApiKey; }
    public void setChillpayApiKey(String chillpayApiKey) { this.chillpayApiKey = chillpayApiKey; }
    public String getChillpayMd5Key() { return chillpayMd5Key; }
    public void setChillpayMd5Key(String chillpayMd5Key) { this.chillpayMd5Key = chillpayMd5Key; }
    public Integer getChillpayRouteNo() { return chillpayRouteNo; }
    public void setChillpayRouteNo(Integer chillpayRouteNo) { this.chillpayRouteNo = chillpayRouteNo; }
    public String getChillpaySandbox() { return chillpaySandbox; }
    public void setChillpaySandbox(String chillpaySandbox) { this.chillpaySandbox = chillpaySandbox; }
    public String getChillpayUrlResultPath() { return chillpayUrlResultPath; }
    public void setChillpayUrlResultPath(String chillpayUrlResultPath) { this.chillpayUrlResultPath = chillpayUrlResultPath; }
    public String getChillpayUrlCallbackUrl() { return chillpayUrlCallbackUrl; }
    public void setChillpayUrlCallbackUrl(String chillpayUrlCallbackUrl) { this.chillpayUrlCallbackUrl = chillpayUrlCallbackUrl; }
    public String getApiBrokerDefaultFlowType() { return apiBrokerDefaultFlowType; }
    public void setApiBrokerDefaultFlowType(String apiBrokerDefaultFlowType) { this.apiBrokerDefaultFlowType = apiBrokerDefaultFlowType; }
    public String getUrlPayDefaultFlowType() { return urlPayDefaultFlowType; }
    public void setUrlPayDefaultFlowType(String urlPayDefaultFlowType) { this.urlPayDefaultFlowType = urlPayDefaultFlowType; }
    public String getUrlPayPathTemplate() { return urlPayPathTemplate; }
    public void setUrlPayPathTemplate(String urlPayPathTemplate) { this.urlPayPathTemplate = urlPayPathTemplate; }
    public String getApiBrokerInlineEnabledYn() { return apiBrokerInlineEnabledYn; }
    public void setApiBrokerInlineEnabledYn(String apiBrokerInlineEnabledYn) { this.apiBrokerInlineEnabledYn = apiBrokerInlineEnabledYn; }
    public String getApiBrokerRedirectEnabledYn() { return apiBrokerRedirectEnabledYn; }
    public void setApiBrokerRedirectEnabledYn(String apiBrokerRedirectEnabledYn) { this.apiBrokerRedirectEnabledYn = apiBrokerRedirectEnabledYn; }
    public String getUrlPayInlineEnabledYn() { return urlPayInlineEnabledYn; }
    public void setUrlPayInlineEnabledYn(String urlPayInlineEnabledYn) { this.urlPayInlineEnabledYn = urlPayInlineEnabledYn; }
    public String getUrlPayRedirectEnabledYn() { return urlPayRedirectEnabledYn; }
    public void setUrlPayRedirectEnabledYn(String urlPayRedirectEnabledYn) { this.urlPayRedirectEnabledYn = urlPayRedirectEnabledYn; }
    public String getMobileCheckoutModeDefault() { return mobileCheckoutModeDefault; }
    public void setMobileCheckoutModeDefault(String mobileCheckoutModeDefault) {
        this.mobileCheckoutModeDefault = mobileCheckoutModeDefault != null && !mobileCheckoutModeDefault.isBlank()
                ? mobileCheckoutModeDefault.trim().toUpperCase(java.util.Locale.ROOT) : "EMBED";
    }
    public String getUrlPayInputModeDefault() { return urlPayInputModeDefault; }
    public void setUrlPayInputModeDefault(String urlPayInputModeDefault) {
        String n = com.pg.urlpay.UrlPayInputModeUtil.normalize(
                urlPayInputModeDefault != null ? urlPayInputModeDefault : com.pg.urlpay.UrlPayInputModeUtil.GENERAL);
        if (com.pg.urlpay.UrlPayInputModeUtil.FOLLOW_HQ.equals(n)) {
            n = com.pg.urlpay.UrlPayInputModeUtil.GENERAL;
        }
        this.urlPayInputModeDefault = n;
    }
    public String getApiUrlPayInputModeDefault() { return apiUrlPayInputModeDefault; }
    public void setApiUrlPayInputModeDefault(String apiUrlPayInputModeDefault) {
        String n = com.pg.urlpay.UrlPayInputModeUtil.normalize(
                apiUrlPayInputModeDefault != null ? apiUrlPayInputModeDefault : com.pg.urlpay.UrlPayInputModeUtil.TYPE_BA);
        if (com.pg.urlpay.UrlPayInputModeUtil.FOLLOW_HQ.equals(n)) {
            n = com.pg.urlpay.UrlPayInputModeUtil.TYPE_BA;
        }
        this.apiUrlPayInputModeDefault = n;
    }
    public String getCardAuthModeDefault() { return cardAuthModeDefault; }
    public void setCardAuthModeDefault(String cardAuthModeDefault) {
        this.cardAuthModeDefault = com.pg.urlpay.CardAuthModeUtil.normalize(
                cardAuthModeDefault != null ? cardAuthModeDefault : com.pg.urlpay.CardAuthModeUtil.THREE_DS);
    }

    public String getUrlPayCardExpiryModeDefault() { return urlPayCardExpiryModeDefault; }
    public void setUrlPayCardExpiryModeDefault(String urlPayCardExpiryModeDefault) {
        this.urlPayCardExpiryModeDefault = com.pg.urlpay.UrlPayCardExpiryModeUtil.normalize(
                urlPayCardExpiryModeDefault != null ? urlPayCardExpiryModeDefault
                        : com.pg.urlpay.UrlPayCardExpiryModeUtil.DROPDOWN);
    }

    public String getUrlPayCompanyNameShowDefaultYn() { return urlPayCompanyNameShowDefaultYn; }
    public void setUrlPayCompanyNameShowDefaultYn(String v) {
        this.urlPayCompanyNameShowDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "Y");
    }
    public String getUrlPayLangMenuUseDefaultYn() { return urlPayLangMenuUseDefaultYn; }
    public void setUrlPayLangMenuUseDefaultYn(String v) {
        this.urlPayLangMenuUseDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "Y");
    }
    public String getCheckoutContactRememberDefaultYn() { return checkoutContactRememberDefaultYn; }
    public void setCheckoutContactRememberDefaultYn(String v) {
        this.checkoutContactRememberDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "Y");
    }
    public String getWebPaymentHeaderLogoModeDefault() { return webPaymentHeaderLogoModeDefault; }
    public void setWebPaymentHeaderLogoModeDefault(String v) {
        this.webPaymentHeaderLogoModeDefault = com.pg.urlpay.WebPaymentHeaderLogoModeUtil.normalize(
                v != null ? v : com.pg.urlpay.WebPaymentHeaderLogoModeUtil.DEFAULT);
    }
    public String getWebPaymentHeaderSubtitleModeDefault() { return webPaymentHeaderSubtitleModeDefault; }
    public void setWebPaymentHeaderSubtitleModeDefault(String v) {
        this.webPaymentHeaderSubtitleModeDefault = com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.normalize(
                v != null ? v : com.pg.urlpay.CheckoutHeaderSubtitleModeUtil.DEFAULT);
    }
    public String getUrlPayShippingAddressUseDefaultYn() { return urlPayShippingAddressUseDefaultYn; }
    public void setUrlPayShippingAddressUseDefaultYn(String v) {
        this.urlPayShippingAddressUseDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "N");
    }
    public String getUrlPayBuyerEmailUseDefaultYn() { return urlPayBuyerEmailUseDefaultYn; }
    public void setUrlPayBuyerEmailUseDefaultYn(String v) {
        this.urlPayBuyerEmailUseDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "Y");
    }
    public String getUrlPayBuyerCountryUseDefaultYn() { return urlPayBuyerCountryUseDefaultYn; }
    public void setUrlPayBuyerCountryUseDefaultYn(String v) {
        this.urlPayBuyerCountryUseDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "Y");
    }
    public String getUrlPayBuyerPhoneUseDefaultYn() { return urlPayBuyerPhoneUseDefaultYn; }
    public void setUrlPayBuyerPhoneUseDefaultYn(String v) {
        this.urlPayBuyerPhoneUseDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "Y");
    }
    public String getUrlPayProductNameUseDefaultYn() { return urlPayProductNameUseDefaultYn; }
    public void setUrlPayProductNameUseDefaultYn(String v) {
        this.urlPayProductNameUseDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "Y");
    }
    public String getUrlPayDefaultProductName() { return urlPayDefaultProductName; }
    public void setUrlPayDefaultProductName(String urlPayDefaultProductName) {
        this.urlPayDefaultProductName = urlPayDefaultProductName != null && !urlPayDefaultProductName.isBlank()
                ? urlPayDefaultProductName.trim() : null;
    }
    public String getUrlPayDefaultProductCode() { return urlPayDefaultProductCode; }
    public void setUrlPayDefaultProductCode(String urlPayDefaultProductCode) {
        this.urlPayDefaultProductCode = urlPayDefaultProductCode != null && !urlPayDefaultProductCode.isBlank()
                ? urlPayDefaultProductCode.trim() : null;
    }
    public java.math.BigDecimal getUrlPayDefaultProductAmount() { return urlPayDefaultProductAmount; }
    public void setUrlPayDefaultProductAmount(java.math.BigDecimal urlPayDefaultProductAmount) {
        this.urlPayDefaultProductAmount = urlPayDefaultProductAmount;
    }
    public String getUrlPayDefaultProductDesc() { return urlPayDefaultProductDesc; }
    public void setUrlPayDefaultProductDesc(String urlPayDefaultProductDesc) {
        this.urlPayDefaultProductDesc = urlPayDefaultProductDesc != null && !urlPayDefaultProductDesc.isBlank()
                ? urlPayDefaultProductDesc.trim() : null;
    }

    public String getApiWordpressPluginEnabledYn() { return apiWordpressPluginEnabledYn; }
    public void setApiWordpressPluginEnabledYn(String apiWordpressPluginEnabledYn) { this.apiWordpressPluginEnabledYn = apiWordpressPluginEnabledYn; }
    public String getUrlPayRepayEnabledYn() { return urlPayRepayEnabledYn; }
    public void setUrlPayRepayEnabledYn(String urlPayRepayEnabledYn) { this.urlPayRepayEnabledYn = urlPayRepayEnabledYn; }
    public String getUrlPayRepayPathTemplate() { return urlPayRepayPathTemplate; }
    public void setUrlPayRepayPathTemplate(String urlPayRepayPathTemplate) { this.urlPayRepayPathTemplate = urlPayRepayPathTemplate; }
    public String getSplitPayContractCancelDefaultYn() { return splitPayContractCancelDefaultYn; }
    public void setSplitPayContractCancelDefaultYn(String v) {
        this.splitPayContractCancelDefaultYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "Y");
    }
    public String getSplitPayContractCancelOrgOpYn() { return splitPayContractCancelOrgOpYn; }
    public void setSplitPayContractCancelOrgOpYn(String v) {
        this.splitPayContractCancelOrgOpYn = com.pg.urlpay.UrlPayFollowHqYnUtil.normalizeHqDefault(v, "N");
    }
    public String getJpaySubscriptionEnabledYn() { return jpaySubscriptionEnabledYn; }
    public void setJpaySubscriptionEnabledYn(String jpaySubscriptionEnabledYn) { this.jpaySubscriptionEnabledYn = jpaySubscriptionEnabledYn; }
    public String getJpaySubscriptionInlineEnabledYn() { return jpaySubscriptionInlineEnabledYn; }
    public void setJpaySubscriptionInlineEnabledYn(String jpaySubscriptionInlineEnabledYn) { this.jpaySubscriptionInlineEnabledYn = jpaySubscriptionInlineEnabledYn; }
    public String getJpaySubscriptionPathTemplate() { return jpaySubscriptionPathTemplate; }
    public void setJpaySubscriptionPathTemplate(String jpaySubscriptionPathTemplate) { this.jpaySubscriptionPathTemplate = jpaySubscriptionPathTemplate; }
    public String getJpaySubscriptionConfigJson() { return jpaySubscriptionConfigJson; }
    public void setJpaySubscriptionConfigJson(String jpaySubscriptionConfigJson) { this.jpaySubscriptionConfigJson = jpaySubscriptionConfigJson; }
    public String getUrlPayFormMode() { return urlPayFormMode; }
    public void setUrlPayFormMode(String urlPayFormMode) { this.urlPayFormMode = urlPayFormMode; }
    public String getJpayCheckoutFieldMode() { return jpayCheckoutFieldMode; }
    public void setJpayCheckoutFieldMode(String jpayCheckoutFieldMode) { this.jpayCheckoutFieldMode = jpayCheckoutFieldMode; }
    public String getJpayPhoneDialCodeYn() { return jpayPhoneDialCodeYn; }
    public void setJpayPhoneDialCodeYn(String jpayPhoneDialCodeYn) { this.jpayPhoneDialCodeYn = jpayPhoneDialCodeYn; }
    public String getUrlPayTabTitleJson() { return urlPayTabTitleJson; }
    public void setUrlPayTabTitleJson(String urlPayTabTitleJson) { this.urlPayTabTitleJson = urlPayTabTitleJson; }
    public String getUrlPayFaviconUrl() { return urlPayFaviconUrl; }
    public void setUrlPayFaviconUrl(String urlPayFaviconUrl) { this.urlPayFaviconUrl = urlPayFaviconUrl; }
    public String getPaymentProviderRegistryJson() { return paymentProviderRegistryJson; }
    public void setPaymentProviderRegistryJson(String paymentProviderRegistryJson) { this.paymentProviderRegistryJson = paymentProviderRegistryJson; }
    public String getPayCurrencyScaleRulesJson() { return payCurrencyScaleRulesJson; }
    public void setPayCurrencyScaleRulesJson(String payCurrencyScaleRulesJson) { this.payCurrencyScaleRulesJson = payCurrencyScaleRulesJson; }
    public String getUrlPayCardCopyConfigJson() { return urlPayCardCopyConfigJson; }
    public void setUrlPayCardCopyConfigJson(String urlPayCardCopyConfigJson) { this.urlPayCardCopyConfigJson = urlPayCardCopyConfigJson; }
    public String getUrlPayDisplayFxJson() { return urlPayDisplayFxJson; }
    public void setUrlPayDisplayFxJson(String urlPayDisplayFxJson) { this.urlPayDisplayFxJson = urlPayDisplayFxJson; }
    public String getBotThailandApiKey() { return botThailandApiKey; }
    public void setBotThailandApiKey(String botThailandApiKey) { this.botThailandApiKey = botThailandApiKey; }
    public String getBotThailandBaseUrl() { return botThailandBaseUrl; }
    public void setBotThailandBaseUrl(String botThailandBaseUrl) { this.botThailandBaseUrl = botThailandBaseUrl; }
    public String getBotThailandDailyAvgPath() { return botThailandDailyAvgPath; }
    public void setBotThailandDailyAvgPath(String botThailandDailyAvgPath) { this.botThailandDailyAvgPath = botThailandDailyAvgPath; }
    public String getBotThailandApiKeyHeader() { return botThailandApiKeyHeader; }
    public void setBotThailandApiKeyHeader(String botThailandApiKeyHeader) { this.botThailandApiKeyHeader = botThailandApiKeyHeader; }
    public String getRecallIncludeFeeYn() { return recallIncludeFeeYn; }
    public void setRecallIncludeFeeYn(String recallIncludeFeeYn) { this.recallIncludeFeeYn = recallIncludeFeeYn; }
    public String getSettlementVatApplyYn() { return settlementVatApplyYn; }
    public void setSettlementVatApplyYn(String settlementVatApplyYn) { this.settlementVatApplyYn = settlementVatApplyYn; }
    public String getBusinessDaySettingsJson() { return businessDaySettingsJson; }
    public void setBusinessDaySettingsJson(String businessDaySettingsJson) { this.businessDaySettingsJson = businessDaySettingsJson; }
    public String getHqDefaultBusinessDayProfileId() { return hqDefaultBusinessDayProfileId; }
    public void setHqDefaultBusinessDayProfileId(String hqDefaultBusinessDayProfileId) { this.hqDefaultBusinessDayProfileId = hqDefaultBusinessDayProfileId; }
    public String getPublicAdminSiteUrl() { return publicAdminSiteUrl; }
    public void setPublicAdminSiteUrl(String publicAdminSiteUrl) { this.publicAdminSiteUrl = publicAdminSiteUrl; }
    public String getPublicApiBaseUrl() { return publicApiBaseUrl; }
    public void setPublicApiBaseUrl(String publicApiBaseUrl) { this.publicApiBaseUrl = publicApiBaseUrl; }
    public String getServerManageSslCertPath() { return serverManageSslCertPath; }
    public void setServerManageSslCertPath(String serverManageSslCertPath) { this.serverManageSslCertPath = serverManageSslCertPath; }
    public String getServerManageSslLeDomain() { return serverManageSslLeDomain; }
    public void setServerManageSslLeDomain(String serverManageSslLeDomain) { this.serverManageSslLeDomain = serverManageSslLeDomain; }
    public Integer getServerManageContractDiskMb() { return serverManageContractDiskMb; }
    public void setServerManageContractDiskMb(Integer serverManageContractDiskMb) { this.serverManageContractDiskMb = serverManageContractDiskMb; }
    public Integer getServerManageContractTrafficMb() { return serverManageContractTrafficMb; }
    public void setServerManageContractTrafficMb(Integer serverManageContractTrafficMb) { this.serverManageContractTrafficMb = serverManageContractTrafficMb; }
    public LocalDate getServerManageContractStart() { return serverManageContractStart; }
    public void setServerManageContractStart(LocalDate serverManageContractStart) { this.serverManageContractStart = serverManageContractStart; }
    public LocalDate getServerManageContractEnd() { return serverManageContractEnd; }
    public void setServerManageContractEnd(LocalDate serverManageContractEnd) { this.serverManageContractEnd = serverManageContractEnd; }
    public Integer getServerManageTrafficUsedMb() { return serverManageTrafficUsedMb; }
    public void setServerManageTrafficUsedMb(Integer serverManageTrafficUsedMb) { this.serverManageTrafficUsedMb = serverManageTrafficUsedMb; }
    public Integer getServerManageUiRefreshSec() { return serverManageUiRefreshSec; }
    public void setServerManageUiRefreshSec(Integer serverManageUiRefreshSec) { this.serverManageUiRefreshSec = serverManageUiRefreshSec; }
    public String getMultiPgRoutingEnabledYn() { return multiPgRoutingEnabledYn; }
    public void setMultiPgRoutingEnabledYn(String multiPgRoutingEnabledYn) { this.multiPgRoutingEnabledYn = multiPgRoutingEnabledYn; }
    public String getMultiPgRoutingMode() { return multiPgRoutingMode; }
    public void setMultiPgRoutingMode(String multiPgRoutingMode) { this.multiPgRoutingMode = multiPgRoutingMode; }
    public String getEximbayMethodsVisible() { return eximbayMethodsVisible; }
    public void setEximbayMethodsVisible(String eximbayMethodsVisible) {
        this.eximbayMethodsVisible = com.pg.service.EximbayPaymentMethodCatalog.toCsv(
                com.pg.service.EximbayPaymentMethodCatalog.resolveVisible(eximbayMethodsVisible));
    }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
