package com.pg.repository;

import com.pg.entity.PgTrnsctn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PgTrnsctnRepository extends JpaRepository<PgTrnsctn, String>, JpaSpecificationExecutor<PgTrnsctn> {

    Optional<PgTrnsctn> findFirstByChillTransactionIdAndMerchantId(String chillTransactionId, String merchantId);

    /**
     * 동일 MID·다 가맹점 노티가 잘못된 업체코드로 파싱될 때 — 승인번호(TransactionId)는 칠페이 전역 유일로 보고 기존 행에 병합.
     */
    Optional<PgTrnsctn> findFirstByChillTransactionIdOrderByCreatedAtDesc(String chillTransactionId);

    Optional<PgTrnsctn> findFirstByMerchantIdAndOrderNoAndOrigin(String merchantId, String orderNo, String origin);

    /** URL 인라인 DirectCredit 직후 적재(origin=URL) 등 — ChillPay RESULT URL(orderNo·transNo) 역추적 */
    Optional<PgTrnsctn> findFirstByOrderNoAndOriginOrderByCreatedAtDesc(String orderNo, String origin);

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

    /** 통합정산 등: 칠페이 승인번호 다건으로 최신 내부 상태 조회 */
    List<PgTrnsctn> findAllByChillTransactionIdIn(Collection<String> chillTransactionIds);

    /** 정산 데이터 초기화: 정산 실행 삭제 후 거래의 정산 반영 플래그만 해제(거래·수수료내역 행은 유지). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PgTrnsctn t SET t.settledYn = 'N'")
    int clearAllSettlementFlagsOnTransactions();
}
