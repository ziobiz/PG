package com.pg.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 거래 마스터 (PG_TRNSCTN 스타일) - 목록/조회용 핵심 컬럼
 */
@Entity
@Table(name = "pg_trnsctn")
public class PgTrnsctn {

    @Id
    @Column(name = "trn_id", length = 20)
    private String trnId;

    @Column(name = "merchant_id", nullable = false, length = 20)
    private String merchantId;

    @Column(name = "service_type", length = 20)
    private String serviceType;

    @Column(name = "status", length = 2)
    private String status;

    @Column(name = "cur_type", length = 3)
    private String curType = "KRW";

    /** 거래 금액(노티·동기화 원문 소수 유지 — USD 등). 컬럼명 amt_krw 는 이력 호환. */
    @Column(name = "amt_krw", precision = 20, scale = 8)
    private BigDecimal amtKrw;

    @Column(name = "pay_no", length = 50)
    private String payNo;

    @Column(name = "approval_no", length = 20)
    private String approvalNo;

    @Column(name = "van", length = 10)
    private String van;

    /** null·CHILL(동기화), NOTI(전산노티), URL(칠페이 URL 결제), CHATBOT(EFO 웹챗봇·동일 칠페이 API) */
    @Column(name = "origin", length = 20)
    private String origin;

    /** 전산 노티 적재 시 수신 경로. CALLBACK | RESULT | BOTH(양 채널 수신). 기존 행·비노티 출처는 null */
    @Column(name = "notify_channel_type", length = 20)
    private String notifyChannelType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** ChillPay OrderNo */
    @Column(name = "order_no", length = 64)
    private String orderNo;

    /** ChillPay Customer (식별자) */
    @Column(name = "customer_id", length = 100)
    private String customerId;

    /** ChillPay Customer 표시명 */
    @Column(name = "customer_nm", length = 200)
    private String customerNm;

    /** JPAY·URL 결제 구매자 전화(로컬 번호). ChillPay 노티에는 없음. */
    @Column(name = "customer_tel", length = 50)
    private String customerTel;

    /** JPAY 등 — 마스킹 카드번호(414520***8306). 평문 PAN 저장 금지. */
    @Column(name = "card_pan_display", length = 32)
    private String cardPanDisplay;

    @Column(name = "card_pan_hash", length = 64)
    private String cardPanHash;

    /** ChillPay Payment Channel (channelCode 등) */
    @Column(name = "payment_channel", length = 80)
    private String paymentChannel;

    /** ChillPay 결제 완료 시각 (거래 적재 시각과 별도일 수 있음) */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** ChillPay/정산 시트 ICOPAY 금액 */
    @Column(name = "icopay_amt", precision = 20, scale = 8)
    private BigDecimal icopayAmt;

    /** ChillPay Fee (PG/대행 수수료 등 원문 금액) */
    @Column(name = "chill_fee_amt", precision = 20, scale = 8)
    private BigDecimal chillFeeAmt;

    /** ChillPay TotalAmount */
    @Column(name = "total_amt", precision = 20, scale = 8)
    private BigDecimal totalAmt;

    /** ChillPay RouteNo */
    @Column(name = "route_no", length = 32)
    private String routeNo;

    /** ChillPay 결제 상태 원문 (Paid, WaitAuthorize 등) */
    @Column(name = "chill_payment_status", length = 50)
    private String chillPaymentStatus;

    /** 정산 완료 여부 Y/N */
    @Column(name = "settled_yn", length = 1)
    private String settledYn = "N";

    /**
     * 칠페이(ChillPay) TransactionId — PG가 부여한 {@link #trnId}와 별도의 고유값.
     * API 응답·노티의 transactionId 등.
     */
    @Column(name = "chill_transaction_id", length = 64)
    private String chillTransactionId;

    /** PG중계→가맹점 콜백 마지막 성공 전송 시 내부 {@link #status} 스냅샷 (V83). */
    @Column(name = "mw_outbound_last_sent_status", length = 8)
    private String mwOutboundLastSentStatus;

    /**
     * 고객 화면(URL·챗봇·API 결제)에 보였던 금액·통화.
     * {@link #curType}/{@link #amtKrw} 는 PG 청구(실승인) 기준이며, DISPLAY_FX 등으로 둘이 다를 수 있습니다.
     */
    @Column(name = "display_cur_type", length = 10)
    private String displayCurType;

    @Column(name = "display_amt", precision = 20, scale = 8)
    private BigDecimal displayAmt;

    /** 실패·취소·무효·환불 등 상태 변경 처리사유(V185) */
    @Column(name = "outcome_reason", columnDefinition = "TEXT")
    private String outcomeReason;

    @Column(name = "outcome_reason_code", length = 64)
    private String outcomeReasonCode;

    @Column(name = "outcome_reason_source", length = 32)
    private String outcomeReasonSource;

    @Column(name = "outcome_reason_at")
    private LocalDateTime outcomeReasonAt;

    /** 결제 고객 IP (V203) */
    @Column(name = "payer_client_ip", length = 64)
    private String payerClientIp;

    /** PC / MOBILE_IOS / MOBILE_ANDROID 등 (V203) */
    @Column(name = "payer_device_category", length = 32)
    private String payerDeviceCategory;

    /** 결제 접속·청구 국가 ISO2 (V203) — PostgreSQL bpchar(2) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payer_country_iso2", length = 2)
    private String payerCountryIso2;

    /** 결제 고객 도시 (V206) — JPAY payCity·CF-IPCity 등 */
    @Column(name = "payer_city", length = 128)
    private String payerCity;

