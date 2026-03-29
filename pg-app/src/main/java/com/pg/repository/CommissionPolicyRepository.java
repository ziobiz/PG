package com.pg.repository;

import com.pg.entity.CommissionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, Long> {

    Optional<CommissionPolicy> findByScope(String scope);
    List<CommissionPolicy> findByScopeStartingWithOrderByScopeAsc(String prefix);
    Optional<CommissionPolicy> findFirstByScopeStartingWithAndDeployYnOrderByUpdatedAtDesc(String prefix, String deployYn);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CommissionPolicy p SET p.chargebackPolicyId = NULL WHERE p.chargebackPolicyId = :cid")
    int clearChargebackPolicyLink(@Param("cid") Long chargebackPolicyId);

    long countByChargebackPolicyId(Long chargebackPolicyId);
}
