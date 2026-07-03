package com.pg.repository;

import com.pg.entity.PayRiskFilterEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PayRiskFilterEventRepository extends JpaRepository<PayRiskFilterEvent, Long> {

    @Query("SELECT e FROM PayRiskFilterEvent e WHERE "
            + "(:merchantId IS NULL OR :merchantId = '' OR e.merchantId = :merchantId) AND "
            + "(:filterCode IS NULL OR :filterCode = '' OR e.filterCode = :filterCode) AND "
            + "e.createdAt >= :fromDt AND e.createdAt <= :toDt "
            + "ORDER BY e.createdAt DESC")
    Page<PayRiskFilterEvent> search(@Param("merchantId") String merchantId,
                                    @Param("filterCode") String filterCode,
                                    @Param("fromDt") LocalDateTime fromDt,
                                    @Param("toDt") LocalDateTime toDt,
                                    Pageable pageable);
}
