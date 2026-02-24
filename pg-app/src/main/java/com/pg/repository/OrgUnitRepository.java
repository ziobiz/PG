package com.pg.repository;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrgUnitRepository extends JpaRepository<OrgUnit, Long> {

    List<OrgUnit> findByOrgLevelOrderByCodeAsc(OrgLevel orgLevel);
    List<OrgUnit> findByParentIdOrderByCodeAsc(Long parentId);
    Optional<OrgUnit> findByCode(String code);
}
