package com.pg.service;

import com.pg.entity.MerchantReceivable;
import com.pg.entity.PgNotifyInbound;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.repository.PgNotifyInboundRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.util.DashboardTupleRows;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 메인 대시보드 인사이트(1단계: 규칙·집계만). 숫자·근거는 서버 생성, LLM 없음.
 */
@Service
public class DashboardInsightsService {

    private static final double W_FAIL = 5.0;
    private static final double W_VOID = 3.0;
    private static final double W_REFUND = 4.0;
    private static final double W_CANCEL = 1.0;
    private static final double SCORE_SCALE = 14.0;

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final PgNotifyInboundRepository pgNotifyInboundRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final SettlementSettingRepository settlementSettingRepository;

    public DashboardInsightsService(PgTrnsctnRepository pgTrnsctnRepository,
                                    MerchantReceivableRepository merchantReceivableRepository,
                                    PgNotifyInboundRepository pgNotifyInboundRepository,
                                    SettlementRunRepository settlementRunRepository,
                                    SettlementSettingRepository settlementSettingRepository) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.pgNotifyInboundRepository = pgNotifyInboundRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.settlementSettingRepository = settlementSettingRepository;
    }

    public Map<String, Object> build(boolean unrestricted,
                                     Set<String> merchantScopeNonNull,
                                     boolean emptyScope,
                                     LocalDate today,
                                     String orgLevel,
                                     String compId,
                                     Long orgUnitId) {
        Map<String, Object> root = new LinkedHashMap<>();
        String lvl = orgLevel != null ? orgLevel.trim().toUpperCase(Locale.ROOT) : "";

        LocalDateTime weekThisStart = today.minusDays(6).atStartOfDay();
        LocalDateTime weekNextStart = today.plusDays(1).atStartOfDay();
        LocalDateTime weekPrevStart = today.minusDays(13).atStartOfDay();
        LocalDateTime weekPrevEnd = today.minusDays(6).atStartOfDay();

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("insightEngine", "RULE_V1");
        evidence.put("txnTimeField", "COALESCE(paid_at, created_at)");
        evidence.put("scope", unrestricted ? "ALL" : "ORG_MERCHANT_CODES");
        evidence.put("rolling7dThis", today.minusDays(6) + " ~ " + today);
        evidence.put("rolling7dPrev", today.minusDays(13) + " ~ " + today.minusDays(7));
        root.put("evidenceBase", evidence);

        if (emptyScope) {
            root.put("riskScorecard", emptyScorecard());
            root.put("kpiStrip", emptyKpi());
            root.put("kpiStripYesterday", emptyKpiYesterday());
            root.put("timeline", List.of());
            root.put("priorityQueue", List.of());
            root.put("anomalies", List.of());
            root.put("payoutOutlook", null);
            root.put("explainers", defaultExplainers());
            root.put("ruleNarrative", "허용된 가맹 범위가 없어 인사이트 집계를 생략했습니다.");
            return root;
        }

        RiskBuckets bThis = loadRiskBuckets(unrestricted, merchantScopeNonNull, weekThisStart, weekNextStart);
        RiskBuckets bPrev = loadRiskBuckets(unrestricted, merchantScopeNonNull, weekPrevStart, weekPrevEnd);

        int scoreThis = scoreFromBuckets(bThis);
        int scorePrev = scoreFromBuckets(bPrev);
        Map<String, Object> scorecard = new LinkedHashMap<>();
        scorecard.put("score", scoreThis);
        scorecard.put("scorePrevWeek", scorePrev);
        scorecard.put("deltaVsPrevWeek", scoreThis - scorePrev);
        scorecard.put("weights", Map.of(
                "fail", W_FAIL,
                "voidFamily", W_VOID,
                "refund", W_REFUND,
                "cancel", W_CANCEL,
                "formula", "min(100, round(SCALE * ln(1 + wF*fail + wV*void + wR*refund + wC*cancel)))"
        ));
        scorecard.put("componentsThisWeek", bThis.toMap());
        scorecard.put("componentsPrevWeek", bPrev.toMap());
        root.put("riskScorecard", scorecard);

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        RiskBuckets bToday = loadRiskBuckets(unrestricted, merchantScopeNonNull, dayStart, weekNextStart);
        RiskBuckets bYesterday = loadRiskBuckets(unrestricted, merchantScopeNonNull, yesterdayStart, dayStart);
        Map<String, Object> todayTxn = loadTxnAgg(unrestricted, merchantScopeNonNull, dayStart, weekNextStart);
        Map<String, Object> yesterdayTxn = loadTxnAgg(unrestricted, merchantScopeNonNull, yesterdayStart, dayStart);
        List<Map<String, Object>> todayByCur = loadApprovedByCurrency(unrestricted, merchantScopeNonNull, dayStart, weekNextStart);
        List<Map<String, Object>> yesterdayByCur = loadApprovedByCurrency(unrestricted, merchantScopeNonNull, yesterdayStart, dayStart);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("todayTxnTotal", todayTxn.get("txnTotal"));
        kpi.put("todayTxnApproved", todayTxn.get("txnApproved"));
        kpi.put("todayApprovedByCurrency", todayByCur);
        kpi.put("todayFailures", bToday.fail);
        kpi.put("todayVoids", bToday.voidFamily);
        kpi.put("todayRefunds", bToday.refund);
        kpi.put("todayCancels", bToday.cancel);

        Map<String, Object> kpiY = new LinkedHashMap<>();
        kpiY.put("yesterdayTxnTotal", yesterdayTxn.get("txnTotal"));
        kpiY.put("yesterdayTxnApproved", yesterdayTxn.get("txnApproved"));
        kpiY.put("yesterdayApprovedByCurrency", yesterdayByCur);
        kpiY.put("yesterdayFailures", bYesterday.fail);
        kpiY.put("yesterdayVoids", bYesterday.voidFamily);
        kpiY.put("yesterdayRefunds", bYesterday.refund);
        kpiY.put("yesterdayCancels", bYesterday.cancel);
        root.put("kpiStripYesterday", kpiY);

        Object[] recv = pendingReceivableStats(unrestricted, merchantScopeNonNull);
        long recvCnt = DashboardTupleRows.readLong(recv != null && recv.length > 0 ? recv[0] : null);
        BigDecimal recvRem = recv != null && recv.length > 1
                ? DashboardTupleRows.readDecimal(recv[1]) : BigDecimal.ZERO;
        kpi.put("receivableOpenCount", recvCnt);
        kpi.put("receivableRemainingSum", recvRem.setScale(0, RoundingMode.HALF_UP));

        LocalDateTime notifySince = today.minusDays(7).atStartOfDay();
        long notifyBad = countNotifyNotParsed(unrestricted, merchantScopeNonNull, notifySince, weekNextStart);
        kpi.put("notifyNotParsedLast7d", notifyBad);

        LocalDate holdFrom = today.minusDays(30);
        long holds = countSettlementHold(unrestricted, merchantScopeNonNull, holdFrom);
        kpi.put("settlementHoldOrPayoutHoldRows30d", holds);
        root.put("kpiStrip", kpi);

        root.put("timeline", buildTimeline(unrestricted, merchantScopeNonNull, today));

        root.put("priorityQueue", buildPriorityQueue(scoreThis - scorePrev, recvCnt, recvRem, notifyBad, holds, lvl));

        root.put("anomalies", buildAnomalies(unrestricted, merchantScopeNonNull, weekThisStart, weekNextStart, lvl));

        root.put("payoutOutlook", buildPayoutOutlook(lvl, compId, orgUnitId));

        root.put("explainers", defaultExplainers());

        root.put("ruleNarrative", buildRuleNarrative(scoreThis, scorePrev, bThis, recvCnt, recvRem, notifyBad, holds, lvl));
        return root;
    }

    /**
     * 집계 쿼리 등 예외 시에도 응답에 {@code insights} 키·하위 구조를 유지하고 원인을 내려 UI에서 표시한다.
     */
    public Map<String, Object> buildDegraded(Throwable error) {
        Map<String, Object> root = new LinkedHashMap<>();
        String msg = error != null && error.getMessage() != null ? error.getMessage() : String.valueOf(error);
        if (msg.length() > 500) {
            msg = msg.substring(0, 500) + "…";
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("insightEngine", "ERROR");
        evidence.put("scope", "DEGRADED");
        evidence.put("error", msg);
        root.put("evidenceBase", evidence);
        root.put("riskScorecard", emptyScorecard());
        root.put("kpiStrip", emptyKpi());
        root.put("kpiStripYesterday", emptyKpiYesterday());
        root.put("timeline", List.of());
        root.put("priorityQueue", List.of());
        root.put("anomalies", List.of());
        root.put("payoutOutlook", null);
        root.put("explainers", defaultExplainers());
        root.put("ruleNarrative", "인사이트 집계 중 오류가 발생했습니다. DB 스키마·연결·서버 로그를 확인하세요.");
        root.put("loadError", msg.length() > 320 ? msg.substring(0, 320) + "…" : msg);
        return root;
    }

    private static Map<String, Object> emptyScorecard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", 0);
        m.put("scorePrevWeek", 0);
        m.put("deltaVsPrevWeek", 0);
        m.put("weights", Map.of("fail", W_FAIL, "voidFamily", W_VOID, "refund", W_REFUND, "cancel", W_CANCEL, "formula", ""));
        m.put("componentsThisWeek", RiskBuckets.empty().toMap());
        m.put("componentsPrevWeek", RiskBuckets.empty().toMap());
        return m;
    }

    private static Map<String, Object> emptyKpi() {
        Map<String, Object> k = new LinkedHashMap<>();
        k.put("todayTxnTotal", 0L);
        k.put("todayTxnApproved", 0L);
        k.put("todayApprovedByCurrency", List.of());
        k.put("todayFailures", 0L);
        k.put("todayVoids", 0L);
        k.put("todayRefunds", 0L);
        k.put("todayCancels", 0L);
        k.put("receivableOpenCount", 0L);
        k.put("receivableRemainingSum", BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP));
        k.put("notifyNotParsedLast7d", 0L);
        k.put("settlementHoldOrPayoutHoldRows30d", 0L);
        return k;
    }

    private static Map<String, Object> emptyKpiYesterday() {
        Map<String, Object> k = new LinkedHashMap<>();
        k.put("yesterdayTxnTotal", 0L);
        k.put("yesterdayTxnApproved", 0L);
        k.put("yesterdayApprovedByCurrency", List.of());
        k.put("yesterdayFailures", 0L);
        k.put("yesterdayVoids", 0L);
        k.put("yesterdayRefunds", 0L);
        k.put("yesterdayCancels", 0L);
        return k;
    }

    private static Map<String, String> defaultExplainers() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("riskScore", "최근 7일(오늘 포함) 실패·무효·환불·취소 건수에 가중치를 둔 규칙 점수입니다. 지난 7일 대비 증감은 동일 규칙으로 비교합니다.");
        m.put("kpiStrip", "오늘 0시 이후 결제일시 기준 성공(상태 10) 건수·통화별 승인금액 합, 전체 거래 건수, 실패·무효·환불·취소 건수이며, 미수(PENDING)·노티 미매핑·정산보류/지급보류 행 수가 함께 포함됩니다.");
        m.put("kpiStripYesterday", "전일 0시~24시(당일 0시 직전) 결제일시 기준 성공(상태 10) 건수·통화별 승인금액 합, 전체 거래 건수, 실패·무효·환불·취소 건수입니다. 미수·노티 등은 시점 스냅샷이 없어 제외합니다.");
        m.put("timeline", "정산 실행 생성·미수금 생성·노티 미처리(매핑 외) 중 최근 이벤트입니다.");
        m.put("priorityQueue", "규칙으로 정렬한 오늘 확인 권장 항목이며, 클릭 시 관리 화면으로 이동합니다.");
        m.put("anomalies", "지난 7일 환불·강제환불 건수 상위 가맹(식별자 마스킹)입니다.");
        m.put("payoutOutlook", "최근 정산 실행 지급액으로 참고 구간만 제시합니다. 실제 지급은 보류·환수·정책에 따라 달라질 수 있습니다.");
        return m;
    }

    private RiskBuckets loadRiskBuckets(boolean unrestricted, Set<String> mids, LocalDateTime from, LocalDateTime toEx) {
        Object raw = unrestricted
                ? pgTrnsctnRepository.dashboardRiskBucketsAll(from, toEx)
                : pgTrnsctnRepository.dashboardRiskBucketsMerchants(from, toEx, mids);
        Object[] row = DashboardTupleRows.normalizeRow(raw);
        return RiskBuckets.fromRow(row);
    }

    /** {@link PgTrnsctnRepository#dashboardAggregateAll} 와 동일: 결제일시 COALESCE(paid_at, created_at), 상한 toExclusive 미포함. */
    private Map<String, Object> loadTxnAgg(boolean unrestricted, Set<String> mids, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        Object raw = unrestricted
                ? pgTrnsctnRepository.dashboardAggregateAll(fromInclusive, toExclusive)
                : pgTrnsctnRepository.dashboardAggregateForMerchants(fromInclusive, toExclusive, mids);
        Object[] row = DashboardTupleRows.normalizeRow(raw);
        long total = DashboardTupleRows.readLong(row != null && row.length > 0 ? row[0] : null);
        long appr = DashboardTupleRows.readLong(row != null && row.length > 1 ? row[1] : null);
        BigDecimal sum = DashboardTupleRows.readDecimal(row != null && row.length > 2 ? row[2] : null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("txnTotal", total);
        m.put("txnApproved", appr);
        m.put("amtApprovedSum", sum.setScale(2, RoundingMode.HALF_UP));
        return m;
    }

    /**
     * 성공(승인)만 통화별로 분해. {@link PgTrnsctnRepository#dashboardAggregateByCurrencyAll} — 청구 통화(curType) 기준, 승인 건이 있는 통화만 포함.
     */
    private List<Map<String, Object>> loadApprovedByCurrency(boolean unrestricted, Set<String> mids,
                                                             LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        if (!unrestricted && (mids == null || mids.isEmpty())) {
            return List.of();
        }
        List<Object[]> rawRows = unrestricted
                ? pgTrnsctnRepository.dashboardAggregateByCurrencyAll(fromInclusive, toExclusive)
                : pgTrnsctnRepository.dashboardAggregateByCurrencyForMerchants(fromInclusive, toExclusive, mids);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] raw : rawRows) {
            Object[] row = DashboardTupleRows.normalizeRow(raw);
            if (row == null || row.length < 4) {
                continue;
            }
            String cur = row[0] != null ? row[0].toString().trim() : "";
            if (cur.isEmpty()) {
                cur = "KRW";
            } else {
                cur = cur.toUpperCase(Locale.ROOT);
            }
            long appr = DashboardTupleRows.readLong(row[2]);
            BigDecimal amt = DashboardTupleRows.readDecimal(row[3]);
            if (appr <= 0 && amt.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("currency", cur);
            line.put("txnApproved", appr);
            line.put("amtApprovedSum", amt.setScale(8, RoundingMode.HALF_UP));
            out.add(line);
        }
        out.sort(Comparator.comparing(m -> String.valueOf(m.get("currency"))));
        return out;
    }

    private Object[] pendingReceivableStats(boolean unrestricted, Set<String> mids) {
        Object raw = unrestricted
                ? merchantReceivableRepository.dashboardPendingReceivableAll()
                : merchantReceivableRepository.dashboardPendingReceivableIn(mids);
        return DashboardTupleRows.normalizeRow(raw);
    }

    private long countNotifyNotParsed(boolean unrestricted, Set<String> mids, LocalDateTime from, LocalDateTime toEx) {
        if (unrestricted) {
            return pgNotifyInboundRepository.countNotParsedBetweenAll(from, toEx);
        }
        return pgNotifyInboundRepository.countNotParsedBetweenIn(mids, from, toEx);
    }

    private long countSettlementHold(boolean unrestricted, Set<String> mids, LocalDate from) {
        if (unrestricted) {
            return settlementRunRepository.countHoldOrPayoutHoldSinceAll(from);
        }
        return settlementRunRepository.countHoldOrPayoutHoldSinceIn(from, mids);
    }

    private static int scoreFromBuckets(RiskBuckets b) {
        double raw = b.fail * W_FAIL + b.voidFamily * W_VOID + b.refund * W_REFUND + b.cancel * W_CANCEL;
        return (int) Math.min(100, Math.round(SCORE_SCALE * Math.log1p(raw)));
    }

    private List<Map<String, Object>> buildTimeline(boolean unrestricted, Set<String> mids, LocalDate today) {
        LocalDateTime since = today.minusDays(14).atStartOfDay();
        var page = PageRequest.of(0, 8);
        List<SettlementRun> runs = unrestricted
                ? settlementRunRepository.findRecentForTimelineAll(since, page)
                : settlementRunRepository.findRecentForTimelineIn(since, mids, page);
        List<MerchantReceivable> recv = unrestricted
                ? merchantReceivableRepository.findRecentCreatedAll(since, page)
                : merchantReceivableRepository.findRecentCreatedIn(since, mids, page);
        List<PgNotifyInbound> bad = unrestricted
                ? pgNotifyInboundRepository.findRecentNotParsedAll(since, page)
                : pgNotifyInboundRepository.findRecentNotParsedIn(since, mids, page);

        List<Map<String, Object>> items = new ArrayList<>();
        for (SettlementRun r : runs) {
            if (r.getCreatedAt() == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("at", r.getCreatedAt().toString());
            m.put("kind", "SETTLEMENT_RUN");
            m.put("title", "정산 실행");
            m.put("detail", "정산일 " + r.getCalcDt() + " · 가맹 " + maskMerchantId(r.getMerchantId())
                    + " · 지급 " + nz(r.getPayAmt()));
            m.put("refUrl", "/calc/exCalcList");
            items.add(m);
        }
        for (MerchantReceivable r : recv) {
            if (r.getCreatedAt() == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("at", r.getCreatedAt().toString());
            m.put("kind", "RECEIVABLE");
            m.put("title", "미수금");
            m.put("detail", maskMerchantId(r.getMerchantId()) + " · 잔액 " + nz(r.getRemainingAmount()) + " · " + nzStr(r.getTitle()));
            m.put("refUrl", "/calc/unpaidMng");
            items.add(m);
        }
        for (PgNotifyInbound n : bad) {
            if (n.getCreatedAt() == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("at", n.getCreatedAt().toString());
            m.put("kind", "NOTIFY_INBOUND");
            m.put("title", "노티 미매핑/미적재");
            m.put("detail", "상태 " + nzStr(n.getProcessStatus()) + " · " + maskMerchantId(n.getMerchantId()));
            m.put("refUrl", "/hq/notifyInbound");
            items.add(m);
        }
        items.sort(Comparator.comparing((Map<String, Object> x) -> String.valueOf(x.get("at"))).reversed());
        if (items.size() > 18) {
            return new ArrayList<>(items.subList(0, 18));
        }
        return items;
    }

    private List<Map<String, Object>> buildPriorityQueue(int riskDelta, long recvCnt, BigDecimal recvRem,
                                                        long notifyBad, long holds, String orgLevel) {
        List<Prior> list = new ArrayList<>();
        if (recvCnt > 0) {
            double extra = recvRem.compareTo(BigDecimal.ZERO) > 0
                    ? Math.min(40, recvRem.divide(BigDecimal.valueOf(5_000_000L), 2, RoundingMode.HALF_UP).doubleValue() * 8.0)
                    : 0.0;
            double sev = Math.min(100, 35 + extra);
            list.add(new Prior(sev, "미수금 잔액 " + recvCnt + "건 · 합계 약 " + recvRem.toPlainString() + " 원", "/calc/unpaidMng",
                    "PENDING_RECEIVABLE"));
        }
        if (notifyBad > 0) {
            list.add(new Prior(Math.min(100, 28 + notifyBad * 6.0), "최근 7일 노티 미매핑/미적재 " + notifyBad + "건", "/hq/notifyInbound", "NOTIFY_PARSE"));
        }
        if (holds > 0) {
            list.add(new Prior(Math.min(100, 26 + holds * 5.0), "최근 30일 정산 보류/지급보류 실행 " + holds + "건", "/calc/paySettlementHoldList", "SETTLEMENT_HOLD"));
        }
        if (riskDelta >= 8) {
            list.add(new Prior(Math.min(100, 22 + riskDelta * 1.5), "리스크 점수가 지난주 대비 +" + riskDelta + " 상승", "/calc/payList", "RISK_UP"));
        }
        if (!"MERCHANT".equals(orgLevel) && list.size() < 5) {
            list.add(new Prior(12, "환불·무효 추이를 결제내역에서 필터로 확인", "/calc/payList", "REVIEW_PAY"));
        }
        list.sort(Comparator.comparingDouble((Prior p) -> p.severity).reversed());
        List<Map<String, Object>> out = new ArrayList<>();
        int rank = 1;
        for (Prior p : list) {
            if (rank > 5) {
                break;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank);
            m.put("title", p.title);
            m.put("url", p.url);
            m.put("reasonCode", p.code);
            m.put("severityApprox", Math.round(p.severity));
            out.add(m);
            rank++;
        }
        return out;
    }

    private List<Map<String, Object>> buildAnomalies(boolean unrestricted, Set<String> mids,
                                                     LocalDateTime from, LocalDateTime toEx, String orgLevel) {
        if ("MERCHANT".equals(orgLevel)) {
            return List.of();
        }
        List<Object[]> rows = unrestricted
                ? pgTrnsctnRepository.dashboardTopRefundMerchantsAll(from, toEx, PageRequest.of(0, 3))
                : pgTrnsctnRepository.dashboardTopRefundMerchantsIn(from, toEx, mids, PageRequest.of(0, 3));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] raw : rows) {
            Object[] row = DashboardTupleRows.normalizeRow(raw);
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String mid = String.valueOf(row[0]).trim();
            long cnt = DashboardTupleRows.readLong(row[1]);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "REFUND_VOLUME_TOP");
            m.put("merchantMasked", maskMerchantId(mid));
            m.put("refundCount7d", cnt);
            m.put("message", "지난 7일 환불·강제환불 건수 상위(조직 범위 내)");
            m.put("refUrl", "/calc/payList");
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> buildPayoutOutlook(String orgLevel, String compId, Long orgUnitId) {
        if (!"MERCHANT".equals(orgLevel) || compId == null || compId.isBlank()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        List<SettlementRun> recent = settlementRunRepository.findByMerchantIdAndCalcDtBetweenOrderByCalcDtAsc(
                compId.trim(), today.minusMonths(3), today.plusDays(1));
        if (recent.isEmpty()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("calcCycle", settlementCycleLabel(orgUnitId));
            m.put("recentPayAmtMin", null);
            m.put("recentPayAmtMax", null);
            m.put("recentPayAmtMedian", null);
            m.put("disclaimer", "최근 정산 실행이 없어 지급 구간을 산출하지 못했습니다.");
            return m;
        }
        List<BigDecimal> pays = new ArrayList<>();
        int take = Math.min(3, recent.size());
        for (int i = recent.size() - take; i < recent.size(); i++) {
            SettlementRun r = recent.get(i);
            if (r.getPayAmt() != null) {
                pays.add(r.getPayAmt());
            }
        }
        if (pays.isEmpty()) {
            return Map.of("calcCycle", settlementCycleLabel(orgUnitId),
                    "disclaimer", "지급액 데이터가 없습니다.");
        }
        BigDecimal min = pays.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal max = pays.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal med = median(pays);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("calcCycle", settlementCycleLabel(orgUnitId));
        m.put("recentPayAmtMin", min.setScale(0, RoundingMode.HALF_UP));
        m.put("recentPayAmtMax", max.setScale(0, RoundingMode.HALF_UP));
        m.put("recentPayAmtMedian", med.setScale(0, RoundingMode.HALF_UP));
        m.put("nextSettlementHint", "다음 정산 실행 일시는 정산주기(" + settlementCycleLabel(orgUnitId) + ") 및 정산실행 배치 기준입니다.");
        m.put("disclaimer", "최근 3회 지급액의 최소·최대·중앙값으로 참고 구간만 표시합니다. 보류·환수·수수료는 실행별로 다릅니다.");
        return m;
    }

    private String settlementCycleLabel(Long orgUnitId) {
        if (orgUnitId == null) {
            return "—";
        }
        return settlementSettingRepository.findByOrgUnitId(orgUnitId)
                .map(SettlementSetting::getCalcCycle)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse("—");
    }

    private static BigDecimal median(List<BigDecimal> vals) {
        List<BigDecimal> copy = new ArrayList<>(vals);
        copy.sort(Comparator.naturalOrder());
        int n = copy.size();
        if (n == 0) {
            return BigDecimal.ZERO;
        }
        if (n % 2 == 1) {
            return copy.get(n / 2);
        }
        return copy.get(n / 2 - 1).add(copy.get(n / 2)).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
    }

    private static String buildRuleNarrative(int scoreThis, int scorePrev, RiskBuckets bThis,
                                             long recvCnt, BigDecimal recvRem, long notifyBad, long holds, String orgLevel) {
        StringBuilder sb = new StringBuilder();
        int dScore = scoreThis - scorePrev;
        sb.append("최근 7일 리스크 점수는 ").append(scoreThis).append("점이며, 직전 7일 대비 ")
                .append(dScore > 0 ? "+" : "").append(dScore).append("입니다. ");
        sb.append("구성은 실패 ").append(bThis.fail).append("·무효계열 ").append(bThis.voidFamily)
                .append("·환불 ").append(bThis.refund).append("·취소 ").append(bThis.cancel).append("건입니다. ");
        if (recvCnt > 0) {
            sb.append("미수금 잔액이 ").append(recvRem.toPlainString()).append(" 원 남아 있습니다. ");
        }
        if (notifyBad > 0) {
            sb.append("노티 미매핑/미적재가 7일간 ").append(notifyBad).append("건입니다. ");
        }
        if (holds > 0) {
            sb.append("정산 보류/지급보류 실행이 30일 내 ").append(holds).append("건 있습니다. ");
        }
        if ("MERCHANT".equals(orgLevel)) {
            sb.append("가맹 화면에서는 본인 정산·미수·노티만 집계됩니다.");
        }
        return sb.toString().trim();
    }

    private static String maskMerchantId(String mid) {
        if (mid == null || mid.isBlank()) {
            return "—";
        }
        String t = mid.trim();
        if (t.length() <= 4) {
            return "****";
        }
        return "****" + t.substring(t.length() - 4);
    }

    private static String nz(BigDecimal v) {
        return v == null ? "0" : v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nzStr(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    private record Prior(double severity, String title, String url, String code) {}

    private static final class RiskBuckets {
        final long fail;
        final long voidFamily;
        final long refund;
        final long cancel;

        private RiskBuckets(long fail, long voidFamily, long refund, long cancel) {
            this.fail = fail;
            this.voidFamily = voidFamily;
            this.refund = refund;
            this.cancel = cancel;
        }

        static RiskBuckets empty() {
            return new RiskBuckets(0, 0, 0, 0);
        }

        static RiskBuckets fromRow(Object[] row) {
            if (row == null || row.length < 4) {
                return empty();
            }
            return new RiskBuckets(n(row[0]), n(row[1]), n(row[2]), n(row[3]));
        }

        private static long n(Object o) {
            return DashboardTupleRows.readLong(o);
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fail", fail);
            m.put("voidFamily", voidFamily);
            m.put("refund", refund);
            m.put("cancel", cancel);
            return m;
        }
    }
}
