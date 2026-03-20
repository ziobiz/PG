package com.pg.repository;

import com.pg.entity.DistributionFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistributionFeeConfigRepository extends JpaRepository<DistributionFeeConfig, Long> {
    Optional<DistributionFeeConfig> findByCompId(String compId);
}

