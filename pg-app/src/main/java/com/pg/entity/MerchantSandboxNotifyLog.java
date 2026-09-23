package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_merchant_sandbox_notify_log")
public class MerchantSandboxNotifyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "comp_id", nullable = false, length = 64)
    private String compId;

    @Column(name = "sandbox_txn_id")
    private Long sandboxTxnId;

    @Column(name = "order_no", length = 100)
    private String orderNo;

    @Column(name = "url_type", nullable = false, length = 32)
    private String urlType;

    @Column(name = "target_url", nullable = false, length = 1000)
    private String targetUrl;

    @Column(name = "result_status", nullable = false, length = 16)
    private String resultStatus;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "retry_cnt", nullable = false)
    private int retryCnt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "payload_body", columnDefinition = "TEXT")
    private String payloadBody;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getCompId() { return compId; }
    public void setCompId(String compId) { this.compId = compId; }
    public Long getSandboxTxnId() { return sandboxTxnId; }
    public void setSandboxTxnId(Long sandboxTxnId) { this.sandboxTxnId = sandboxTxnId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getUrlType() { return urlType; }
    public void setUrlType(String urlType) { this.urlType = urlType; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public int getRetryCnt() { return retryCnt; }
    public void setRetryCnt(int retryCnt) { this.retryCnt = retryCnt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getPayloadBody() { return payloadBody; }
    public void setPayloadBody(String payloadBody) { this.payloadBody = payloadBody; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
