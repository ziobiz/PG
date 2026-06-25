package com.pg.repository;

import com.pg.entity.PayCardFailCooldown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayCardFailCooldownRepository extends JpaRepository<PayCardFailCooldown, Long> {

    Optional<PayCardFailCooldown> findByPgVendorAndPanHashAndOrgUnitId(String pgVendor, String panHash, Long orgUnitId);

    Optional<PayCardFailCooldown> findByPgVendorAndPanHashAndOrgUnitIdIsNull(String pgVendor, String panHash);
}
