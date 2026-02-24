package com.pg.repository;

import com.pg.entity.CommissionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, Long> {

    Optional<CommissionPolicy> findByScope(String scope);
}
