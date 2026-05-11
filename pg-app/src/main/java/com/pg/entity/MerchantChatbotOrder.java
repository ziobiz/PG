package com.pg.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 챗봇 고객 주문(주문자 정보·상품·예약 구간). 결제 전 {@code PENDING_PAYMENT},
 * ChillPay 승인(거래 적재) 후 {@code CONFIRMED} 및 {@link #pgTrnId} 연결.
 */
@Entity
@Table(name = "tb_merchant_chatbot_order")
public class MerchantChatbotOrder {

    public static final String STATUS_PENDING = "PENDING_PAYMENT";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_title", nullable = false, length = 200)
    private String productTitle;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "KRW";

    @Column(name = "listing_type_snapshot", nullable = false, length = 16)
    private String listingTypeSnapshot = "SALE";

    @Column(name = "reservation_collect_snapshot", nullable = false, length = 16)
    private String reservationCollectSnapshot = "FULL";

    @Column(name = "product_line_total_amount", precision = 18, scale = 4)
    private BigDecimal productLineTotalAmount;

    @Column(name = "balance_due_amount", precision = 18, scale = 4)
    private BigDecimal balanceDueAmount;

    @Column(name = "orderer_name", length = 100)
    private String ordererName;

    @Column(name = "orderer_email", length = 120)
    private String ordererEmail;

    @Column(name = "orderer_phone", length = 50)
    private String ordererPhone;

    @Column(name = "orderer_addr", length = 600)
    private String ordererAddr;

    @Column(name = "reservation_start")
    private Instant reservationStart;

    @Column(name = "reservation_end")
    private Instant reservationEnd;

    @Column(nullable = false, length = 24)
    private String status = STATUS_PENDING;

    @Column(name = "checkout_order_no", nullable = false, length = 20, unique = true)
    private String checkoutOrderNo;

    @Column(name = "pg_trn_id", length = 20)
    private String pgTrnId;

    @Column(name = "order_memo", columnDefinition = "TEXT")
    private String orderMemo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant n = Instant.now();
        if (createdAt == null) {
            createdAt = n;
        }
        updatedAt = n;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getListingTypeSnapshot() {
        return listingTypeSnapshot;
    }

    public void setListingTypeSnapshot(String listingTypeSnapshot) {
        this.listingTypeSnapshot = listingTypeSnapshot;
    }

    public String getReservationCollectSnapshot() {
        return reservationCollectSnapshot;
    }

    public void setReservationCollectSnapshot(String reservationCollectSnapshot) {
        this.reservationCollectSnapshot = reservationCollectSnapshot;
    }

    public BigDecimal getProductLineTotalAmount() {
        return productLineTotalAmount;
    }

    public void setProductLineTotalAmount(BigDecimal productLineTotalAmount) {
        this.productLineTotalAmount = productLineTotalAmount;
    }

    public BigDecimal getBalanceDueAmount() {
        return balanceDueAmount;
    }

    public void setBalanceDueAmount(BigDecimal balanceDueAmount) {
        this.balanceDueAmount = balanceDueAmount;
    }

    public String getOrdererName() {
        return ordererName;
    }

    public void setOrdererName(String ordererName) {
        this.ordererName = ordererName;
    }

    public String getOrdererEmail() {
        return ordererEmail;
    }

    public void setOrdererEmail(String ordererEmail) {
        this.ordererEmail = ordererEmail;
    }

    public String getOrdererPhone() {
        return ordererPhone;
    }

    public void setOrdererPhone(String ordererPhone) {
        this.ordererPhone = ordererPhone;
    }

    public String getOrdererAddr() {
        return ordererAddr;
    }

    public void setOrdererAddr(String ordererAddr) {
        this.ordererAddr = ordererAddr;
    }

    public Instant getReservationStart() {
        return reservationStart;
    }

    public void setReservationStart(Instant reservationStart) {
        this.reservationStart = reservationStart;
    }

    public Instant getReservationEnd() {
        return reservationEnd;
    }

    public void setReservationEnd(Instant reservationEnd) {
        this.reservationEnd = reservationEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCheckoutOrderNo() {
        return checkoutOrderNo;
    }

    public void setCheckoutOrderNo(String checkoutOrderNo) {
        this.checkoutOrderNo = checkoutOrderNo;
    }

    public String getPgTrnId() {
        return pgTrnId;
    }

    public void setPgTrnId(String pgTrnId) {
        this.pgTrnId = pgTrnId;
    }

    public String getOrderMemo() {
        return orderMemo;
    }

    public void setOrderMemo(String orderMemo) {
        this.orderMemo = orderMemo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
