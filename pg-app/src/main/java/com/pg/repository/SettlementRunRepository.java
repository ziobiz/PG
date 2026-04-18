package com.pg.repository;

import com.pg.entity.SettlementRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SettlementRunRepository extends JpaRepository<SettlementRun, Long> {

    List<SettlementRun> findByCalcDtBetweenOrderByMerchantId(LocalDate from, LocalDate to);

    List<SettlementRun> findByCalcDtAndMerchantId(LocalDate calcDt, String merchantId);

    boolean existsByMerchantIdAndCalcDt(String merchantId, LocalDate calcDt);

    boolean existsByMerchantIdAndCalcDtAndPeriodEndAt(String merchantId, LocalDate calcDt, LocalDateTime periodEndAt);

    /**
     * 당일 누적 재집계(T0·TM/TH) 등에서 동일 가맹·정산일 행을 비울 때 사용.
     * {@code merchant_id} 대소문자·앞뒤 공백 불일치로 과거 행이 남아 누적 행이 쌓이는 것을 막기 위해 비교는 정규화합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SettlementRun r where lower(trim(r.merchantId)) = lower(trim(:merchantId)) and r.calcDt = :calcDt")
    int deleteByMerchantIdAndCalcDt(@Param("merchantId") String merchantId, @Param("calcDt") LocalDate calcDt);

    /** 해당 가맹·기간(포함) 내 정산 실행 건수 — 월간 고정 이용료 1회 부과 여부 판단용 */
    long countByMerchantIdAndCalcDtBetween(String merchantId, LocalDate calcDtStart, LocalDate calcDtEnd);
}
