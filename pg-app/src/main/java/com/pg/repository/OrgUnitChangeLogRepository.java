package com.pg.repository;

import com.pg.entity.OrgUnitChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrgUnitChangeLogRepository extends JpaRepository<OrgUnitChangeLog, Long>, JpaSpecificationExecutor<OrgUnitChangeLog> {
}
