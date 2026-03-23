package com.pg.repository;

import com.pg.entity.HqNotifyMappingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HqNotifyMappingConfigRepository extends JpaRepository<HqNotifyMappingConfig, Long> {
    Optional<HqNotifyMappingConfig> findFirstByOrderByIdAsc();
}