    /** 결제개요 위치 — JPAY 스타일 영문 (예: Japan-Chiba Prefecture, V208) */
    @Column(name = "payer_location_label", length = 256)
    private String payerLocationLabel;

    /** 결제창 언어 KOR/ENG/JPN/CHN/THA */
    @Column(name = "checkout_lang", length = 8)
    private String checkoutLang;

    /** 고객 거래명세서 이메일 발송 시각 */
    @Column(name = "receipt_mail_sent_at")
    private LocalDateTime receiptMailSentAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Bangkok"));
        }
    }

    public String getTrnId() { return trnId; }
    public void setTrnId(String trnId) { this.trnId = trnId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurType() { return curType; }
    public void setCurType(String curType) { this.curType = curType; }
    public BigDecimal getAmtKrw() { return amtKrw; }
    public void setAmtKrw(BigDecimal amtKrw) { this.amtKrw = amtKrw; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public String getApprovalNo() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }
    public String getVan() { return van; }
    public void setVan(String van) { this.van = van; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getNotifyChannelType() { return notifyChannelType; }
    public void setNotifyChannelType(String notifyChannelType) { this.notifyChannelType = notifyChannelType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerNm() { return customerNm; }
    public void setCustomerNm(String customerNm) { this.customerNm = customerNm; }
    public String getCustomerTel() { return customerTel; }
    public void setCustomerTel(String customerTel) { this.customerTel = customerTel; }
    public String getCardPanDisplay() { return cardPanDisplay; }
    public void setCardPanDisplay(String cardPanDisplay) { this.cardPanDisplay = cardPanDisplay; }
    public String getCardPanHash() { return cardPanHash; }
    public void setCardPanHash(String cardPanHash) { this.cardPanHash = cardPanHash; }
    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public BigDecimal getIcopayAmt() { return icopayAmt; }
    public void setIcopayAmt(BigDecimal icopayAmt) { this.icopayAmt = icopayAmt; }
    public BigDecimal getChillFeeAmt() { return chillFeeAmt; }
    public void setChillFeeAmt(BigDecimal chillFeeAmt) { this.chillFeeAmt = chillFeeAmt; }
    public BigDecimal getTotalAmt() { return totalAmt; }
    public void setTotalAmt(BigDecimal totalAmt) { this.totalAmt = totalAmt; }
    public String getRouteNo() { return routeNo; }
    public void setRouteNo(String routeNo) { this.routeNo = routeNo; }
    public String getChillPaymentStatus() { return chillPaymentStatus; }
    public void setChillPaymentStatus(String chillPaymentStatus) { this.chillPaymentStatus = chillPaymentStatus; }
    public String getSettledYn() { return settledYn; }
    public void setSettledYn(String settledYn) { this.settledYn = settledYn; }
    public String getChillTransactionId() { return chillTransactionId; }
    public void setChillTransactionId(String chillTransactionId) { this.chillTransactionId = chillTransactionId; }
    public String getMwOutboundLastSentStatus() { return mwOutboundLastSentStatus; }
    public void setMwOutboundLastSentStatus(String mwOutboundLastSentStatus) { this.mwOutboundLastSentStatus = mwOutboundLastSentStatus; }

    public String getDisplayCurType() { return displayCurType; }
    public void setDisplayCurType(String displayCurType) { this.displayCurType = displayCurType; }
    public BigDecimal getDisplayAmt() { return displayAmt; }
    public void setDisplayAmt(BigDecimal displayAmt) { this.displayAmt = displayAmt; }

    public String getOutcomeReason() { return outcomeReason; }
    public void setOutcomeReason(String outcomeReason) { this.outcomeReason = outcomeReason; }
    public String getOutcomeReasonCode() { return outcomeReasonCode; }
    public void setOutcomeReasonCode(String outcomeReasonCode) { this.outcomeReasonCode = outcomeReasonCode; }
    public String getOutcomeReasonSource() { return outcomeReasonSource; }
    public void setOutcomeReasonSource(String outcomeReasonSource) { this.outcomeReasonSource = outcomeReasonSource; }
    public LocalDateTime getOutcomeReasonAt() { return outcomeReasonAt; }
    public void setOutcomeReasonAt(LocalDateTime outcomeReasonAt) { this.outcomeReasonAt = outcomeReasonAt; }

    public String getPayerClientIp() { return payerClientIp; }
    public void setPayerClientIp(String payerClientIp) { this.payerClientIp = payerClientIp; }
    public String getPayerDeviceCategory() { return payerDeviceCategory; }
    public void setPayerDeviceCategory(String payerDeviceCategory) { this.payerDeviceCategory = payerDeviceCategory; }
    public String getPayerCountryIso2() { return payerCountryIso2; }
    public void setPayerCountryIso2(String payerCountryIso2) { this.payerCountryIso2 = payerCountryIso2; }
    public String getPayerCity() { return payerCity; }
    public void setPayerCity(String payerCity) { this.payerCity = payerCity; }
    public String getPayerLocationLabel() { return payerLocationLabel; }
    public void setPayerLocationLabel(String payerLocationLabel) { this.payerLocationLabel = payerLocationLabel; }
    public String getCheckoutLang() { return checkoutLang; }
    public void setCheckoutLang(String checkoutLang) { this.checkoutLang = checkoutLang; }
    public LocalDateTime getReceiptMailSentAt() { return receiptMailSentAt; }
    public void setReceiptMailSentAt(LocalDateTime receiptMailSentAt) { this.receiptMailSentAt = receiptMailSentAt; }
}
