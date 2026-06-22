package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_split_pay_email_phase")
public class SplitPayEmailPhase {

    @Id
    @Column(name = "phase", length = 16)
    private String phase;

    @Column(name = "mail_from_address", length = 255)
    private String mailFromAddress;

    @Column(name = "mail_from_name", length = 200)
    private String mailFromName;

    @Column(name = "alert_recipient_emails", columnDefinition = "TEXT")
    private String alertRecipientEmails;

    @Column(name = "test_recipient_email", length = 255)
    private String testRecipientEmail;

    @Column(name = "subject_kor", length = 500)
    private String subjectKor;

    @Column(name = "body_kor", columnDefinition = "TEXT")
    private String bodyKor;

    @Column(name = "subject_eng", length = 500)
    private String subjectEng;

    @Column(name = "body_eng", columnDefinition = "TEXT")
    private String bodyEng;

    @Column(name = "subject_jpn", length = 500)
    private String subjectJpn;

    @Column(name = "body_jpn", columnDefinition = "TEXT")
    private String bodyJpn;

    @Column(name = "subject_chn", length = 500)
    private String subjectChn;

    @Column(name = "body_chn", columnDefinition = "TEXT")
    private String bodyChn;

    @Column(name = "subject_tha", length = 500)
    private String subjectTha;

    @Column(name = "body_tha", columnDefinition = "TEXT")
    private String bodyTha;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getMailFromAddress() { return mailFromAddress; }
    public void setMailFromAddress(String mailFromAddress) { this.mailFromAddress = mailFromAddress; }
    public String getMailFromName() { return mailFromName; }
    public void setMailFromName(String mailFromName) { this.mailFromName = mailFromName; }
    public String getAlertRecipientEmails() { return alertRecipientEmails; }
    public void setAlertRecipientEmails(String alertRecipientEmails) { this.alertRecipientEmails = alertRecipientEmails; }
    public String getTestRecipientEmail() { return testRecipientEmail; }
    public void setTestRecipientEmail(String testRecipientEmail) { this.testRecipientEmail = testRecipientEmail; }
    public String getSubjectKor() { return subjectKor; }
    public void setSubjectKor(String subjectKor) { this.subjectKor = subjectKor; }
    public String getBodyKor() { return bodyKor; }
    public void setBodyKor(String bodyKor) { this.bodyKor = bodyKor; }
    public String getSubjectEng() { return subjectEng; }
    public void setSubjectEng(String subjectEng) { this.subjectEng = subjectEng; }
    public String getBodyEng() { return bodyEng; }
    public void setBodyEng(String bodyEng) { this.bodyEng = bodyEng; }
    public String getSubjectJpn() { return subjectJpn; }
    public void setSubjectJpn(String subjectJpn) { this.subjectJpn = subjectJpn; }
    public String getBodyJpn() { return bodyJpn; }
    public void setBodyJpn(String bodyJpn) { this.bodyJpn = bodyJpn; }
    public String getSubjectChn() { return subjectChn; }
    public void setSubjectChn(String subjectChn) { this.subjectChn = subjectChn; }
    public String getBodyChn() { return bodyChn; }
    public void setBodyChn(String bodyChn) { this.bodyChn = bodyChn; }
    public String getSubjectTha() { return subjectTha; }
    public void setSubjectTha(String subjectTha) { this.subjectTha = subjectTha; }
    public String getBodyTha() { return bodyTha; }
    public void setBodyTha(String bodyTha) { this.bodyTha = bodyTha; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
