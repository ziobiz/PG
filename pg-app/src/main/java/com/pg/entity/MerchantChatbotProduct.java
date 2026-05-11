package com.pg.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_merchant_chatbot_product")
public class MerchantChatbotProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "product_code", length = 64)
    private String productCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "KRW";

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "image_url_2", length = 512)
    private String imageUrl2;

    @Column(name = "image_url_3", length = 512)
    private String imageUrl3;

    @Column(name = "image_url_4", length = 512)
    private String imageUrl4;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    /** Y이면 본사·총판이 고객 챗봇/공개 카탈로그에서 판매·노출 불가로 지정 (가맹 사용 여부와 별개) */
    @Column(name = "hq_catalog_block_yn", nullable = false, length = 1)
    private String hqCatalogBlockYn = "N";

    /** SALE=일반판매, RESERVATION=예약 */
    @Column(name = "listing_type", nullable = false, length = 16)
    private String listingType = "SALE";

    /** 예약 시 슬롯 길이(분) 오버라이드. null 이면 가맹 기본설정(프로필) 사용 */
    @Column(name = "reservation_slot_minutes")
    private Integer reservationSlotMinutes;

    /** FULL=전액 즉시 결제, DEPOSIT=예약금(부분) */
    @Column(name = "reservation_collect_mode", nullable = false, length = 16)
    private String reservationCollectMode = "FULL";

    @Column(name = "deposit_amount", precision = 18, scale = 4)
    private BigDecimal depositAmount;

    /** Y이면 고객 챗봇-pay 상단 「프로모션」 영역에만 노출(등록·채팅용 전체 목록과 별개 표시) */
    @Column(name = "promotion_shelf_yn", nullable = false, length = 1)
    private String promotionShelfYn = "N";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl2() {
        return imageUrl2;
    }

    public void setImageUrl2(String imageUrl2) {
        this.imageUrl2 = imageUrl2;
    }

    public String getImageUrl3() {
        return imageUrl3;
    }

    public void setImageUrl3(String imageUrl3) {
        this.imageUrl3 = imageUrl3;
    }

    public String getImageUrl4() {
        return imageUrl4;
    }

    public void setImageUrl4(String imageUrl4) {
        this.imageUrl4 = imageUrl4;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public String getHqCatalogBlockYn() {
        return hqCatalogBlockYn;
    }

    public void setHqCatalogBlockYn(String hqCatalogBlockYn) {
        this.hqCatalogBlockYn = hqCatalogBlockYn;
    }

    public String getListingType() {
        return listingType;
    }

    public void setListingType(String listingType) {
        this.listingType = listingType;
    }

    public Integer getReservationSlotMinutes() {
        return reservationSlotMinutes;
    }

    public void setReservationSlotMinutes(Integer reservationSlotMinutes) {
        this.reservationSlotMinutes = reservationSlotMinutes;
    }

    public String getReservationCollectMode() {
        return reservationCollectMode;
    }

    public void setReservationCollectMode(String reservationCollectMode) {
        this.reservationCollectMode = reservationCollectMode;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getPromotionShelfYn() {
        return promotionShelfYn;
    }

    public void setPromotionShelfYn(String promotionShelfYn) {
        this.promotionShelfYn = promotionShelfYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
