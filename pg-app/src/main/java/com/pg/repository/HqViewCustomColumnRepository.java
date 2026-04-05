package com.pg.repository;

import com.pg.entity.HqViewCustomColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HqViewCustomColumnRepository extends JpaRepository<HqViewCustomColumn, Long> {

    List<HqViewCustomColumn> findByPageUrlOrderBySortOrderAscIdAsc(String pageUrl);

    long countByPageUrl(String pageUrl);
}
