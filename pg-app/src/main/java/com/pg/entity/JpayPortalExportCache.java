package com.pg.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_jpay_portal_export_cache")
public class JpayPortalExportCache {

    public static final String DEFAULT_KEY = "DEFAULT";

    @Id
    @Column(name = "cache_key", length = 32)
    private String cacheKey = DEFAULT_KEY;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "last_sync_message", columnDefinition = "TEXT")
    private String lastSyncMessage;

    @Column(name = "export_from")
    private LocalDate exportFrom;

    @Column(name = "export_to")
    private LocalDate exportTo;

    @Column(name = "rows_json", nullable = false, columnDefinition = "TEXT")
    private String rowsJson = "[]";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 당일 동기화 횟수 집계 기준일(전산 타임존) */
    @Column(name = "sync_count_date")
    private LocalDate syncCountDate;

    @Column(name = "sync_count_today", nullable = false)
    private int syncCountToday;

    @PrePersist
    @PreUpdate
    protected void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }

    public String getLastSyncMessage() { return lastSyncMessage; }
    public void setLastSyncMessage(String lastSyncMessage) { this.lastSyncMessage = lastSyncMessage; }

    public LocalDate getExportFrom() { return exportFrom; }
    public void setExportFrom(LocalDate exportFrom) { this.exportFrom = exportFrom; }

    public LocalDate getExportTo() { return exportTo; }
    public void setExportTo(LocalDate exportTo) { this.exportTo = exportTo; }

    public String getRowsJson() { return rowsJson; }
    public void setRowsJson(String rowsJson) { this.rowsJson = rowsJson; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDate getSyncCountDate() { return syncCountDate; }
    public void setSyncCountDate(LocalDate syncCountDate) { this.syncCountDate = syncCountDate; }

    public int getSyncCountToday() { return syncCountToday; }
    public void setSyncCountToday(int syncCountToday) { this.syncCountToday = syncCountToday; }
}
