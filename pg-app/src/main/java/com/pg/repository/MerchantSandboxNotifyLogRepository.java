package com.pg.repository;

import com.pg.entity.MerchantSandboxNotifyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MerchantSandboxNotifyLogRepository extends JpaRepository<MerchantSandboxNotifyLog, Long> {

    List<MerchantSandboxNotifyLog> findByCompIdOrderBySentAtDesc(String compId);

    @Query("SELECT l FROM MerchantSandboxNotifyLog l WHERE (:compId IS NULL OR :compId = '' OR LOWER(l.compId) = LOWER(:compId)) "
            + "ORDER BY l.sentAt DESC")
    List<MerchantSandboxNotifyLog> search(@Param("compId") String compId);

    @Modifying
    @Query("DELETE FROM MerchantSandboxNotifyLog l WHERE l.sentAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
