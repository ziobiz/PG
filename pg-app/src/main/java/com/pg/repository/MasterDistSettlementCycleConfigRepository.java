package com.pg.repository;

import com.pg.entity.MasterDistSettlementCycleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterDistSettlementCycleConfigRepository extends JpaRepository<MasterDistSettlementCycleConfig, Long> {

    Optional<MasterDistSettlementCycleConfig> findByOrgUnitId(Long orgUnitId);
}
