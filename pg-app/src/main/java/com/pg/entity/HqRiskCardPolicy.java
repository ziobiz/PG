package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_hq_risk_card_policy")
public class HqRiskCardPolicy {

    @Id
    private Long id = 1L;

    @Column(name = "enabled_yn", nullable = false, length = 1)
    private String enabledYn = "Y";

    @Column(name = "tier1_hours", nullable = false)
    private Integer tier1Hours = 0;

    @Column(name = "tier1_min", nullable = false)
    private Integer tier1Min = 5;

    @Column(name = "tier2_hours", nullable = false)
    private Integer tier2Hours = 0;

    @Column(name = "tier2_min", nullable = false)
    private Integer tier2Min = 10;

    @Column(name = "tier3_hours", nullable = false)
    private Integer tier3Hours = 1;

    @Column(name = "tier3_min", nullable = false)
    private Integer tier3Min = 0;

    @Column(name = "tier4_hours", nullable = false)
    private Integer tier4Hours = 0;

    @Column(name = "tier4_min", nullable = false)
    private Integer tier4Min = 0;

    @Column(name = "auto_blacklist_trigger_tier", nullable = false)
    private Integer autoBlacklistTriggerTier = 4;

    /** NONE | DAY | MONTH | YEAR */
    @Column(name = "track_period_mode", nullable = false, length = 8)
    private String trackPeriodMode = "NONE";

    @Column(name = "track_period_value", nullable = false)
    private Integer trackPeriodValue = 0;

    @Column(name = "presale_filter_enabled_yn", nullable = false, length = 1)
    private String presaleFilterEnabledYn = "Y";

    @Column(name = "filter_buyer_contact_mismatch_yn", nullable = false, length = 1)
    private String filterBuyerContactMismatchYn = "Y";

    @Column(name = "filter_holder_name_yn", nullable = false, length = 1)
    private String filterHolderNameYn = "Y";

    @Column(name = "filter_velocity_card_yn", nullable = false, length = 1)
    private String filterVelocityCardYn = "Y";

    @Column(name = "filter_velocity_email_yn", nullable = false, length = 1)
    private String filterVelocityEmailYn = "Y";

    @Column(name = "filter_velocity_ip_yn", nullable = false, length = 1)
    private String filterVelocityIpYn = "Y";

    @Column(name = "velocity_window_minutes", nullable = false)
    private Integer velocityWindowMinutes = 10;

    @Column(name = "velocity_max_attempts", nullable = false)
    private Integer velocityMaxAttempts = 3;

    /** 동일 카드 속도제한 창(분) — 기본 10 */
    @Column(name = "velocity_card_window_minutes", nullable = false)
    private Integer velocityCardWindowMinutes = 10;

    @Column(name = "velocity_card_max_attempts", nullable = false)
    private Integer velocityCardMaxAttempts = 3;

    /** 동일 이메일 속도제한 창(분) — 기본 30 */
    @Column(name = "velocity_email_window_minutes", nullable = false)
    private Integer velocityEmailWindowMinutes = 30;

    @Column(name = "velocity_email_max_attempts", nullable = false)
    private Integer velocityEmailMaxAttempts = 5;

    /** 동일 IP 속도제한 창(분) — 기본 15 */
    @Column(name = "velocity_ip_window_minutes", nullable = false)
    private Integer velocityIpWindowMinutes = 15;

    @Column(name = "velocity_ip_max_attempts", nullable = false)
    private Integer velocityIpMaxAttempts = 10;

    @Column(name = "checkout_contact_remember_default_yn", nullable = false, length = 1)
    private String checkoutContactRememberDefaultYn = "Y";

    @Column(name = "filter_phone_invalid_yn", nullable = false, length = 1)
    private String filterPhoneInvalidYn = "Y";

    @Column(name = "filter_email_invalid_yn", nullable = false, length = 1)
    private String filterEmailInvalidYn = "Y";

    @Column(name = "postsale_cooldown_jpay_highrisk_yn", nullable = false, length = 1)
    private String postsaleCooldownJpayHighriskYn = "Y";

