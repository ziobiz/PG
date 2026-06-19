package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 외부(칠페이·NOTI 등)에서 수신한 PG 노티 원문 및 가맹점 매핑 결과
 */
@Entity
@Table(name = "tb_pg_notify_inbound")
public class PgNotifyInbound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mid", length = 80)
    private String mid;

    @Column(name = "root_no", length = 40)
    private String rootNo;

    /** 노티 본문에서 파싱한 업체코드(URL 결제 1:N 분기). MID+루트만 쓰는 노티 연동에서는 비움 */
    @Column(name = "payload_comp_id", length = 64)
    private String payloadCompId;

    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    /** PostgreSQL: TEXT (V11 마이그레이션). @Lob+CLOB 는 PG에서 스키마 검증 불일치 남 */
    @Column(name = "raw_body", columnDefinition = "TEXT")
    private String rawBody;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    /** 노티 URL 경로의 대상코드(cb…/rs… 등). 없으면 null */
    @Column(name = "notify_target_code", length = 64)
    private String notifyTargetCode;

    /** 본사설정 노티대상의 채널 유형(CALLBACK/RESULT 등). 미등록·레거시는 null */
    @Column(name = "notify_channel_type", length = 20)
    private String notifyChannelType;

    /** LIVE=실시간 최초, RETRY=재전송, UNKNOWN=헤더 없음(V83) */
    @Column(name = "ingress_delivery_kind", length = 16)
    private String ingressDeliveryKind;

    @Column(name = "process_status", length = 32)
    private String processStatus = "RECEIVED";

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMid() { return mid; }
    public void setMid(String mid) { this.mid = mid; }
    public String getRootNo() { return rootNo; }
    public void setRootNo(String rootNo) { this.rootNo = rootNo; }
    public String getPayloadCompId() { return payloadCompId; }
    public void setPayloadCompId(String payloadCompId) { this.payloadCompId = payloadCompId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getRawBody() { return rawBody; }
    public void setRawBody(String rawBody) { this.rawBody = rawBody; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getNotifyTargetCode() { return notifyTargetCode; }
    public void setNotifyTargetCode(String notifyTargetCode) { this.notifyTargetCode = notifyTargetCode; }
    public String getNotifyChannelType() { return notifyChannelType; }
    public void setNotifyChannelType(String notifyChannelType) { this.notifyChannelType = notifyChannelType; }
    public String getIngressDeliveryKind() { return ingressDeliveryKind; }
    public void setIngressDeliveryKind(String ingressDeliveryKind) { this.ingressDeliveryKind = ingressDeliveryKind; }
    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
