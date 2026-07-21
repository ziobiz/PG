package com.pg.repository;

import com.pg.entity.NotiProvisionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotiProvisionLogRepository extends JpaRepository<NotiProvisionLog, Long> {

    @Query("SELECT l FROM NotiProvisionLog l WHERE "
            + "(:compId = '' OR LOWER(l.compId) LIKE LOWER(CONCAT('%', :compId, '%'))) "
            + "ORDER BY l.provisionedAt DESC, l.id DESC")
    Page<NotiProvisionLog> search(@Param("compId") String compId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(l.slotNo), 0) FROM NotiProvisionLog l WHERE "
            + "l.baseCurrency = :baseCurrency AND l.slotNo >= :minSlot")
    Integer findMaxSlotForCurrency(@Param("baseCurrency") String baseCurrency, @Param("minSlot") int minSlot);

    Optional<NotiProvisionLog> findFirstByOrgUnitIdOrderByProvisionedAtDescIdDesc(Long orgUnitId);
}
