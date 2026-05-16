package com.pg.repository;

import com.pg.entity.OrgTabletMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgTabletMenuRepository extends JpaRepository<OrgTabletMenu, Long> {

    List<OrgTabletMenu> findByOrgLevelOrderByPageUrlAsc(String orgLevel);

    void deleteByOrgLevel(String orgLevel);
}
