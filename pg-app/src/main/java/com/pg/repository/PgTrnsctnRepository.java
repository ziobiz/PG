package com.pg.repository;

import com.pg.entity.PgTrnsctn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PgTrnsctnRepository extends JpaRepository<PgTrnsctn, String>, JpaSpecificationExecutor<PgTrnsctn> {

    Optional<PgTrnsctn> findFirstByChillTransactionIdAndMerchantId(String chillTransactionId, String merchantId);

    Optional<PgTrnsctn> findFirstByMerchantIdAndOrderNoAndOrigin(String merchantId, String orderNo, String origin);

    @Query("SELECT t FROM PgTrnsctn t WHERE " +
           "(:merchantId IS NULL OR :merchantId = '' OR t.merchantId = :merchantId) " +
           "AND t.createdAt >= :fromDt AND t.createdAt <= :toDt ORDER BY t.createdAt ASC")
    List<PgTrnsctn> findForSettlement(@Param("merchantId") String merchantId,
                                      @Param("fromDt") LocalDateTime fromDt,
                                      @Param("toDt") LocalDateTime toDt);

    /** 정산 리포트: 결제일시 = COALESCE(paidAt, createdAt) */
    @Query("SELECT t FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :fromDt AND COALESCE(t.paidAt, t.createdAt) <= :toDt " +
           "AND (:curType IS NULL OR t.curType = :curType) ORDER BY COALESCE(t.paidAt, t.createdAt) ASC")
    List<PgTrnsctn> findForReportRange(@Param("fromDt") LocalDateTime fromDt,
                                       @Param("toDt") LocalDateTime toDt,
                                       @Param("curType") String curType);

    /** 차지백 구간 산정용: 해당 월 환불(30)·강제환불(31) 건수 */
    long countByMerchantIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String merchantId,
            Collection<String> statuses,
            LocalDateTime createdAtGreaterThanEqual,
            LocalDateTime createdAtLessThan);
}
