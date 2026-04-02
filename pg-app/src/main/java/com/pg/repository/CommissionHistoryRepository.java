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

    /** 조회 시 대소문자·입력 편차에도 동일 가맹점 이력 매칭 */
    List<CommissionHistory> findByCompIdIgnoreCaseOrderByCreatedAtDesc(String compId);

    /** 구간(시작·종료일시) 계산용 — 과거 → 최신 순 */
    List<CommissionHistory> findByCompIdIgnoreCaseOrderByCreatedAtAsc(String compId);
}
