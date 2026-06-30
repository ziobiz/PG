package com.pg.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 노티수령정보(tb_pg_notify_inbound) — 루프·재유입 등으로 쌓인 중복 건 일괄 삭제.
 * <ul>
 *   <li>OUTBOUND_ECHO 및 ICOPAY 결제통보({@code event=pg.payment.status}) 재유입 본문</li>
 *   <li>동일 {@code raw_body} 중복(가장 이른 id 1건만 유지)</li>
 * </ul>
 */
@Service
public class HqNotifyInboundDuplicateCleanupService {

    private static final Logger log = LoggerFactory.getLogger(HqNotifyInboundDuplicateCleanupService.class);

    /** {@link com.pg.util.PaidApprovalEvidenceGuard#isIcopayOutboundPaymentStatusEcho} 와 동일 판별( SQL ILIKE ). JSON 시작 `{` 는 Hibernate 파서 충돌 방지로 CHR(123) 사용 */
    private static final String ECHO_BODY_SQL = """
            (n.raw_body IS NOT NULL
             AND LEFT(TRIM(n.raw_body), 1) = CHR(123)
             AND n.raw_body ILIKE '%pg.payment.status%'
             AND (n.raw_body ILIKE '%"trnId"%' OR n.raw_body ILIKE '%"trn_id"%')
             AND (n.raw_body ILIKE '%"compId"%' OR n.raw_body ILIKE '%"comp_id"%'))""";

    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    @PersistenceContext
    private EntityManager entityManager;

    public HqNotifyInboundDuplicateCleanupService(HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(LocalDate fromDate,
                                       LocalDate toDate,
                                       String merchantId,
                                       boolean removeOutboundEcho,
                                       boolean removeExactRawBodyDuplicates) {
        Scope scope = resolveScope(fromDate, toDate, merchantId);
        Map<String, Object> out = baseResult(scope, true);
        if (removeOutboundEcho) {
            out.put("outboundEchoCount", countOutboundEcho(scope));
        } else {
            out.put("outboundEchoCount", 0L);
        }
        if (removeExactRawBodyDuplicates) {
            out.put("rawBodyDuplicateCount", countRawBodyDuplicates(scope));
        } else {
            out.put("rawBodyDuplicateCount", 0L);
        }
        return out;
    }

    @Transactional
    public Map<String, Object> cleanup(LocalDate fromDate,
                                       LocalDate toDate,
                                       String merchantId,
                                       boolean removeOutboundEcho,
                                       boolean removeExactRawBodyDuplicates) {
        Scope scope = resolveScope(fromDate, toDate, merchantId);
        long echoWould = removeOutboundEcho ? countOutboundEcho(scope) : 0L;
        long dupWould = removeExactRawBodyDuplicates ? countRawBodyDuplicates(scope) : 0L;

        int echoDeleted = 0;
        int dupDeleted = 0;
        if (removeOutboundEcho && echoWould > 0) {
            echoDeleted = deleteOutboundEcho(scope);
        }
        if (removeExactRawBodyDuplicates && dupWould > 0) {
            dupDeleted = deleteRawBodyDuplicates(scope);
        }

        log.warn("notify inbound duplicate cleanup zone={} from={} to={} merchantId={} echoDeleted={} dupDeleted={}",
                scope.zone().getId(), scope.from(), scope.to(), scope.merchantId() != null ? scope.merchantId() : "*",
                echoDeleted, dupDeleted);

        Map<String, Object> out = baseResult(scope, false);
        out.put("outboundEchoDeleted", echoDeleted);
        out.put("rawBodyDuplicateDeleted", dupDeleted);
        out.put("totalDeleted", echoDeleted + dupDeleted);
        return out;
    }

    private Map<String, Object> baseResult(Scope scope, boolean dryRun) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dryRun", dryRun);
        m.put("timezone", scope.zone().getId());
        if (scope.fromDate() != null) {
            m.put("fromDate", scope.fromDate().toString());
        }
        if (scope.toDate() != null) {
            m.put("toDate", scope.toDate().toString());
        }
        m.put("merchantId", scope.merchantId());
        return m;
    }

