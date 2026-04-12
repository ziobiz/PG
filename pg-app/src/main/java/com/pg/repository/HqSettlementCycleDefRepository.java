package com.pg.repository;

import com.pg.entity.HqSettlementCycleDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HqSettlementCycleDefRepository extends JpaRepository<HqSettlementCycleDef, Long> {
    Optional<HqSettlementCycleDef> findByCycleCodeIgnoreCase(String cycleCode);
}
