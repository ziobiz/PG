package com.pg.repository;

import com.pg.entity.PgTrnsctn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
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

    /** JPAY 인라인·URL 결제 등 — 주문번호만으로 최신 거래 1건(출처 무관) */
    Optional<PgTrnsctn> findFirstByOrderNoOrderByCreatedAtDesc(String orderNo);

    /** 동일 주문번호 복수 건(무효·성공 등) — JPAY 통합조회 성공 건 우선 매칭용 */
    List<PgTrnsctn> findByOrderNoOrderByCreatedAtDesc(String orderNo);

    /** URL 인라인 DirectCredit 직후 적재(origin=URL) 등 — ChillPay RESULT URL(orderNo·transNo) 역추적 */
    Optional<PgTrnsctn> findFirstByOrderNoAndOriginOrderByCreatedAtDesc(String orderNo, String origin);

    /** 카드 해시·가맹 코드 기준 최신 거래 고객명(리스크 자동등록 이름 표시용) */
    @Query("SELECT t FROM PgTrnsctn t WHERE t.cardPanHash = :hash AND t.merchantId = :merchantId "
            + "AND t.customerNm IS NOT NULL AND TRIM(t.customerNm) <> '' ORDER BY t.createdAt DESC")
    List<PgTrnsctn> findRecentWithCustomerNmByCardPanHashAndMerchantId(
            @Param("hash") String hash, @Param("merchantId") String merchantId, Pageable pageable);

    /** 정산·수수료 집계: 결제일시 = COALESCE(paidAt, createdAt) — 리포트 {@link #findForReportRange} 와 동일 기준 */
    @Query("SELECT t FROM PgTrnsctn t WHERE " +
           "(:merchantId IS NULL OR :merchantId = '' OR t.merchantId = :merchantId) " +
           "AND COALESCE(t.paidAt, t.createdAt) >= :fromDt AND COALESCE(t.paidAt, t.createdAt) <= :toDt " +
           "ORDER BY COALESCE(t.paidAt, t.createdAt) ASC")
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

    /**
     * 메인 대시보드 집계: 결제일시 = COALESCE(paidAt, createdAt), 상한 {@code toExclusive} 미포함.
     * 반환: [전체건수, 승인(10)건수, 승인금액합]
     */
    @Query("SELECT COUNT(t), SUM(CASE WHEN t.status = '10' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = '10' THEN COALESCE(t.amtKrw, 0) ELSE 0 END) FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :fromInclusive AND COALESCE(t.paidAt, t.createdAt) < :toExclusive")
    Object[] dashboardAggregateAll(@Param("fromInclusive") LocalDateTime fromInclusive,
                                   @Param("toExclusive") LocalDateTime toExclusive);

    @Query("SELECT COUNT(t), SUM(CASE WHEN t.status = '10' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = '10' THEN COALESCE(t.amtKrw, 0) ELSE 0 END) FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :fromInclusive AND COALESCE(t.paidAt, t.createdAt) < :toExclusive " +
           "AND t.merchantId IN :merchantIds")
    Object[] dashboardAggregateForMerchants(@Param("fromInclusive") LocalDateTime fromInclusive,
                                          @Param("toExclusive") LocalDateTime toExclusive,
                                          @Param("merchantIds") Collection<String> merchantIds);

    /**
     * 통화별 집계(청구 통화 curType 기준). 반환 행: [통화코드, 전체건수, 승인건수, 승인금액합(해당 통화 단위 amtKrw)]
     */
    @Query("SELECT COALESCE(NULLIF(TRIM(t.curType), ''), 'KRW'), COUNT(t), " +
           "SUM(CASE WHEN t.status = '10' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = '10' THEN COALESCE(t.amtKrw, 0) ELSE 0 END) FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :fromInclusive AND COALESCE(t.paidAt, t.createdAt) < :toExclusive " +
           "GROUP BY COALESCE(NULLIF(TRIM(t.curType), ''), 'KRW') ORDER BY COALESCE(NULLIF(TRIM(t.curType), ''), 'KRW')")
    List<Object[]> dashboardAggregateByCurrencyAll(@Param("fromInclusive") LocalDateTime fromInclusive,
                                                    @Param("toExclusive") LocalDateTime toExclusive);

    @Query("SELECT COALESCE(NULLIF(TRIM(t.curType), ''), 'KRW'), COUNT(t), " +
           "SUM(CASE WHEN t.status = '10' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = '10' THEN COALESCE(t.amtKrw, 0) ELSE 0 END) FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :fromInclusive AND COALESCE(t.paidAt, t.createdAt) < :toExclusive " +
           "AND t.merchantId IN :merchantIds " +
           "GROUP BY COALESCE(NULLIF(TRIM(t.curType), ''), 'KRW') ORDER BY COALESCE(NULLIF(TRIM(t.curType), ''), 'KRW')")
    List<Object[]> dashboardAggregateByCurrencyForMerchants(@Param("fromInclusive") LocalDateTime fromInclusive,
                                                            @Param("toExclusive") LocalDateTime toExclusive,
                                                            @Param("merchantIds") Collection<String> merchantIds);

    /** 리스크 스코어용: 실패(99/F0)·무효계열·환불·취소 건수 */
    @Query("SELECT SUM(CASE WHEN t.status IN ('99','F0','f0') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status IN ('21','22','40','41','42') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status IN ('30','31') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = '20' THEN 1 ELSE 0 END) FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :f AND COALESCE(t.paidAt, t.createdAt) < :t")
    Object[] dashboardRiskBucketsAll(@Param("f") LocalDateTime f, @Param("t") LocalDateTime t);

    @Query("SELECT SUM(CASE WHEN t.status IN ('99','F0','f0') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status IN ('21','22','40','41','42') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status IN ('30','31') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status = '20' THEN 1 ELSE 0 END) FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :f AND COALESCE(t.paidAt, t.createdAt) < :t " +
           "AND t.merchantId IN :mids")
    Object[] dashboardRiskBucketsMerchants(@Param("f") LocalDateTime f, @Param("t") LocalDateTime t,
                                           @Param("mids") Collection<String> mids);

    @Query("SELECT t.merchantId, COUNT(t) FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :f AND COALESCE(t.paidAt, t.createdAt) < :t " +
           "AND t.status IN ('30','31') GROUP BY t.merchantId ORDER BY COUNT(t) DESC")
    List<Object[]> dashboardTopRefundMerchantsAll(@Param("f") LocalDateTime f, @Param("t") LocalDateTime t, Pageable pageable);

    @Query("SELECT t.merchantId, COUNT(t) FROM PgTrnsctn t WHERE " +
           "COALESCE(t.paidAt, t.createdAt) >= :f AND COALESCE(t.paidAt, t.createdAt) < :t " +
           "AND t.status IN ('30','31') AND t.merchantId IN :mids GROUP BY t.merchantId ORDER BY COUNT(t) DESC")
    List<Object[]> dashboardTopRefundMerchantsIn(@Param("f") LocalDateTime f, @Param("t") LocalDateTime t,
                                                   @Param("mids") Collection<String> mids, Pageable pageable);

    /** 일자별 결제내역 삭제(재노티) — created_at 기준 */
    @Query("SELECT t.trnId FROM PgTrnsctn t WHERE t.createdAt >= :from AND t.createdAt < :to " +
           "AND (:merchantId IS NULL OR :merchantId = '' OR t.merchantId = :merchantId)")
    List<String> findTrnIdsByCreatedAtRange(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            @Param("merchantId") String merchantId);

    /** JPAY 요청(08)·오승인(10) 대기 건 — Trade Query 자동 동기화 대상 */
    @Query("SELECT t FROM PgTrnsctn t WHERE t.status IN ('08', '10') " +
           "AND UPPER(t.van) LIKE 'JPAY%' " +
           "AND t.orderNo IS NOT NULL AND TRIM(t.orderNo) <> '' " +
           "AND t.createdAt <= :staleBefore AND t.createdAt >= :notOlderThan " +
           "ORDER BY t.createdAt ASC")
    List<PgTrnsctn> findStaleJpayPendingForReconcile(@Param("staleBefore") LocalDateTime staleBefore,
                                                     @Param("notOlderThan") LocalDateTime notOlderThan,
                                                     Pageable pageable);
}
