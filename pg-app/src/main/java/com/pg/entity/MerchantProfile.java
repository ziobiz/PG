package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 가맹점/영업조직 상세 정보 (업체관리 등록 화면의 상세 필드 보관)
 */
@Entity
@Table(name = "tb_merchant_profile")
public class MerchantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** OrgUnit.id (총본사/지사/대리점/가맹점 공통) */
    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "comp_div", length = 20)
    private String compDiv;

    @Column(name = "tel", length = 50)
    private String compTel;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "addr", length = 255)
    private String addr;

    @Column(name = "addr_detail", length = 255)
    private String addrDetail;

    @Column(name = "ceo_nm", length = 100)
    private String ceoNm;

    @Column(name = "ceo_mobile", length = 50)
    private String ceoMobile;

    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "login_id", length = 50)
    private String loginId;

    @Column(name = "reg_no", length = 50)
    private String regNo;

    /** 업태 (사업자등록증) */
    @Column(name = "biz_type", length = 100)
    private String bizType;

    /** 종목 (사업자등록증) */
    @Column(name = "industry", length = 100)
    private String industry;

    /** 사업자형태 (가맹점 전용) */
    @Column(name = "biz_nature", length = 100)
    private String bizNature;

    /** 취급물품 (가맹점 전용) */
    @Column(name = "product", length = 100)
    private String product;

    /** 대표사이트 (가맹점 전용) */
    @Column(name = "homepage", length = 255)
    private String homepage;

    /** 사이트 주소 */
    @Column(name = "site_url", length = 255)
    private String siteUrl;

    /** 정산담당자명 (가맹점 전용) */
    @Column(name = "settle_name", length = 100)
    private String settleName;

    /** 정산담당자연락처 (가맹점 전용) */
    @Column(name = "settle_tel_no", length = 50)
    private String settleTelNo;

    /** 정산형태 (총판/지사/대리점): M=가맹점별정산, G=총판정산 */
    @Column(name = "settle_type", length = 5)
    private String settleType;

    /** 요율 (총판/지사/대리점) */
    @Column(name = "commission_rate", precision = 10, scale = 4)
    private java.math.BigDecimal commissionRate;

    /** 사용한도 (총판/지사/대리점) */
    @Column(name = "limit_amt", precision = 18, scale = 0)
    private java.math.BigDecimal limitAmt;

    @Column(name = "fax", length = 50)
    private String fax;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "pwd", length = 200)
    private String pwd;

    @Column(name = "bank_cd", length = 20)
    private String bankCd;

    @Column(name = "transfer_fee", length = 50)
    private String transferFee;

    @Column(name = "account_no", length = 50)
    private String accountNo;

    @Column(name = "account_holder", length = 100)
    private String accountHolder;

    @Column(name = "country_cd", length = 10)
    private String countryCd;

    @Column(name = "swift", length = 50)
    private String swift;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "branch_addr", length = 255)
    private String branchAddr;

    @Column(name = "contact_tel", length = 50)
    private String contactTel;

    @Column(name = "wallet_address", length = 255)
    private String walletAddress;

    @Column(name = "network_name", length = 50)
    private String networkName;

    @Column(name = "remark", length = 500)
    private String remark;

    /** 수수료 설정 권한 (총본사가 본사/총판에 부여) Y/N */
    @Column(name = "commission_config_allowed", length = 1)
    private String commissionConfigAllowed = "N";

    /** 웹결제 사용여부 (가맹점) - 미사용 시 WEB 결제 시스템 중지 */
    @Column(name = "web_payment_use_yn", length = 1)
    private String webPaymentUseYn = "Y";

    /** 기준 화폐 (본사: USD, JPY, KRW 등 - 1화폐 1본사) */
    @Column(name = "base_currency", length = 10)
    private String baseCurrency;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompDiv() { return compDiv; }
    public void setCompDiv(String compDiv) { this.compDiv = compDiv; }
    public String getCompTel() { return compTel; }
    public void setCompTel(String compTel) { this.compTel = compTel; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }
    public String getAddrDetail() { return addrDetail; }
    public void setAddrDetail(String addrDetail) { this.addrDetail = addrDetail; }
    public String getCeoNm() { return ceoNm; }
    public void setCeoNm(String ceoNm) { this.ceoNm = ceoNm; }
    public String getCeoMobile() { return ceoMobile; }
    public void setCeoMobile(String ceoMobile) { this.ceoMobile = ceoMobile; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getBizNature() { return bizNature; }
    public void setBizNature(String bizNature) { this.bizNature = bizNature; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getSettleName() { return settleName; }
    public void setSettleName(String settleName) { this.settleName = settleName; }
    public String getSettleTelNo() { return settleTelNo; }
    public void setSettleTelNo(String settleTelNo) { this.settleTelNo = settleTelNo; }
    public String getSettleType() { return settleType; }
    public void setSettleType(String settleType) { this.settleType = settleType; }
    public java.math.BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(java.math.BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public java.math.BigDecimal getLimitAmt() { return limitAmt; }
    public void setLimitAmt(java.math.BigDecimal limitAmt) { this.limitAmt = limitAmt; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public String getBankCd() { return bankCd; }
    public void setBankCd(String bankCd) { this.bankCd = bankCd; }
    public String getTransferFee() { return transferFee; }
    public void setTransferFee(String transferFee) { this.transferFee = transferFee; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    public String getCountryCd() { return countryCd; }
    public void setCountryCd(String countryCd) { this.countryCd = countryCd; }
    public String getSwift() { return swift; }
    public void setSwift(String swift) { this.swift = swift; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getBranchAddr() { return branchAddr; }
    public void setBranchAddr(String branchAddr) { this.branchAddr = branchAddr; }
    public String getContactTel() { return contactTel; }
    public void setContactTel(String contactTel) { this.contactTel = contactTel; }
    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
    public String getNetworkName() { return networkName; }
    public void setNetworkName(String networkName) { this.networkName = networkName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCommissionConfigAllowed() { return commissionConfigAllowed; }
    public void setCommissionConfigAllowed(String commissionConfigAllowed) { this.commissionConfigAllowed = commissionConfigAllowed; }
    public String getWebPaymentUseYn() { return webPaymentUseYn; }
    public void setWebPaymentUseYn(String webPaymentUseYn) { this.webPaymentUseYn = webPaymentUseYn; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

