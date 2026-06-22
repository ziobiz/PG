package com.pg.repository;

import com.pg.entity.SplitPayInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SplitPayInstallmentRepository extends JpaRepository<SplitPayInstallment, Long> {

    List<SplitPayInstallment> findByContractIdOrderByInstallmentNoAsc(Long contractId);

    Optional<SplitPayInstallment> findByPayToken(String payToken);

    Optional<SplitPayInstallment> findByOrderNo(String orderNo);

    @Query("""
            SELECT i FROM SplitPayInstallment i
            WHERE i.status = 'PENDING'
              AND i.dueDateAdjusted BETWEEN :from AND :to
            """)
    List<SplitPayInstallment> findPendingDueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
