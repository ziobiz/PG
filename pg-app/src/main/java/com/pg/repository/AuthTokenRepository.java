package com.pg.repository;

import com.pg.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {

    Optional<AuthToken> findByTokenAndExpiresAtAfter(String token, Instant now);

    void deleteByUserId(Long userId);

    void deleteByExpiresAtBefore(Instant instant);
}
