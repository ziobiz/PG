package com.pg.repository;

import com.pg.entity.CommissionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, Long> {

    Optional<CommissionPolicy> findByScope(String scope);
    List<CommissionPolicy> findByScopeStartingWithOrderByScopeAsc(String prefix);
    Optional<CommissionPolicy> findFirstByScopeStartingWithAndDeployYnOrderByUpdatedAtDesc(String prefix, String deployYn);
}
