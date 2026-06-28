package com.pg.repository;

import com.pg.entity.PayCardFailRiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PayCardFailRiskEventRepository extends JpaRepository<PayCardFailRiskEvent, Long> {

    @Query("""
            SELECT COUNT(e) FROM PayCardFailRiskEvent e
            WHERE e.pgVendor = :pg AND e.panHash = :hash
              AND ((:orgUnitId IS NULL AND e.orgUnitId IS NULL) OR e.orgUnitId = :orgUnitId)
            """)
    long countAllForCard(@Param("pg") String pg,
                         @Param("hash") String hash,
                         @Param("orgUnitId") Long orgUnitId);

    @Query("""
            SELECT COUNT(e) FROM PayCardFailRiskEvent e
            WHERE e.pgVendor = :pg AND e.panHash = :hash
              AND ((:orgUnitId IS NULL AND e.orgUnitId IS NULL) OR e.orgUnitId = :orgUnitId)
              AND e.occurredAt >= :since
            """)
    long countSinceForCard(@Param("pg") String pg,
                           @Param("hash") String hash,
                           @Param("orgUnitId") Long orgUnitId,
                           @Param("since") LocalDateTime since);

    @Modifying
    @Query("""
            DELETE FROM PayCardFailRiskEvent e
            WHERE e.pgVendor = :pg AND e.panHash = :hash
              AND ((:orgUnitId IS NULL AND e.orgUnitId IS NULL) OR e.orgUnitId = :orgUnitId)
            """)
    void deleteAllForCard(@Param("pg") String pg,
                          @Param("hash") String hash,
                          @Param("orgUnitId") Long orgUnitId);

    @Modifying
    @Query("""
            DELETE FROM PayCardFailRiskEvent e
            WHERE e.pgVendor = :pg AND e.panHash = :hash
              AND ((:orgUnitId IS NULL AND e.orgUnitId IS NULL) OR e.orgUnitId = :orgUnitId)
              AND e.occurredAt < :before
            """)
    int deleteOlderThanForCard(@Param("pg") String pg,
                               @Param("hash") String hash,
                               @Param("orgUnitId") Long orgUnitId,
                               @Param("before") LocalDateTime before);
}
