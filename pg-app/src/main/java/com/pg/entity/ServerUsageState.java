package com.pg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tb_server_usage_state")
public class ServerUsageState {

    public static final short SINGLETON_ID = 1;

    @Id
    @Column(name = "id")
    private short id = SINGLETON_ID;

    @Column(name = "last_net_total_bytes")
    private Long lastNetTotalBytes;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public Long getLastNetTotalBytes() {
        return lastNetTotalBytes;
    }

    public void setLastNetTotalBytes(Long lastNetTotalBytes) {
        this.lastNetTotalBytes = lastNetTotalBytes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
