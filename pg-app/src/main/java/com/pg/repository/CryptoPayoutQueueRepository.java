package com.pg.repository;

import com.pg.entity.CryptoPayoutQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CryptoPayoutQueueRepository extends JpaRepository<CryptoPayoutQueue, Long> {

    @Query("SELECT p FROM CryptoPayoutQueue p WHERE " +
            "(:compId IS NULL OR :compId = '' OR LOWER(p.compId) LIKE LOWER(CONCAT('%', :compId, '%'))) AND " +
            "(:status IS NULL OR :status = '' OR p.status = :status) AND " +
            "(:payoutMode IS NULL OR :payoutMode = '' OR p.payoutMode = :payoutMode) " +
            "ORDER BY p.createdAt DESC")
    Page<CryptoPayoutQueue> search(@Param("compId") String compId,
                                   @Param("status") String status,
                                   @Param("payoutMode") String payoutMode,
                                   Pageable pageable);

    List<CryptoPayoutQueue> findByPayoutModeAndStatusAndScheduledAtLessThanEqual(
            String payoutMode, String status, LocalDateTime at);
}
