package com.pg.repository;

import com.pg.entity.SettlementCalcCycleAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementCalcCycleAuditRepository extends JpaRepository<SettlementCalcCycleAudit, Long> {

    List<SettlementCalcCycleAudit> findByMerchantCodeOrderByCreatedAtDesc(String merchantCode, Pageable pageable);

    List<SettlementCalcCycleAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
