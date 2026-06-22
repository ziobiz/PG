package com.pg.repository;

import com.pg.entity.SplitPayInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SplitPayInstallmentRepository extends JpaRepository<SplitPayInstallment, Long>,
        JpaSpecificationExecutor<SplitPayInstallment> {

    List<SplitPayInstallment> findByContractIdOrderByInstallmentNoAsc(Long contractId);

    Optional<SplitPayInstallment> findByPayToken(String payToken);

    Optional<SplitPayInstallment> findByOrderNo(String orderNo);

    List<SplitPayInstallment> findByOrderNoIn(Collection<String> orderNos);

    Optional<SplitPayInstallment> findByPgTrnId(String pgTrnId);

    List<SplitPayInstallment> findByPgTrnIdIn(Collection<String> pgTrnIds);

    @Query("""
            SELECT i FROM SplitPayInstallment i
            WHERE i.status = 'PENDING'
              AND i.dueDateAdjusted BETWEEN :from AND :to
            """)
    List<SplitPayInstallment> findPendingDueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
