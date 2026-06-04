package com.pg.repository;

import com.pg.entity.PgAgencyCostPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PgAgencyCostPolicyRepository extends JpaRepository<PgAgencyCostPolicy, Long> {
    Optional<PgAgencyCostPolicy> findByPgCd(String pgCd);
    List<PgAgencyCostPolicy> findAllByOrderByPgCdAsc();
}
