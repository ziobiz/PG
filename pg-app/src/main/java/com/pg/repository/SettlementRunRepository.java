package com.pg.repository;

import com.pg.entity.SettlementRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRunRepository extends JpaRepository<SettlementRun, Long> {

    List<SettlementRun> findByCalcDtBetweenOrderByMerchantId(LocalDate from, LocalDate to);

    List<SettlementRun> findByCalcDtAndMerchantId(LocalDate calcDt, String merchantId);

    /** 해당 가맹·기간(포함) 내 정산 실행 건수 — 월간 고정 이용료 1회 부과 여부 판단용 */
    long countByMerchantIdAndCalcDtBetween(String merchantId, LocalDate calcDtStart, LocalDate calcDtEnd);
}
