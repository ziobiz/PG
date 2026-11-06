package com.pg.repository;

import com.pg.entity.CryptoDepositTxn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CryptoDepositTxnRepository extends JpaRepository<CryptoDepositTxn, Long> {
    Optional<CryptoDepositTxn> findByOrgUnitIdAndOrderNo(Long orgUnitId, String orderNo);
    Optional<CryptoDepositTxn> findBySessionToken(String sessionToken);

    Optional<CryptoDepositTxn> findFirstByDepositTxHashIgnoreCase(String depositTxHash);

    @Query("SELECT t FROM CryptoDepositTxn t WHERE t.status IN :statuses "
            + "AND (t.expireAt IS NULL OR t.expireAt > :now) ORDER BY t.id ASC")
    List<CryptoDepositTxn> findWatchCandidates(@Param("statuses") Collection<String> statuses,
                                               @Param("now") LocalDateTime now,
                                               Pageable pageable);

    @Query("SELECT t FROM CryptoDepositTxn t WHERE " +
            "(:compId IS NULL OR :compId = '' OR LOWER(t.compId) LIKE LOWER(CONCAT('%', :compId, '%'))) AND " +
            "(:status IS NULL OR :status = '' OR t.status = :status) " +
            "ORDER BY t.createdAt DESC")
    Page<CryptoDepositTxn> search(@Param("compId") String compId,
                                  @Param("status") String status,
                                  Pageable pageable);
}
