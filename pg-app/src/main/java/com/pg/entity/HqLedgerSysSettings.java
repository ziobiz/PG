package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 본사설정 &gt; 전산설정관리 — NOTI 미들웨어 시스템/환경설정에 대응하는 단일 행 설정.
 */
@Entity
@Table(name = "tb_hq_ledger_sys_settings")
public class HqLedgerSysSettings {

    @Id
    @Column(nullable = false)
    private Long id;

    @Column(name = "display_timezone", length = 64)
    private String displayTimezone;

    @Column(name = "ntp_sync_enabled_yn", nullable = false, length = 1)
    private String ntpSyncEnabledYn = "N";

    @Column(name = "ntp_server_list", length = 500)
    private String ntpServerList;

    @Column(name = "time_sync_interval_min")
    private Integer timeSyncIntervalMin;

    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_tls_yn", nullable = false, length = 1)
    private String smtpTlsYn = "Y";

    @Column(name = "smtp_auth_yn", nullable = false, length = 1)
    private String smtpAuthYn = "Y";

    @Column(name = "smtp_username", length = 255)
    private String smtpUsername;

    @Column(name = "smtp_password", length = 512)
    private String smtpPassword;

    @Column(name = "mail_from_address", length = 255)
    private String mailFromAddress;

    @Column(name = "mail_from_name", length = 200)
    private String mailFromName;

    @Column(name = "alert_recipient_emails", columnDefinition = "TEXT")
    private String alertRecipientEmails;

    @Column(name = "email_on_sync_failure_yn", nullable = false, length = 1)
    private String emailOnSyncFailureYn = "N";

    @Column(name = "email_daily_digest_yn", nullable = false, length = 1)
    private String emailDailyDigestYn = "N";

    @Column(name = "email_notify_void_batch_yn", nullable = false, length = 1)
    private String emailNotifyVoidBatchYn = "N";

    @Column(name = "email_notify_refund_batch_yn", nullable = false, length = 1)
    private String emailNotifyRefundBatchYn = "N";

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    /**
     * 데이터 유형별 보관 일수. JSON 객체 {@code { "PG_NOTIFY_INBOUND": 90, ... }} — 키는 {@link com.pg.catalog.DataRetentionCatalog} id.
     */
    @Column(name = "data_retention_policy_json", columnDefinition = "TEXT")
    private String dataRetentionPolicyJson;

    /** 수동무효(이메일) 수신처 — 기본 help@chillpay.co 는 서비스에서 보완 */
    @Column(name = "email_void_to", length = 255)
    private String emailVoidTo;

    @Column(name = "email_void_subject", length = 500)
    private String emailVoidSubject;

    @Column(name = "email_void_body_template", columnDefinition = "TEXT")
    private String emailVoidBodyTemplate;

    @Column(name = "email_void_company_name", length = 200)
    private String emailVoidCompanyName;

    @Column(name = "email_void_contact_name", length = 200)
    private String emailVoidContactName;

    /** 통합내역: 검색 초기화 시 과거 몇 개월(기본 3) */
    @Column(name = "chillpay_tr_init_sync_months", nullable = false)
    private Integer chillpayTrInitSyncMonths = 3;

    /** 통합내역: TransactionDate 미지정 조회 시 최근 며칠(포함, 기본 2) */
    @Column(name = "chillpay_tr_recent_sync_days", nullable = false)
    private Integer chillpayTrRecentSyncDays = 2;

    @Column(name = "app_log_memory_retention_days", nullable = false)
    private Integer appLogMemoryRetentionDays = 30;

