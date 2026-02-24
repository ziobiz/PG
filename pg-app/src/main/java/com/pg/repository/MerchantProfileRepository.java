package com.pg.repository;

import com.pg.entity.MerchantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, Long> {

    Optional<MerchantProfile> findByOrgUnitId(Long orgUnitId);
}

