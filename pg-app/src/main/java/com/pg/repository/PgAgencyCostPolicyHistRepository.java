package com.pg.repository;

import com.pg.entity.PgAgencyCostPolicyHist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PgAgencyCostPolicyHistRepository extends JpaRepository<PgAgencyCostPolicyHist, Long> {
    List<PgAgencyCostPolicyHist> findByPolicyIdOrderByCreatedAtDesc(Long policyId);

    void deleteByPolicyId(Long policyId);
}
