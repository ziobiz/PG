package com.pg.repository;

import com.pg.entity.BalanceDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceDeductionRepository extends JpaRepository<BalanceDeduction, Long> {
    List<BalanceDeduction> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
}
