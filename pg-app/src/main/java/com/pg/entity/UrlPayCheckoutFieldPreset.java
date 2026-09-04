package com.pg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 결제창 구매자 입력 필드 프리셋 — 기본형·1형·2형…
 */
@Entity
@Table(name = "tb_url_pay_checkout_field_preset",
        uniqueConstraints = @UniqueConstraint(name = "uq_url_pay_checkout_field_preset_name", columnNames = "preset_name"))
public class UrlPayCheckoutFieldPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "preset_name", nullable = false, length = 40)
    private String presetName;

    @Column(name = "sort_no", nullable = false)
    private int sortNo = 0;

    @Column(name = "is_default_yn", nullable = false, length = 1)
    private String isDefaultYn = "N";

    @Column(name = "buyer_email_use_yn", nullable = false, length = 1)
    private String buyerEmailUseYn = "Y";

    @Column(name = "buyer_country_use_yn", nullable = false, length = 1)
    private String buyerCountryUseYn = "Y";

    @Column(name = "buyer_phone_use_yn", nullable = false, length = 1)
    private String buyerPhoneUseYn = "Y";

    @Column(name = "shipping_address_use_yn", nullable = false, length = 1)
    private String shippingAddressUseYn = "N";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPresetName() { return presetName; }
    public void setPresetName(String presetName) { this.presetName = presetName; }

    public int getSortNo() { return sortNo; }
    public void setSortNo(int sortNo) { this.sortNo = sortNo; }

    public String getIsDefaultYn() { return isDefaultYn; }
    public void setIsDefaultYn(String isDefaultYn) {
        this.isDefaultYn = "Y".equalsIgnoreCase(isDefaultYn != null ? isDefaultYn.trim() : "") ? "Y" : "N";
    }

    public boolean isDefault() {
        return "Y".equalsIgnoreCase(isDefaultYn);
    }

    public String getBuyerEmailUseYn() { return buyerEmailUseYn; }
    public void setBuyerEmailUseYn(String buyerEmailUseYn) {
        this.buyerEmailUseYn = yn(buyerEmailUseYn, "Y");
    }

    public String getBuyerCountryUseYn() { return buyerCountryUseYn; }
    public void setBuyerCountryUseYn(String buyerCountryUseYn) {
        this.buyerCountryUseYn = yn(buyerCountryUseYn, "Y");
    }

    public String getBuyerPhoneUseYn() { return buyerPhoneUseYn; }
    public void setBuyerPhoneUseYn(String buyerPhoneUseYn) {
        this.buyerPhoneUseYn = yn(buyerPhoneUseYn, "Y");
    }

    public String getShippingAddressUseYn() { return shippingAddressUseYn; }
    public void setShippingAddressUseYn(String shippingAddressUseYn) {
        this.shippingAddressUseYn = yn(shippingAddressUseYn, "N");
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    private static String yn(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return "Y".equalsIgnoreCase(fallback) ? "Y" : "N";
        }
        return "Y".equalsIgnoreCase(raw.trim()) ? "Y" : "N";
    }
}