    private Scope resolveScope(LocalDate fromDate, LocalDate toDate, String merchantId) {
        ZoneId zone = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay(zone).toLocalDateTime() : null;
        LocalDateTime to = toDate != null ? toDate.plusDays(1).atStartOfDay(zone).toLocalDateTime() : null;
        String mid = normalizeMerchantId(merchantId);
        return new Scope(zone, fromDate, toDate, from, to, mid);
    }

    private long countOutboundEcho(Scope scope) {
        String sql = "SELECT COUNT(n.id) FROM tb_pg_notify_inbound n WHERE "
                + outboundEchoPredicate()
                + scopeSql(scope);
        return scalarLong(bindScope(createQuery(sql), scope));
    }

    private int deleteOutboundEcho(Scope scope) {
        String sql = "DELETE FROM tb_pg_notify_inbound n WHERE "
                + outboundEchoPredicate()
                + scopeSql(scope);
        return bindScope(entityManager.createNativeQuery(sql), scope).executeUpdate();
    }

    private static String outboundEchoPredicate() {
        return "(UPPER(TRIM(COALESCE(n.process_status, ''))) = 'OUTBOUND_ECHO' OR " + ECHO_BODY_SQL + ")";
    }

    private long countRawBodyDuplicates(Scope scope) {
        String sql = """
                SELECT COALESCE(SUM(cnt - 1), 0) FROM (
                  SELECT COUNT(*) AS cnt
                  FROM tb_pg_notify_inbound n
                  WHERE n.raw_body IS NOT NULL AND LENGTH(TRIM(n.raw_body)) > 0"""
                + scopeSql(scope)
                + """
                  GROUP BY n.raw_body
                  HAVING COUNT(*) > 1
                ) t""";
        return scalarLong(bindScope(createQuery(sql), scope));
    }

    private int deleteRawBodyDuplicates(Scope scope) {
        String sql = """
                DELETE FROM tb_pg_notify_inbound a
                USING (
                  SELECT n.raw_body, MIN(n.id) AS keep_id
                  FROM tb_pg_notify_inbound n
                  WHERE n.raw_body IS NOT NULL AND LENGTH(TRIM(n.raw_body)) > 0"""
                + scopeSql(scope)
                + """
                  GROUP BY n.raw_body
                  HAVING COUNT(*) > 1
                ) d
                WHERE a.raw_body = d.raw_body AND a.id > d.keep_id""";
        return bindScope(entityManager.createNativeQuery(sql), scope).executeUpdate();
    }

    private static String scopeSql(Scope scope) {
        StringBuilder sb = new StringBuilder();
        if (scope.from() != null) {
            sb.append(" AND n.created_at >= :fromDt");
        }
        if (scope.to() != null) {
            sb.append(" AND n.created_at < :toDt");
        }
        if (scope.merchantId() != null) {
            sb.append(" AND n.merchant_id = :merchantId");
        }
        return sb.toString();
    }

    private Query createQuery(String sql) {
        return entityManager.createNativeQuery(sql);
    }

    private static Query bindScope(Query q, Scope scope) {
        if (scope.from() != null) {
            q.setParameter("fromDt", scope.from());
        }
        if (scope.to() != null) {
            q.setParameter("toDt", scope.to());
        }
        if (scope.merchantId() != null) {
            q.setParameter("merchantId", scope.merchantId());
        }
        return q;
    }

    private static long scalarLong(Query q) {
        Object v = q.getSingleResult();
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String normalizeMerchantId(String merchantId) {
        if (merchantId == null) {
            return null;
        }
        String t = merchantId.trim();
        return t.isEmpty() ? null : t;
    }

    private record Scope(ZoneId zone,
                         LocalDate fromDate,
                         LocalDate toDate,
                         LocalDateTime from,
                         LocalDateTime to,
                         String merchantId) {
    }
}
