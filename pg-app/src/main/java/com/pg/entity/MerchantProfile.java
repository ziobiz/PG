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

    @Column(name = "remark", length = 500)
    private String remark;

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
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

