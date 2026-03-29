package com.pg.repository;

import com.pg.entity.OrgViewColumnAllowance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrgViewColumnAllowanceRepository extends JpaRepository<OrgViewColumnAllowance, Long> {

    Optional<OrgViewColumnAllowance> findByRegionalOrgCodeAndPageUrlAndViewerScope(
            String regionalOrgCode, String pageUrl, String viewerScope);

    List<OrgViewColumnAllowance> findByRegionalOrgCodeOrderByPageUrlAscViewerScopeAsc(String regionalOrgCode);

    void deleteByRegionalOrgCodeAndPageUrlAndViewerScope(String regionalOrgCode, String pageUrl, String viewerScope);
}
