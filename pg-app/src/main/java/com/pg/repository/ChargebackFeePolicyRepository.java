package com.pg.repository;

import com.pg.entity.ChargebackFeePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChargebackFeePolicyRepository extends JpaRepository<ChargebackFeePolicy, Long> {

    List<ChargebackFeePolicy> findAllByOrderByNameAsc();

    @Query("SELECT DISTINCT p FROM ChargebackFeePolicy p LEFT JOIN FETCH p.tiers WHERE p.id = :id")
    Optional<ChargebackFeePolicy> findByIdWithTiers(@Param("id") Long id);
}
