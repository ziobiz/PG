package com.pg.repository;

import com.pg.entity.MerchantCommissionExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantCommissionExtraRepository extends JpaRepository<MerchantCommissionExtra, Long> {
    Optional<MerchantCommissionExtra> findByOrgUnitId(Long orgUnitId);
}
