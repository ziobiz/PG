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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
