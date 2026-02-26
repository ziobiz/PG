package com.pg.repository;

import com.pg.entity.CommissionHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommissionHistoryRepository extends JpaRepository<CommissionHistory, Long> {
    List<CommissionHistory> findByCompIdOrderByCreatedAtDesc(String compId, Pageable pageable);
}
