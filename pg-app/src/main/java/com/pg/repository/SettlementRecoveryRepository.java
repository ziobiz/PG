package com.pg.repository;

import com.pg.entity.SettlementRecovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SettlementRecoveryRepository extends JpaRepository<SettlementRecovery, Long>, JpaSpecificationExecutor<SettlementRecovery> {

    boolean existsByTrnIdAndReasonCode(String trnId, String reasonCode);

    List<SettlementRecovery> findByMerchantIdAndStatusInOrderByIdAsc(String merchantId, Collection<String> statuses);

    List<SettlementRecovery> findByMerchantIdOrderByIdDesc(String merchantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SettlementRecovery r WHERE r.trnId IN :trnIds")
    int deleteByTrnIdIn(@Param("trnIds") Collection<String> trnIds);
}
