package com.pg.repository;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrgUnitRepository extends JpaRepository<OrgUnit, Long> {

    List<OrgUnit> findByOrgLevelInOrderByNameAsc(Collection<OrgLevel> levels);

    List<OrgUnit> findByOrgLevelOrderByCodeAsc(OrgLevel orgLevel);
    List<OrgUnit> findByParentIdOrderByCodeAsc(Long parentId);
    Optional<OrgUnit> findByCode(String code);

    Optional<OrgUnit> findByCodeIgnoreCase(String code);

    @Query("SELECT o FROM OrgUnit o WHERE o.orgLevel = :lvl AND LOWER(o.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<OrgUnit> findByOrgLevelAndNameContainingIgnoreCase(@Param("lvl") OrgLevel lvl, @Param("q") String q);

    @Query("SELECT o FROM OrgUnit o WHERE o.orgLevel = :lvl AND LOWER(o.code) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<OrgUnit> findByOrgLevelAndCodeContainingIgnoreCase(@Param("lvl") OrgLevel lvl, @Param("q") String q);

    @Query("SELECT o FROM OrgUnit o WHERE o.code IN :codes")
    List<OrgUnit> findByCodeIn(@Param("codes") Collection<String> codes);
}
