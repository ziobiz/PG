package com.pg.repository;

import com.pg.entity.OrgPagePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrgPagePermissionRepository extends JpaRepository<OrgPagePermission, Long> {

    long countByOrgLevel(String orgLevel);

    List<OrgPagePermission> findByOrgLevelOrderByPageUrlAsc(String orgLevel);

    Optional<OrgPagePermission> findByOrgLevelAndPageUrl(String orgLevel, String pageUrl);

    void deleteByOrgLevel(String orgLevel);
}
