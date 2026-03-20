package com.pg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 본사설정 계정관리: 사용자(로그인ID)가 접근 가능한 업체코드(본사·총판·가맹 등) 허용 목록
 */
@Entity
@Table(name = "tb_user_comp_access", uniqueConstraints = @UniqueConstraint(columnNames = {"username", "comp_code"}))
public class UserCompAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "comp_code", nullable = false, length = 32)
    private String compCode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCompCode() { return compCode; }
    public void setCompCode(String compCode) { this.compCode = compCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
