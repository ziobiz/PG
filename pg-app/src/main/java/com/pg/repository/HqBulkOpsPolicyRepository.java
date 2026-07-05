package com.pg.repository;

import com.pg.entity.HqBulkOpsPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HqBulkOpsPolicyRepository extends JpaRepository<HqBulkOpsPolicy, Long> {

    Optional<HqBulkOpsPolicy> findByPolicyType(String policyType);
}
