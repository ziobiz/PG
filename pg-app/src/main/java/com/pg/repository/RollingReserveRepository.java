package com.pg.repository;

import com.pg.entity.RollingReserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RollingReserveRepository extends JpaRepository<RollingReserve, Long> {

    boolean existsByTrnIdAndStatus(String trnId, String status);

    List<RollingReserve> findByMerchantIdAndStatusOrderByCreatedAtDesc(String merchantId, String status);

    /** 정산 실행일(calcDt)에 해지 대상: 해지일(포함) 이전 또는 당일까지 보류였던 건 */
    List<RollingReserve> findByMerchantIdAndStatusAndReleaseDateLessThanEqual(String merchantId, String status, LocalDate releaseDate);

    List<RollingReserve> findByStatusAndReleaseDateLessThanEqual(String status, LocalDate releaseDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RollingReserve r WHERE r.trnId IN :trnIds")
    int deleteByTrnIdIn(@Param("trnIds") Collection<String> trnIds);
}
