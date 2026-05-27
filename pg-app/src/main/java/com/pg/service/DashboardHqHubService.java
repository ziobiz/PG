package com.pg.service;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementRun;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.service.settlement.SettlementRunDateDisplayService;
import com.pg.util.DashboardTupleRows;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 총본사(HEADQUARTERS)·ADMIN 메인용 운영 허브 집계. orgAccess와 동일 가맹 범위(비관리자)로 거래·정산 요약.
 */
@Service
public class DashboardHqHubService {

    private final OrgUnitRepository orgUnitRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final SettlementRunDateDisplayService settlementRunDateDisplayService;

    public DashboardHqHubService(OrgUnitRepository orgUnitRepository,
                                 PgTrnsctnRepository pgTrnsctnRepository,
                                 SettlementRunRepository settlementRunRepository,
                                 SettlementRunDateDisplayService settlementRunDateDisplayService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.settlementRunDateDisplayService = settlementRunDateDisplayService;
    }

    public Map<String, Object> build(boolean admin,
                                     boolean unrestricted,
                                     Set<String> merchantMids,
                                     boolean emptyScope,
                                     Long hqRootOrgUnitId,
                                     LocalDate today) {
        Map<String, Object> hub = new LinkedHashMap<>();
        hub.put("variant", "HEADQUARTERS_HUB");
        hub.put("title", "DASHBOARD");

        List<OrgUnit> allOrgs = orgUnitRepository.findAll();
        Set<Long> scopeOrgIds = resolveScopeOrgIds(admin, hqRootOrgUnitId, allOrgs);
        Map<String, Long> levelCounts = countByOrgLevel(allOrgs, scopeOrgIds);
        hub.put("orgUnitsByLevel", levelCounts);
        hub.put("merchantOrgCount", levelCounts.getOrDefault(OrgLevel.MERCHANT.name(), 0L));
        hub.put("regionalOrgCount", levelCounts.getOrDefault(OrgLevel.REGIONAL.name(), 0L));
        hub.put("masterDistOrgCount", levelCounts.getOrDefault(OrgLevel.MASTER_DIST.name(), 0L));

        hub.put("tiles", buildTiles());

        if (emptyScope && !admin) {
            hub.put("revenueTrend7d", Collections.emptyList());
            hub.put("statusMix30d", Map.of());
            hub.put("recentSettlements", Collections.emptyList());
            hub.put("headline", Map.of(
                    "approveAmt7d", BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP),
                    "approveCnt7d", 0L,
                    "txnTotal7d", 0L
            ));
            hub.put("note", "허용 가맹 범위가 없어 거래·정산 요약을 생략했습니다.");
            return hub;
        }

