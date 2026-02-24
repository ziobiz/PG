package com.pg.repository;

import com.pg.entity.SettlementRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRunRepository extends JpaRepository<SettlementRun, Long> {

    List<SettlementRun> findByCalcDtBetweenOrderByMerchantId(LocalDate from, LocalDate to);

    List<SettlementRun> findByCalcDtAndMerchantId(LocalDate calcDt, String merchantId);
}
