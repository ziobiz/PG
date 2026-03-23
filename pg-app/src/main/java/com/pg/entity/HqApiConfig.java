package com.pg.entity;

import jakarta.persistence.*;
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

    /** 환수금에서 수수료 포함 여부 (Y/N) */
    @Column(name = "recall_include_fee_yn", length = 1)
    private String recallIncludeFeeYn = "N";

    /** 정산 관련 VAT 부과 여부 (Y/N) */
    @Column(name = "settlement_vat_apply_yn", length = 1)
    private String settlementVatApplyYn = "Y";

    /** 본사 영업일 설정 목록 JSON */
    @Column(name = "business_day_settings_json", columnDefinition = "TEXT")
    private String businessDaySettingsJson;

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
    public String getRecallIncludeFeeYn() { return recallIncludeFeeYn; }
    public void setRecallIncludeFeeYn(String recallIncludeFeeYn) { this.recallIncludeFeeYn = recallIncludeFeeYn; }
    public String getSettlementVatApplyYn() { return settlementVatApplyYn; }
    public void setSettlementVatApplyYn(String settlementVatApplyYn) { this.settlementVatApplyYn = settlementVatApplyYn; }
    public String getBusinessDaySettingsJson() { return businessDaySettingsJson; }
    public void setBusinessDaySettingsJson(String businessDaySettingsJson) { this.businessDaySettingsJson = businessDaySettingsJson; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
