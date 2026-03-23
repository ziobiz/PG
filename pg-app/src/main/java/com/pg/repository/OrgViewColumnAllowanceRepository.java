package com.pg.repository;

import com.pg.entity.OrgViewColumnAllowance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrgViewColumnAllowanceRepository extends JpaRepository<OrgViewColumnAllowance, Long> {

    Optional<OrgViewColumnAllowance> findByRegionalOrgCodeAndPageUrl(String regionalOrgCode, String pageUrl);

    void deleteByRegionalOrgCodeAndPageUrl(String regionalOrgCode, String pageUrl);
}
