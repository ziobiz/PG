package com.pg.repository;

import com.pg.entity.OrgUnitPagePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgUnitPagePermissionRepository extends JpaRepository<OrgUnitPagePermission, Long> {

    List<OrgUnitPagePermission> findByOrgUnitIdOrderByPageUrlAsc(Long orgUnitId);

    void deleteByOrgUnitId(Long orgUnitId);
}
