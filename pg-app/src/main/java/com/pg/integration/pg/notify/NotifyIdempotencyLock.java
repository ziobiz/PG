package com.pg.integration.pg.notify;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 노티 중복 수신(특히 <strong>동시 수신</strong>) 시, 같은 거래에 대한 처리를 직렬화하기 위한
 * PostgreSQL 트랜잭션 단위 advisory lock 도우미입니다.
 *
 * <p>설계 원칙(기존 동작 보존):
 * <ul>
 *   <li>호출하는 노티 핸들러는 이미 {@code @Transactional} 안에서 실행되므로,
 *       {@code pg_advisory_xact_lock} 은 해당 트랜잭션이 커밋/롤백될 때 자동 해제됩니다.
 *       즉 별도의 unlock 코드가 필요 없습니다.</li>
 *   <li>같은 거래키에 대한 동시 노티만 직렬화하며, <strong>서로 다른 거래는 절대 막지 않습니다.</strong></li>
 *   <li>어떤 이유로든 락 획득에 실패해도 <strong>예외를 던지지 않습니다.</strong>
 *       (DEBUG 로그만 남기고 정상 흐름을 그대로 진행 → 기존 동작과 동일하게 폴백)</li>
 * </ul>
 *
 * <p>이 컴포넌트는 새로 발생할 수 있는 동시 중복 처리만 보강하며,
 * 기존 단건/순차 처리 경로(상태 머지, {@code settled_yn} 게이트, 아웃바운드 dedup)는 변경하지 않습니다.
 */
@Component
public class NotifyIdempotencyLock {

    private static final Logger log = LoggerFactory.getLogger(NotifyIdempotencyLock.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 거래 식별 키 단위 트랜잭션 advisory lock(best-effort). 현재 트랜잭션이 끝나면 자동 해제됩니다.
     *
     * @param namespace 벤더/도메인 구분(예: {@code "CHILLPAY"}, {@code "JPAY"}) — 키 공간 분리용
     * @param key       같은 거래의 중복 노티가 동일하게 산출하는 문자열 키(예: 거래ID 또는 가맹점|주문번호)
     */
    public void lock(String namespace, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            long h = hash64((namespace == null ? "" : namespace) + "|" + key.trim());
            entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(?1)")
                    .setParameter(1, h)
                    .getResultList();
        } catch (Exception e) {
            log.debug("노티 advisory lock 생략(폴백 진행) ns={} err={}", namespace, e.getMessage());
        }
    }

    /** FNV-1a 64bit. 문자열을 안정적인 64bit 정수 키로 변환합니다(advisory lock 키 충돌 최소화). */
    private static long hash64(String s) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
