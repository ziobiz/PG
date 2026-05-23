package com.pg.repository;

import com.pg.entity.MerchantReceivable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MerchantReceivableRepository extends JpaRepository<MerchantReceivable, Long>, JpaSpecificationExecutor<MerchantReceivable> {

    List<MerchantReceivable> findByMerchantIdAndStatusInOrderByIdAsc(String merchantId, Collection<String> statuses);

    List<MerchantReceivable> findByMerchantIdOrderByIdDesc(String merchantId);

    boolean existsByMerchantIdAndReasonCodeAndMemo(String merchantId, String reasonCode, String memo);

    Optional<MerchantReceivable> findByMerchantIdAndReasonCodeAndMemo(String merchantId, String reasonCode, String memo);

    Optional<MerchantReceivable> findFirstByMerchantIdAndReasonCodeOrderByIdDesc(String merchantId, String reasonCode);

    Optional<MerchantReceivable> findFirstByMerchantIdAndReasonCodeInOrderByIdDesc(String merchantId, Collection<String> reasonCodes);

    List<MerchantReceivable> findByReasonCodeAndMemoIn(String reasonCode, Collection<String> memos);

    @Query("SELECT COUNT(r), COALESCE(SUM(r.remainingAmount), 0) FROM MerchantReceivable r WHERE UPPER(TRIM(r.status)) = 'PENDING'")
    Object[] dashboardPendingReceivableAll();

    @Query("SELECT COUNT(r), COALESCE(SUM(r.remainingAmount), 0) FROM MerchantReceivable r WHERE UPPER(TRIM(r.status)) = 'PENDING' AND r.merchantId IN :mids")
    Object[] dashboardPendingReceivableIn(@Param("mids") Collection<String> mids);

    @Query("SELECT r FROM MerchantReceivable r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<MerchantReceivable> findRecentCreatedAll(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT r FROM MerchantReceivable r WHERE r.createdAt >= :since AND r.merchantId IN :mids ORDER BY r.createdAt DESC")
    List<MerchantReceivable> findRecentCreatedIn(@Param("since") LocalDateTime since, @Param("mids") Collection<String> mids,
                                                 Pageable pageable);

    /** 미수금관리 목록·요약 — 취소·대손 제외 */
    @Query("""
            SELECT COUNT(r), COALESCE(SUM(r.remainingAmount), 0), COALESCE(SUM(r.totalAmount), 0)
            FROM MerchantReceivable r
            WHERE UPPER(TRIM(r.status)) NOT IN ('CANCELLED', 'WRITE_OFF')
            AND (:compIdPat IS NULL OR :compIdPat = '' OR lower(r.merchantId) LIKE lower(concat('%', :compIdPat, '%')) ESCAPE '\\')
            AND (:allMerchants = TRUE OR r.merchantId IN :merchantIds)
            """)
    Object[] summarizeManagementReceivables(
            @Param("compIdPat") String compIdPat,
            @Param("allMerchants") boolean allMerchants,
            @Param("merchantIds") Collection<String> merchantIds);
}
