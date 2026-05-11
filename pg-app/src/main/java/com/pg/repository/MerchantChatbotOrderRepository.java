package com.pg.repository;

import com.pg.entity.MerchantChatbotOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MerchantChatbotOrderRepository extends JpaRepository<MerchantChatbotOrder, Long> {

    Optional<MerchantChatbotOrder> findByCheckoutOrderNo(String checkoutOrderNo);

    boolean existsByCheckoutOrderNo(String checkoutOrderNo);

    List<MerchantChatbotOrder> findTop200ByOrgUnitIdOrderByCreatedAtDesc(Long orgUnitId);

    @Query("""
            SELECT COUNT(o) FROM MerchantChatbotOrder o
            WHERE o.orgUnitId = :orgUnitId
              AND o.productId = :productId
              AND o.status IN ('PENDING_PAYMENT', 'CONFIRMED')
              AND o.reservationStart IS NOT NULL AND o.reservationEnd IS NOT NULL
              AND o.reservationStart < :end AND o.reservationEnd > :start
              AND (:excludeId IS NULL OR o.id <> :excludeId)
            """)
    long countReservationOverlap(@Param("orgUnitId") Long orgUnitId,
                                 @Param("productId") Long productId,
                                 @Param("start") Instant start,
                                 @Param("end") Instant end,
                                 @Param("excludeId") Long excludeId);
}
