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

    /** 환수금에서 수수료 포함 여부 (Y/N) */
    @Column(name = "recall_include_fee_yn", length = 1)
    private String recallIncludeFeeYn = "N";

    /** 정산 관련 VAT 부과 여부 (Y/N) */
    @Column(name = "settlement_vat_apply_yn", length = 1)
    private String settlementVatApplyYn = "Y";

    /** 본사 영업일 설정 목록 JSON */
    @Column(name = "business_day_settings_json", columnDefinition = "TEXT")
    private String businessDaySettingsJson;

    /** 관리자(웹) 공개 URL — 안내·문서용 */
    @Column(name = "public_admin_site_url", length = 500)
    private String publicAdminSiteUrl;

    /** API 공개 베이스 URL — 안내용 */
    @Column(name = "public_api_base_url", length = 500)
    private String publicApiBaseUrl;

    /** 서버관리: 모니터링할 fullchain.pem 경로(비우면 ENV 또는 LE 자동) */
    @Column(name = "server_manage_ssl_cert_path", length = 500)
    private String serverManageSslCertPath;

    /** 서버관리: Let's Encrypt live 폴더명(다중 도메인 시) */
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

    /** 서버관리 대시보드 자동 갱신 간격(초). NULL이면 application.yml app.serverManage.uiAutoRefreshSeconds */
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
    public String getRecallIncludeFeeYn() { return recallIncludeFeeYn; }
    public void setRecallIncludeFeeYn(String recallIncludeFeeYn) { this.recallIncludeFeeYn = recallIncludeFeeYn; }
    public String getSettlementVatApplyYn() { return settlementVatApplyYn; }
    public void setSettlementVatApplyYn(String settlementVatApplyYn) { this.settlementVatApplyYn = settlementVatApplyYn; }
    public String getBusinessDaySettingsJson() { return businessDaySettingsJson; }
    public void setBusinessDaySettingsJson(String businessDaySettingsJson) { this.businessDaySettingsJson = businessDaySettingsJson; }
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
