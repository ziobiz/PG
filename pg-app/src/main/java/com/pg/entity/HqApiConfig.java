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
     * URL 공개 결제 페이지(/pay/…) 입력 필드 구성.
     * FULL: 청구지·성명 분리 등 전체, SIMPLE: 상품·연락·금액 중심 간편 폼.
     */
    @Column(name = "url_pay_form_mode", length = 20)
    private String urlPayFormMode = "FULL";

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
    public String getUrlPayFormMode() { return urlPayFormMode; }
    public void setUrlPayFormMode(String urlPayFormMode) { this.urlPayFormMode = urlPayFormMode; }
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
