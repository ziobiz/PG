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
}
