package com.pg.repository;

import com.pg.entity.SettlementSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementSettingRepository extends JpaRepository<SettlementSetting, Long> {
    Optional<SettlementSetting> findByOrgUnitId(Long orgUnitId);

    List<SettlementSetting> findByCalcCycle(String calcCycle);
}
