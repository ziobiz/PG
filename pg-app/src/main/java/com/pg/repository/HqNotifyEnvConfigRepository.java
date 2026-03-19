package com.pg.repository;

import com.pg.entity.HqNotifyEnvConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HqNotifyEnvConfigRepository extends JpaRepository<HqNotifyEnvConfig, Long> {
    Optional<HqNotifyEnvConfig> findFirstByOrderByIdAsc();
}
