package com.pg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "tb_server_usage_daily")
public class ServerUsageDaily {

    @Id
    @Column(name = "usage_date")
    private LocalDate usageDate;

    @Column(name = "traffic_bytes", nullable = false)
    private long trafficBytes;

    @Column(name = "memory_peak_pct", nullable = false)
    private double memoryPeakPct;

    public ServerUsageDaily() {
    }

    public ServerUsageDaily(LocalDate usageDate, long trafficBytes, double memoryPeakPct) {
        this.usageDate = usageDate;
        this.trafficBytes = trafficBytes;
        this.memoryPeakPct = memoryPeakPct;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    public long getTrafficBytes() {
        return trafficBytes;
    }

    public void setTrafficBytes(long trafficBytes) {
        this.trafficBytes = trafficBytes;
    }

    public double getMemoryPeakPct() {
        return memoryPeakPct;
    }

    public void setMemoryPeakPct(double memoryPeakPct) {
        this.memoryPeakPct = memoryPeakPct;
    }
}