    @Column(name = "postsale_cooldown_jpay_py0124_yn", nullable = false, length = 1)
    private String postsaleCooldownJpayPy0124Yn = "Y";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEnabledYn() { return enabledYn; }
    public void setEnabledYn(String enabledYn) { this.enabledYn = enabledYn; }
    public Integer getTier1Hours() { return tier1Hours; }
    public void setTier1Hours(Integer tier1Hours) { this.tier1Hours = tier1Hours; }
    public Integer getTier1Min() { return tier1Min; }
    public void setTier1Min(Integer tier1Min) { this.tier1Min = tier1Min; }
    public Integer getTier2Hours() { return tier2Hours; }
    public void setTier2Hours(Integer tier2Hours) { this.tier2Hours = tier2Hours; }
    public Integer getTier2Min() { return tier2Min; }
    public void setTier2Min(Integer tier2Min) { this.tier2Min = tier2Min; }
    public Integer getTier3Hours() { return tier3Hours; }
    public void setTier3Hours(Integer tier3Hours) { this.tier3Hours = tier3Hours; }
    public Integer getTier3Min() { return tier3Min; }
    public void setTier3Min(Integer tier3Min) { this.tier3Min = tier3Min; }
    public Integer getTier4Hours() { return tier4Hours; }
    public void setTier4Hours(Integer tier4Hours) { this.tier4Hours = tier4Hours; }
    public Integer getTier4Min() { return tier4Min; }
    public void setTier4Min(Integer tier4Min) { this.tier4Min = tier4Min; }
    public Integer getAutoBlacklistTriggerTier() { return autoBlacklistTriggerTier; }
    public void setAutoBlacklistTriggerTier(Integer autoBlacklistTriggerTier) {
        this.autoBlacklistTriggerTier = autoBlacklistTriggerTier;
    }
    public String getTrackPeriodMode() { return trackPeriodMode; }
    public void setTrackPeriodMode(String trackPeriodMode) { this.trackPeriodMode = trackPeriodMode; }
    public Integer getTrackPeriodValue() { return trackPeriodValue; }
    public void setTrackPeriodValue(Integer trackPeriodValue) { this.trackPeriodValue = trackPeriodValue; }
    public String getPresaleFilterEnabledYn() { return presaleFilterEnabledYn; }
    public void setPresaleFilterEnabledYn(String presaleFilterEnabledYn) {
        this.presaleFilterEnabledYn = presaleFilterEnabledYn;
    }
    public String getFilterBuyerContactMismatchYn() { return filterBuyerContactMismatchYn; }
    public void setFilterBuyerContactMismatchYn(String v) { this.filterBuyerContactMismatchYn = v; }
    public String getFilterHolderNameYn() { return filterHolderNameYn; }
    public void setFilterHolderNameYn(String v) { this.filterHolderNameYn = v; }
    public String getFilterVelocityCardYn() { return filterVelocityCardYn; }
    public void setFilterVelocityCardYn(String v) { this.filterVelocityCardYn = v; }
    public String getFilterVelocityEmailYn() { return filterVelocityEmailYn; }
    public void setFilterVelocityEmailYn(String v) { this.filterVelocityEmailYn = v; }
    public String getFilterVelocityIpYn() { return filterVelocityIpYn; }
    public void setFilterVelocityIpYn(String v) { this.filterVelocityIpYn = v; }
    public Integer getVelocityWindowMinutes() { return velocityWindowMinutes; }
    public void setVelocityWindowMinutes(Integer velocityWindowMinutes) { this.velocityWindowMinutes = velocityWindowMinutes; }
    public Integer getVelocityMaxAttempts() { return velocityMaxAttempts; }
    public void setVelocityMaxAttempts(Integer velocityMaxAttempts) { this.velocityMaxAttempts = velocityMaxAttempts; }
    public Integer getVelocityCardWindowMinutes() { return velocityCardWindowMinutes; }
    public void setVelocityCardWindowMinutes(Integer velocityCardWindowMinutes) { this.velocityCardWindowMinutes = velocityCardWindowMinutes; }
    public Integer getVelocityCardMaxAttempts() { return velocityCardMaxAttempts; }
    public void setVelocityCardMaxAttempts(Integer velocityCardMaxAttempts) { this.velocityCardMaxAttempts = velocityCardMaxAttempts; }
    public Integer getVelocityEmailWindowMinutes() { return velocityEmailWindowMinutes; }
    public void setVelocityEmailWindowMinutes(Integer velocityEmailWindowMinutes) { this.velocityEmailWindowMinutes = velocityEmailWindowMinutes; }
    public Integer getVelocityEmailMaxAttempts() { return velocityEmailMaxAttempts; }
    public void setVelocityEmailMaxAttempts(Integer velocityEmailMaxAttempts) { this.velocityEmailMaxAttempts = velocityEmailMaxAttempts; }
    public Integer getVelocityIpWindowMinutes() { return velocityIpWindowMinutes; }
    public void setVelocityIpWindowMinutes(Integer velocityIpWindowMinutes) { this.velocityIpWindowMinutes = velocityIpWindowMinutes; }
    public Integer getVelocityIpMaxAttempts() { return velocityIpMaxAttempts; }
    public void setVelocityIpMaxAttempts(Integer velocityIpMaxAttempts) { this.velocityIpMaxAttempts = velocityIpMaxAttempts; }
    public String getCheckoutContactRememberDefaultYn() { return checkoutContactRememberDefaultYn; }
    public void setCheckoutContactRememberDefaultYn(String checkoutContactRememberDefaultYn) {
        this.checkoutContactRememberDefaultYn = checkoutContactRememberDefaultYn;
    }
    public String getFilterPhoneInvalidYn() { return filterPhoneInvalidYn; }
    public void setFilterPhoneInvalidYn(String filterPhoneInvalidYn) { this.filterPhoneInvalidYn = filterPhoneInvalidYn; }
    public String getFilterEmailInvalidYn() { return filterEmailInvalidYn; }
    public void setFilterEmailInvalidYn(String filterEmailInvalidYn) { this.filterEmailInvalidYn = filterEmailInvalidYn; }
    public String getPostsaleCooldownJpayHighriskYn() { return postsaleCooldownJpayHighriskYn; }
    public void setPostsaleCooldownJpayHighriskYn(String v) { this.postsaleCooldownJpayHighriskYn = v; }
    public String getPostsaleCooldownJpayPy0124Yn() { return postsaleCooldownJpayPy0124Yn; }
    public void setPostsaleCooldownJpayPy0124Yn(String v) { this.postsaleCooldownJpayPy0124Yn = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
