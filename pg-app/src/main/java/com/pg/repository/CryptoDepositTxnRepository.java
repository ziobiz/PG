package com.pg.repository;

import com.pg.entity.CryptoDepositTxn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CryptoDepositTxnRepository extends JpaRepository<CryptoDepositTxn, Long> {
    Optional<CryptoDepositTxn> findByOrgUnitIdAndOrderNo(Long orgUnitId, String orderNo);
    Optional<CryptoDepositTxn> findBySessionToken(String sessionToken);

    @Query("SELECT t FROM CryptoDepositTxn t WHERE " +
            "(:compId IS NULL OR :compId = '' OR LOWER(t.compId) LIKE LOWER(CONCAT('%', :compId, '%'))) AND " +
            "(:status IS NULL OR :status = '' OR t.status = :status) " +
            "ORDER BY t.createdAt DESC")
    Page<CryptoDepositTxn> search(@Param("compId") String compId,
                                  @Param("status") String status,
                                  Pageable pageable);
}