        LocalDateTime t7Start = today.minusDays(6).atStartOfDay();
        LocalDateTime t7End = today.plusDays(1).atStartOfDay();
        List<Map<String, Object>> trend = new ArrayList<>();
        BigDecimal sum7 = BigDecimal.ZERO;
        long appr7 = 0;
        long tot7 = 0;
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Object> dayAgg = aggregateOneDay(unrestricted, merchantMids, d);
            trend.add(Map.of(
                    "date", d.toString(),
                    "amtApprovedSum", dayAgg.get("amtApprovedSum"),
                    "txnApproved", dayAgg.get("txnApproved"),
                    "txnTotal", dayAgg.get("txnTotal")
            ));
            sum7 = sum7.add((BigDecimal) dayAgg.get("amtApprovedSum"));
            appr7 += ((Number) dayAgg.get("txnApproved")).longValue();
            tot7 += ((Number) dayAgg.get("txnTotal")).longValue();
        }
        hub.put("revenueTrend7d", trend);
        hub.put("headline", Map.of(
                "approveAmt7d", sum7.setScale(0, RoundingMode.HALF_UP),
                "approveCnt7d", appr7,
                "txnTotal7d", tot7
        ));

        LocalDateTime m30 = today.minusDays(29).atStartOfDay();
        LocalDateTime tNext = today.plusDays(1).atStartOfDay();
        Object rawMix = unrestricted
                ? pgTrnsctnRepository.dashboardRiskBucketsAll(m30, tNext)
                : pgTrnsctnRepository.dashboardRiskBucketsMerchants(m30, tNext, merchantMids);
        hub.put("statusMix30d", riskRowToMap(DashboardTupleRows.normalizeRow(rawMix)));

        LocalDateTime since = today.minusDays(21).atStartOfDay();
        var page = PageRequest.of(0, 18);
        List<SettlementRun> runs = unrestricted
                ? settlementRunRepository.findRecentForTimelineAll(since, page)
                : settlementRunRepository.findRecentForTimelineIn(since, merchantMids, page);
        List<Map<String, Object>> runRows = new ArrayList<>(runs.size());
        for (SettlementRun r : runs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : null);
            settlementRunDateDisplayService.enrichCloseAndExecDates(m, r);
            m.put("merchantIdMasked", maskMid(r.getMerchantId()));
            m.put("payAmt", r.getPayAmt());
            m.put("settlementPublishSts", r.getSettlementPublishSts());
            m.put("payoutHoldYn", r.getPayoutHoldYn());
            runRows.add(m);
        }
        hub.put("recentSettlements", runRows);

        hub.put("note", "");
        return hub;
    }

    /**
     * 허브 집계 중 예외 시 타일·제목은 유지하고 수치·추이만 비운다.
     */
    public Map<String, Object> buildDegraded(boolean admin, Throwable error) {
        Map<String, Object> hub = new LinkedHashMap<>();
        hub.put("variant", "HEADQUARTERS_HUB");
        hub.put("title", "DASHBOARD");
        String msg = error != null && error.getMessage() != null ? error.getMessage() : String.valueOf(error);
        if (msg.length() > 400) {
            msg = msg.substring(0, 400) + "…";
        }
        hub.put("note", "DASHBOARD 집계 중 오류: " + msg);
        hub.put("tiles", buildTiles());
        hub.put("orgUnitsByLevel", Map.of());
        hub.put("merchantOrgCount", 0L);
        hub.put("regionalOrgCount", 0L);
        hub.put("masterDistOrgCount", 0L);
        hub.put("revenueTrend7d", Collections.emptyList());
        hub.put("statusMix30d", Map.of("fail", 0L, "voidFamily", 0L, "refund", 0L, "cancel", 0L));
        hub.put("recentSettlements", Collections.emptyList());
        hub.put("headline", Map.of(
                "approveAmt7d", BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP),
                "approveCnt7d", 0L,
                "txnTotal7d", 0L));
        return hub;
    }

    private static List<Map<String, Object>> buildTiles() {
        List<Map<String, Object>> tiles = new ArrayList<>();
        addTile(tiles, "/hq/serverManage", "서버 운영", "호스트·SSL·디스크·DB 요약", "bi-hdd-stack", true);
        addTile(tiles, "/hq/settlementAdmin", "정산 관리설정", "주기·보류·환수 정책", "bi-calendar-week", true);
        addTile(tiles, "/hq/notifyInbound", "노티 수신", "미매핑·재전송 점검", "bi-bell", true);
        addTile(tiles, "/calc/payList", "결제 내역", "승인·환불·무효 필터", "bi-credit-card", true);
        addTile(tiles, "/calc/exCalcList", "정산 실행", "비자동 가맹만 [수동실행]", "bi-play-circle", true);
        addTile(tiles, "/calc/calcList", "유통망 정산", "단계별 정산 내역", "bi-diagram-3", true);
        addTile(tiles, "/calc/calcGmList", "가맹점 정산", "가맹 지급·보류", "bi-shop", true);
        addTile(tiles, "/calc/unpaidMng", "미수금", "잔액·환수", "bi-cash-coin", true);
        addTile(tiles, "/comp/compMngTree", "업체 트리", "조직·가맹 구조", "bi-diagram-2", true);
        addTile(tiles, "/commission/commisionList", "수수료", "요율·배분", "bi-percent", true);
        addTile(tiles, "/hq/pgApiMng", "PG사 연동", "API·MID", "bi-plug", true);
        addTile(tiles, "/hq/domainConfig", "도메인·포털", "호스트·브랜딩", "bi-globe2", true);
        return tiles;
    }

    private static void addTile(List<Map<String, Object>> tiles, String url, String title, String subtitle,
                                String icon, boolean include) {
        if (!include) {
            return;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("url", url);
        m.put("title", title);
        m.put("subtitle", subtitle);
        m.put("icon", icon);
        tiles.add(m);
    }

    private Map<String, Object> aggregateOneDay(boolean unrestricted, Set<String> mids, LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        Object raw = unrestricted
                ? pgTrnsctnRepository.dashboardAggregateAll(start, end)
                : pgTrnsctnRepository.dashboardAggregateForMerchants(start, end, mids);
        Object[] row = DashboardTupleRows.normalizeRow(raw);
        long total = DashboardTupleRows.readLong(row != null && row.length > 0 ? row[0] : null);
        long appr = DashboardTupleRows.readLong(row != null && row.length > 1 ? row[1] : null);
        BigDecimal sum = DashboardTupleRows.readDecimal(row != null && row.length > 2 ? row[2] : null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("txnTotal", total);
        m.put("txnApproved", appr);
        m.put("amtApprovedSum", sum.setScale(0, RoundingMode.HALF_UP));
        return m;
    }

    private static Map<String, Long> riskRowToMap(Object[] row) {
        Map<String, Long> m = new LinkedHashMap<>();
        if (row == null || row.length < 4) {
            m.put("fail", 0L);
            m.put("voidFamily", 0L);
            m.put("refund", 0L);
            m.put("cancel", 0L);
            return m;
        }
        m.put("fail", n(row[0]));
        m.put("voidFamily", n(row[1]));
        m.put("refund", n(row[2]));
        m.put("cancel", n(row[3]));
        return m;
    }

    private static long n(Object o) {
        return DashboardTupleRows.readLong(o);
    }

    private static String maskMid(String mid) {
        if (mid == null || mid.isBlank()) {
            return "—";
        }
        String t = mid.trim();
        if (t.length() <= 4) {
            return "****";
        }
        return "****" + t.substring(t.length() - 4);
    }

    /** ADMIN: 전체 조직. 총본사: 루트 및 하위 id 집합 */
    private static Set<Long> resolveScopeOrgIds(boolean admin, Long hqRootOrgUnitId, List<OrgUnit> allOrgs) {
        if (admin) {
            return allOrgs.stream().map(OrgUnit::getId).collect(Collectors.toCollection(HashSet::new));
        }
        if (hqRootOrgUnitId == null) {
            return Collections.emptySet();
        }
        Set<Long> out = new HashSet<>();
        out.add(hqRootOrgUnitId);
        Map<Long, List<OrgUnit>> byParent = allOrgs.stream()
                .filter(o -> o.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        collectDesc(hqRootOrgUnitId, byParent, out);
        return out;
    }

    private static void collectDesc(Long id, Map<Long, List<OrgUnit>> byParent, Set<Long> out) {
        for (OrgUnit ch : byParent.getOrDefault(id, Collections.emptyList())) {
            out.add(ch.getId());
            collectDesc(ch.getId(), byParent, out);
        }
    }

    private static Map<String, Long> countByOrgLevel(List<OrgUnit> all, Set<Long> allowedIds) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (OrgLevel lv : OrgLevel.values()) {
            m.put(lv.name(), 0L);
        }
        for (OrgUnit o : all) {
            if (!allowedIds.contains(o.getId())) {
                continue;
            }
            if (o.getOrgLevel() == null) {
                continue;
            }
            String k = o.getOrgLevel().name();
            m.merge(k, 1L, Long::sum);
        }
        return m;
    }
}
