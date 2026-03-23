package com.pg.repository;

import com.pg.entity.CommissionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommissionHistoryRepository extends JpaRepository<CommissionHistory, Long> {
    Page<CommissionHistory> findByCompIdOrderByCreatedAtDesc(String compId, Pageable pageable);

    /** compId별 전체 이력(최신순) — 종료일시 계산용 */
    List<CommissionHistory> findAllByCompIdOrderByCreatedAtDesc(String compId);
}