    @Column(name = "app_log_file_retention_days", nullable = false)
    private Integer appLogFileRetentionDays = 90;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = 1L;
        }
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = n;
        }
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDisplayTimezone() { return displayTimezone; }
    public void setDisplayTimezone(String displayTimezone) { this.displayTimezone = displayTimezone; }
    public String getNtpSyncEnabledYn() { return ntpSyncEnabledYn; }
    public void setNtpSyncEnabledYn(String ntpSyncEnabledYn) { this.ntpSyncEnabledYn = ntpSyncEnabledYn; }
    public String getNtpServerList() { return ntpServerList; }
    public void setNtpServerList(String ntpServerList) { this.ntpServerList = ntpServerList; }
    public Integer getTimeSyncIntervalMin() { return timeSyncIntervalMin; }
    public void setTimeSyncIntervalMin(Integer timeSyncIntervalMin) { this.timeSyncIntervalMin = timeSyncIntervalMin; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }
    public String getSmtpTlsYn() { return smtpTlsYn; }
    public void setSmtpTlsYn(String smtpTlsYn) { this.smtpTlsYn = smtpTlsYn; }
    public String getSmtpAuthYn() { return smtpAuthYn; }
    public void setSmtpAuthYn(String smtpAuthYn) { this.smtpAuthYn = smtpAuthYn; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }
    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }
    public String getMailFromAddress() { return mailFromAddress; }
    public void setMailFromAddress(String mailFromAddress) { this.mailFromAddress = mailFromAddress; }
    public String getMailFromName() { return mailFromName; }
    public void setMailFromName(String mailFromName) { this.mailFromName = mailFromName; }
    public String getAlertRecipientEmails() { return alertRecipientEmails; }
    public void setAlertRecipientEmails(String alertRecipientEmails) { this.alertRecipientEmails = alertRecipientEmails; }
    public String getEmailOnSyncFailureYn() { return emailOnSyncFailureYn; }
    public void setEmailOnSyncFailureYn(String emailOnSyncFailureYn) { this.emailOnSyncFailureYn = emailOnSyncFailureYn; }
    public String getEmailDailyDigestYn() { return emailDailyDigestYn; }
    public void setEmailDailyDigestYn(String emailDailyDigestYn) { this.emailDailyDigestYn = emailDailyDigestYn; }
    public String getEmailNotifyVoidBatchYn() { return emailNotifyVoidBatchYn; }
    public void setEmailNotifyVoidBatchYn(String emailNotifyVoidBatchYn) { this.emailNotifyVoidBatchYn = emailNotifyVoidBatchYn; }
    public String getEmailNotifyRefundBatchYn() { return emailNotifyRefundBatchYn; }
    public void setEmailNotifyRefundBatchYn(String emailNotifyRefundBatchYn) { this.emailNotifyRefundBatchYn = emailNotifyRefundBatchYn; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getDataRetentionPolicyJson() { return dataRetentionPolicyJson; }
    public void setDataRetentionPolicyJson(String dataRetentionPolicyJson) { this.dataRetentionPolicyJson = dataRetentionPolicyJson; }
    public String getEmailVoidTo() { return emailVoidTo; }
    public void setEmailVoidTo(String emailVoidTo) { this.emailVoidTo = emailVoidTo; }
    public String getEmailVoidSubject() { return emailVoidSubject; }
    public void setEmailVoidSubject(String emailVoidSubject) { this.emailVoidSubject = emailVoidSubject; }
    public String getEmailVoidBodyTemplate() { return emailVoidBodyTemplate; }
    public void setEmailVoidBodyTemplate(String emailVoidBodyTemplate) { this.emailVoidBodyTemplate = emailVoidBodyTemplate; }
    public String getEmailVoidCompanyName() { return emailVoidCompanyName; }
    public void setEmailVoidCompanyName(String emailVoidCompanyName) { this.emailVoidCompanyName = emailVoidCompanyName; }
    public String getEmailVoidContactName() { return emailVoidContactName; }
    public void setEmailVoidContactName(String emailVoidContactName) { this.emailVoidContactName = emailVoidContactName; }
    public Integer getChillpayTrInitSyncMonths() { return chillpayTrInitSyncMonths; }
    public void setChillpayTrInitSyncMonths(Integer chillpayTrInitSyncMonths) { this.chillpayTrInitSyncMonths = chillpayTrInitSyncMonths; }
    public Integer getChillpayTrRecentSyncDays() { return chillpayTrRecentSyncDays; }
    public void setChillpayTrRecentSyncDays(Integer chillpayTrRecentSyncDays) { this.chillpayTrRecentSyncDays = chillpayTrRecentSyncDays; }
    public Integer getAppLogMemoryRetentionDays() { return appLogMemoryRetentionDays; }
    public void setAppLogMemoryRetentionDays(Integer appLogMemoryRetentionDays) { this.appLogMemoryRetentionDays = appLogMemoryRetentionDays; }
    public Integer getAppLogFileRetentionDays() { return appLogFileRetentionDays; }
    public void setAppLogFileRetentionDays(Integer appLogFileRetentionDays) { this.appLogFileRetentionDays = appLogFileRetentionDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
