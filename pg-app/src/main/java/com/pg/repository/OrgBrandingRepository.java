package com.pg.repository;

import com.pg.entity.OrgBranding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrgBrandingRepository extends JpaRepository<OrgBranding, Long> {

    Optional<OrgBranding> findByOrgUnitId(Long orgUnitId);
}
