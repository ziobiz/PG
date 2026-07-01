package com.pg.repository;

import com.pg.entity.PayCardFailCooldown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayCardFailCooldownRepository extends JpaRepository<PayCardFailCooldown, Long> {

    Optional<PayCardFailCooldown> findByPgVendorAndPanHashAndOrgUnitId(String pgVendor, String panHash, Long orgUnitId);

    Optional<PayCardFailCooldown> findByPgVendorAndPanHashAndOrgUnitIdIsNull(String pgVendor, String panHash);

    @Query("""
            SELECT c FROM PayCardFailCooldown c
            WHERE c.pgVendor = :pg
            AND (c.panHash = :panHash OR (:panMaskKey IS NOT NULL AND :panMaskKey <> '' AND c.panMaskKey = :panMaskKey))
            """)
    List<PayCardFailCooldown> findAllByPgAndPanIdentity(@Param("pg") String pgVendor,
                                                        @Param("panHash") String panHash,
                                                        @Param("panMaskKey") String panMaskKey);
}
