package com.pg.repository;

import com.pg.entity.PgTrnsctn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PgTrnsctnRepository extends JpaRepository<PgTrnsctn, String>, JpaSpecificationExecutor<PgTrnsctn> {

    @Query("SELECT t FROM PgTrnsctn t WHERE " +
           "(:merchantId IS NULL OR :merchantId = '' OR t.merchantId = :merchantId) " +
           "AND t.createdAt >= :fromDt AND t.createdAt <= :toDt ORDER BY t.createdAt ASC")
    List<PgTrnsctn> findForSettlement(@Param("merchantId") String merchantId,
                                      @Param("fromDt") LocalDateTime fromDt,
                                      @Param("toDt") LocalDateTime toDt);
}
