package com.pg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_org_level_pay_follow_cap")
public class OrgLevelPayFollowCap {

    @Id
    @Column(name = "org_level", length = 20)
    private String orgLevel;

    @Column(name = "auto_void_yn", nullable = false, length = 1)
    private String autoVoidYn = "Y";

    @Column(name = "email_void_yn", nullable = false, length = 1)
    private String emailVoidYn = "Y";

    @Column(name = "auto_refund_yn", nullable = false, length = 1)
    private String autoRefundYn = "Y";

    @Column(name = "force_refund_yn", nullable = false, length = 1)
    private String forceRefundYn = "Y";

    @Column(name = "manual_void_yn", nullable = false, length = 1)
    private String manualVoidYn = "Y";

    @Column(name = "manual_refund_yn", nullable = false, length = 1)
    private String manualRefundYn = "Y";

    /** URL 결제 당일환불. 환불처리와 별도. 기본 N */
    @Column(name = "same_day_refund_yn", nullable = false, length = 1)
    private String sameDayRefundYn = "N";

    public String getOrgLevel() { return orgLevel; }
    public void setOrgLevel(String orgLevel) { this.orgLevel = orgLevel; }
    public String getAutoVoidYn() { return autoVoidYn; }
    public void setAutoVoidYn(String autoVoidYn) { this.autoVoidYn = autoVoidYn; }
    public String getEmailVoidYn() { return emailVoidYn; }
    public void setEmailVoidYn(String emailVoidYn) { this.emailVoidYn = emailVoidYn; }
    public String getAutoRefundYn() { return autoRefundYn; }
    public void setAutoRefundYn(String autoRefundYn) { this.autoRefundYn = autoRefundYn; }
    public String getForceRefundYn() { return forceRefundYn; }
    public void setForceRefundYn(String forceRefundYn) { this.forceRefundYn = forceRefundYn; }
    public String getManualVoidYn() { return manualVoidYn; }
    public void setManualVoidYn(String manualVoidYn) { this.manualVoidYn = manualVoidYn; }
    public String getManualRefundYn() { return manualRefundYn; }
    public void setManualRefundYn(String manualRefundYn) { this.manualRefundYn = manualRefundYn; }
    public String getSameDayRefundYn() { return sameDayRefundYn; }
    public void setSameDayRefundYn(String sameDayRefundYn) { this.sameDayRefundYn = sameDayRefundYn; }
}
