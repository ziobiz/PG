package com.pg.repository;

import com.pg.entity.ChatbotAdminSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ChatbotAdminSessionRepository extends JpaRepository<ChatbotAdminSession, Long> {

    Optional<ChatbotAdminSession> findByToken(String token);

    @Modifying
    @Query("delete from ChatbotAdminSession s where s.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from ChatbotAdminSession s where s.expiresAt < :now")
    int deleteExpiredBefore(@Param("now") Instant now);
}
