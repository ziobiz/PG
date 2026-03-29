package com.pg.repository;

import com.pg.entity.OrgUnitAssistantPagePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgUnitAssistantPagePermissionRepository extends JpaRepository<OrgUnitAssistantPagePermission, Long> {

    List<OrgUnitAssistantPagePermission> findByOrgUnitIdOrderByAssistantRoleTypeAscPageUrlAsc(long orgUnitId);

    void deleteByOrgUnitId(long orgUnitId);
}
