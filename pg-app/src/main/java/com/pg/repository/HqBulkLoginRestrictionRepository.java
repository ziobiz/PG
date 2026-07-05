package com.pg.repository;

import com.pg.entity.HqBulkLoginRestriction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HqBulkLoginRestrictionRepository extends JpaRepository<HqBulkLoginRestriction, Long> {

    List<HqBulkLoginRestriction> findByStatusOrderByIdDesc(String status);
}
