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

    @Column(name = "addr_etc", length = 255)
    private String addrEtc;

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

    /** 사이트개요 */
    @Column(name = "site_summary", length = 500)
    private String siteSummary;

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

    @Column(name = "crypto_transfer_fee", length = 50)
    private String cryptoTransferFee;

    @Column(name = "account_no", length = 50)
    private String accountNo;

    @Column(name = "account_holder", length = 100)
    private String accountHolder;

    @Column(name = "country_cd", length = 10)
    private String countryCd;

    /** 주소 국가 (기본정보) JP/KR/TH 또는 기타 시 국가명 */
    @Column(name = "addr_country_cd", length = 20)
    private String addrCountryCd;

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

    /** 챗봇결제 사용여부 (가맹점) - 미사용 시 관리자 챗봇 메뉴 비표시 */
    @Column(name = "chatbot_payment_use_yn", length = 1)
    private String chatbotPaymentUseYn = "N";

    /** 챗봇 카탈로그 등록 가능 상품 수(10·20·50·80·100·150·200). 월 이용료·등록 건수 제한 */
    @Column(name = "chatbot_product_slot_limit")
    private Integer chatbotProductSlotLimit;

    /** 고객 대면 챗봇 안내 — 회사명(비우면 조직명) */
    @Column(name = "chatbot_kb_company_nm", length = 200)
    private String chatbotKbCompanyNm;

    /** 고객 대면 챗봇 안내 — 주소 */
    @Column(name = "chatbot_kb_addr", length = 600)
    private String chatbotKbAddr;

    @Column(name = "chatbot_kb_tel", length = 100)
    private String chatbotKbTel;

    @Column(name = "chatbot_kb_email", length = 120)
    private String chatbotKbEmail;

    @Column(name = "chatbot_kb_contact_nm", length = 100)
    private String chatbotKbContactNm;

    @Column(name = "chatbot_kb_intro", columnDefinition = "TEXT")
    private String chatbotKbIntro;

    @Column(name = "chatbot_kb_product_desc", columnDefinition = "TEXT")
    private String chatbotKbProductDesc;

    /** 공개 챗봇 결제 페이지 상단 로고(URL). 미설정 시 상위 본사·총판 브랜딩 로고 사용 */
    @Column(name = "chatbot_header_logo_url", length = 500)
    private String chatbotHeaderLogoUrl;

    /** 챗봇에서 상품 등록 허용 관리자(tb_user.id), 가맹당 1명 */
    @Column(name = "chatbot_admin_user_id")
    private Long chatbotAdminUserId;

    /** URL·챗봇 인라인(DirectCredit) 승인 시 가맹점 대표 이메일로 알림 */
    @Column(name = "url_pay_alert_email_yn", length = 1)
    private String urlPayAlertEmailYn = "N";

    /** LINE Notify(https://notify-bot.line.me/) 발급 토큰. 비면 미사용 */
    @Column(name = "url_pay_line_notify_token", length = 256)
    private String urlPayLineNotifyToken;

    /** 기준 화폐. 본사: 최대 3종 comma구분 (KRW,USD,JPY). 총판: 1종만 */
    @Column(name = "base_currency", length = 30)
    private String baseCurrency;

    /** 터미널[단말] 개수 */
    @Column(name = "terminal_count_terminal")
    private Integer terminalCountTerminal;

    /** 터미널[웹] 개수 */
    @Column(name = "terminal_count_web")
    private Integer terminalCountWeb;

    /** 본사(REGIONAL) 전용 JSON 설정 */
    @Column(name = "regional_settings", columnDefinition = "TEXT")
    private String regionalSettings;

    /** 가맹점 관리자 결제 후속조치 사용 (NULL=기존 호환 허용) */
    @Column(name = "pay_follow_merchant_use_yn", length = 1)
    private String payFollowMerchantUseYn;

    @Column(name = "pay_follow_auto_void_yn", length = 1)
    private String payFollowAutoVoidYn;

    @Column(name = "pay_follow_email_void_yn", length = 1)
    private String payFollowEmailVoidYn;

    @Column(name = "pay_follow_auto_refund_yn", length = 1)
    private String payFollowAutoRefundYn;

    @Column(name = "pay_follow_force_refund_yn", length = 1)
    private String payFollowForceRefundYn;

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
    public String getAddrEtc() { return addrEtc; }
    public void setAddrEtc(String addrEtc) { this.addrEtc = addrEtc; }
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
    public String getSiteSummary() { return siteSummary; }
    public void setSiteSummary(String siteSummary) { this.siteSummary = siteSummary; }
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
    public String getCryptoTransferFee() { return cryptoTransferFee; }
    public void setCryptoTransferFee(String cryptoTransferFee) { this.cryptoTransferFee = cryptoTransferFee; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    public String getCountryCd() { return countryCd; }
    public void setCountryCd(String countryCd) { this.countryCd = countryCd; }
    public String getAddrCountryCd() { return addrCountryCd; }
    public void setAddrCountryCd(String addrCountryCd) { this.addrCountryCd = addrCountryCd; }
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
    public String getChatbotPaymentUseYn() { return chatbotPaymentUseYn; }
    public void setChatbotPaymentUseYn(String chatbotPaymentUseYn) { this.chatbotPaymentUseYn = chatbotPaymentUseYn; }
    public Integer getChatbotProductSlotLimit() { return chatbotProductSlotLimit; }
    public void setChatbotProductSlotLimit(Integer chatbotProductSlotLimit) {
        this.chatbotProductSlotLimit = chatbotProductSlotLimit;
    }
    public String getChatbotKbCompanyNm() { return chatbotKbCompanyNm; }
    public void setChatbotKbCompanyNm(String chatbotKbCompanyNm) { this.chatbotKbCompanyNm = chatbotKbCompanyNm; }
    public String getChatbotKbAddr() { return chatbotKbAddr; }
    public void setChatbotKbAddr(String chatbotKbAddr) { this.chatbotKbAddr = chatbotKbAddr; }
    public String getChatbotKbTel() { return chatbotKbTel; }
    public void setChatbotKbTel(String chatbotKbTel) { this.chatbotKbTel = chatbotKbTel; }
    public String getChatbotKbEmail() { return chatbotKbEmail; }
    public void setChatbotKbEmail(String chatbotKbEmail) { this.chatbotKbEmail = chatbotKbEmail; }
    public String getChatbotKbContactNm() { return chatbotKbContactNm; }
    public void setChatbotKbContactNm(String chatbotKbContactNm) { this.chatbotKbContactNm = chatbotKbContactNm; }
    public String getChatbotKbIntro() { return chatbotKbIntro; }
    public void setChatbotKbIntro(String chatbotKbIntro) { this.chatbotKbIntro = chatbotKbIntro; }
    public String getChatbotKbProductDesc() { return chatbotKbProductDesc; }
    public void setChatbotKbProductDesc(String chatbotKbProductDesc) { this.chatbotKbProductDesc = chatbotKbProductDesc; }
    public String getChatbotHeaderLogoUrl() { return chatbotHeaderLogoUrl; }
    public void setChatbotHeaderLogoUrl(String chatbotHeaderLogoUrl) { this.chatbotHeaderLogoUrl = chatbotHeaderLogoUrl; }
    public Long getChatbotAdminUserId() { return chatbotAdminUserId; }
    public void setChatbotAdminUserId(Long chatbotAdminUserId) { this.chatbotAdminUserId = chatbotAdminUserId; }
    public String getUrlPayAlertEmailYn() { return urlPayAlertEmailYn; }
    public void setUrlPayAlertEmailYn(String urlPayAlertEmailYn) { this.urlPayAlertEmailYn = urlPayAlertEmailYn; }
    public String getUrlPayLineNotifyToken() { return urlPayLineNotifyToken; }
    public void setUrlPayLineNotifyToken(String urlPayLineNotifyToken) { this.urlPayLineNotifyToken = urlPayLineNotifyToken; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    public Integer getTerminalCountTerminal() { return terminalCountTerminal; }
    public void setTerminalCountTerminal(Integer terminalCountTerminal) { this.terminalCountTerminal = terminalCountTerminal; }
    public Integer getTerminalCountWeb() { return terminalCountWeb; }
    public void setTerminalCountWeb(Integer terminalCountWeb) { this.terminalCountWeb = terminalCountWeb; }
    public String getRegionalSettings() { return regionalSettings; }
    public void setRegionalSettings(String regionalSettings) { this.regionalSettings = regionalSettings; }
    public String getPayFollowMerchantUseYn() { return payFollowMerchantUseYn; }
    public void setPayFollowMerchantUseYn(String payFollowMerchantUseYn) { this.payFollowMerchantUseYn = payFollowMerchantUseYn; }
    public String getPayFollowAutoVoidYn() { return payFollowAutoVoidYn; }
    public void setPayFollowAutoVoidYn(String payFollowAutoVoidYn) { this.payFollowAutoVoidYn = payFollowAutoVoidYn; }
    public String getPayFollowEmailVoidYn() { return payFollowEmailVoidYn; }
    public void setPayFollowEmailVoidYn(String payFollowEmailVoidYn) { this.payFollowEmailVoidYn = payFollowEmailVoidYn; }
    public String getPayFollowAutoRefundYn() { return payFollowAutoRefundYn; }
    public void setPayFollowAutoRefundYn(String payFollowAutoRefundYn) { this.payFollowAutoRefundYn = payFollowAutoRefundYn; }
    public String getPayFollowForceRefundYn() { return payFollowForceRefundYn; }
    public void setPayFollowForceRefundYn(String payFollowForceRefundYn) { this.payFollowForceRefundYn = payFollowForceRefundYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

