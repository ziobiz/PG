package com.pg.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 표준 시간대({@code display_timezone}) 변경 시 — DB naive 시각을 동일 Instant 기준 새 벽시계로 변환.
 */
@Service
public class LedgerDisplayTimezoneMigrationService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * {@code pg_trnsctn.created_at}·{@code paid_at} — PostgreSQL AT TIME ZONE 변환.
     *
     * @return 갱신된 행 수(created_at·paid_at 각 UPDATE 합)
     */
    @Transactional
    public int migratePgTrnsctnWallClock(String fromZoneId, String toZoneId) {
        if (fromZoneId == null || toZoneId == null || fromZoneId.isBlank() || toZoneId.isBlank()) {
            return 0;
        }
        String from = fromZoneId.trim();
        String to = toZoneId.trim();
        if (from.equals(to)) {
            return 0;
        }
        int created = entityManager.createNativeQuery("""
                UPDATE pg_trnsctn
                SET created_at = ((created_at AT TIME ZONE :fromZ) AT TIME ZONE :toZ)
                WHERE created_at IS NOT NULL
                """)
                .setParameter("fromZ", from)
                .setParameter("toZ", to)
                .executeUpdate();
        int paid = entityManager.createNativeQuery("""
                UPDATE pg_trnsctn
                SET paid_at = ((paid_at AT TIME ZONE :fromZ) AT TIME ZONE :toZ)
                WHERE paid_at IS NOT NULL
                """)
                .setParameter("fromZ", from)
                .setParameter("toZ", to)
                .executeUpdate();
        return created + paid;
    }
}
