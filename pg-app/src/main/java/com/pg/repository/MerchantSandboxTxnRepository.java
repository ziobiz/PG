package com.pg.repository;

import com.pg.entity.MerchantSandboxTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MerchantSandboxTxnRepository extends JpaRepository<MerchantSandboxTxn, Long> {

    Optional<MerchantSandboxTxn> findByOrgUnitIdAndOrderNo(Long orgUnitId, String orderNo);

    Optional<MerchantSandboxTxn> findBySessionToken(String sessionToken);

    List<MerchantSandboxTxn> findByOrgUnitIdOrderByCreatedAtDesc(Long orgUnitId);

    @Query("SELECT t FROM MerchantSandboxTxn t WHERE (:compId IS NULL OR :compId = '' OR LOWER(t.compId) = LOWER(:compId)) "
            + "AND (:status IS NULL OR :status = '' OR t.status = :status) "
            + "ORDER BY t.createdAt DESC")
    List<MerchantSandboxTxn> search(@Param("compId") String compId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM MerchantSandboxTxn t WHERE t.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
