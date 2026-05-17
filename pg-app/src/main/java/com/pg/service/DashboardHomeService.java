package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementRun;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.util.DashboardCurrencyAggregate;
import com.pg.util.DashboardTupleRows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 로그인 후 메인(/main)용 조직별 요약: 거래 집계, (권한 시) 서버 트래픽 요약, 가맹점 정산 실행 달력 데이터.
 */
@Service
public class DashboardHomeService {

    private static final Logger log = LoggerFactory.getLogger(DashboardHomeService.class);

    /** {@link #buildHome}·{@link #buildExtensionsOnly} 공통: 인증·조직·가맹 범위·집계일 */
    private record DashboardAuthSlice(
            AppUser user,
            String orgLevel,
            String compId,
            String compNm,
            Long orgUnitId,
            Set<String> merchantScope,
            boolean admin,
            boolean unrestricted,
            boolean emptyScope,
            LocalDate today,
            Set<String> merchantsForQuery) {
    }

    private final AuthService authService;
    private final OrgAccessService orgAccessService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final ServerUsageService serverUsageService;
    private final DashboardInsightsService dashboardInsightsService;
    private final DashboardHqHubService dashboardHqHubService;
    private final DashboardBusinessDayCalendarService dashboardBusinessDayCalendarService;

    public DashboardHomeService(AuthService authService,
                                OrgAccessService orgAccessService,
                                PgTrnsctnRepository pgTrnsctnRepository,
                                SettlementRunRepository settlementRunRepository,
                                ServerUsageService serverUsageService,
                                DashboardInsightsService dashboardInsightsService,
                                DashboardHqHubService dashboardHqHubService,
                                DashboardBusinessDayCalendarService dashboardBusinessDayCalendarService) {
        this.authService = authService;
        this.orgAccessService = orgAccessService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.serverUsageService = serverUsageService;
        this.dashboardInsightsService = dashboardInsightsService;
        this.dashboardHqHubService = dashboardHqHubService;
        this.dashboardBusinessDayCalendarService = dashboardBusinessDayCalendarService;
    }

    public Map<String, Object> buildHome(Authentication authentication) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<DashboardAuthSlice> sliceOpt = resolveAuthSlice(authentication);
        if (sliceOpt.isEmpty()) {
            out.put("ok", false);
            out.put("message", "로그인이 필요합니다.");
            return out;
        }
        DashboardAuthSlice s = sliceOpt.get();
        AppUser user = s.user();
        String orgLevel = s.orgLevel();
        String compId = s.compId();
        String compNm = s.compNm();
        Long orgUnitId = s.orgUnitId();
        Set<String> merchantScope = s.merchantScope();
        boolean admin = s.admin();
        boolean unrestricted = s.unrestricted();
        boolean emptyScope = s.emptyScope();
        LocalDate today = s.today();
        Set<String> merchantsForQuery = s.merchantsForQuery();

        out.put("orgUnitId", orgUnitId);
        out.put("ok", true);
        out.put("role", user.getRole());
        out.put("orgLevel", orgLevel.isEmpty() ? null : orgLevel);
        out.put("compId", compId.isEmpty() ? null : compId);
        out.put("compNm", compNm.isEmpty() ? null : compNm);
        out.put("userNm", user.getName() != null ? user.getName() : user.getUsername());

        out.put("asOfDate", today.toString());
        out.put("merchantScopeCount", (unrestricted || merchantScope == null) ? null : merchantScope.size());

        out.put("sales", Map.of(
                "today", emptyScope ? zeros() : aggregatePeriod(unrestricted, merchantsForQuery, today, today),
                "last7d", emptyScope ? zeros() : aggregatePeriod(unrestricted, merchantsForQuery, today.minusDays(6), today),
                "last30d", emptyScope ? zeros() : aggregatePeriod(unrestricted, merchantsForQuery, today.minusDays(29), today)
        ));
        out.put("salesByCurrency", Map.of(
                "today", emptyScope ? Collections.emptyList() : aggregateByCurrency(unrestricted, merchantsForQuery, today, today),
                "last7d", emptyScope ? Collections.emptyList() : aggregateByCurrency(unrestricted, merchantsForQuery, today.minusDays(6), today),
                "last30d", emptyScope ? Collections.emptyList() : aggregateByCurrency(unrestricted, merchantsForQuery, today.minusDays(29), today)
        ));

        out.put("quickLinks", buildQuickLinks(user.getRole(), orgLevel));

        if (canViewServerUsageSummary(user.getRole(), orgLevel)) {
            try {
                Map<String, Object> usage = serverUsageService.buildUsageReport("daily");
                Object summary = usage != null ? usage.get("summary") : null;
                out.put("serverUsageSummary", summary);
            } catch (Exception e) {
                out.put("serverUsageSummary", null);
                out.put("serverUsageSummaryError", e.getMessage() != null ? e.getMessage() : "error");
            }
        } else {
            out.put("serverUsageSummary", null);
        }

        if ("MERCHANT".equals(orgLevel) && !compId.isEmpty()) {
            LocalDate from = today.minusMonths(2);
            LocalDate to = today.plusMonths(1);
            List<SettlementRun> runs = settlementRunRepository.findByMerchantIdAndCalcDtBetweenOrderByCalcDtAsc(compId, from, to);
            List<Map<String, Object>> events = new ArrayList<>(runs.size());
            for (SettlementRun r : runs) {
                events.add(settlementRunToMap(r));
            }
            out.put("settlementCalendar", Map.of(
                    "from", from.toString(),
                    "to", to.toString(),
                    "events", events
            ));
        } else {
            out.put("settlementCalendar", null);
        }

        out.put("insightHint", buildInsightHint(orgLevel, admin, emptyScope));
        putBusinessDayCalendar(out, s);
        putInsightsAndHqHub(out, s);
        return out;
    }

    /**
     * 메인 영업일 3개월(지난달·당월·다음달). anchor는 당월(기준일의 연월).
     */
    private void putBusinessDayCalendar(Map<String, Object> out, DashboardAuthSlice s) {
        try {
            Map<String, Object> cal = dashboardBusinessDayCalendarService.build(
                    s.user().getRole(),
                    s.orgLevel(),
                    s.orgUnitId(),
                    null);
            out.put("businessDayCalendar", cal);
        } catch (Exception ex) {
            log.warn("dashboard businessDayCalendar build failed for user={}", s.user().getUsername(), ex);
            out.put("businessDayCalendar", null);
        }
    }

    public Map<String, Object> buildBusinessDayCalendar(Authentication authentication, String anchorMonth) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<DashboardAuthSlice> sliceOpt = resolveAuthSlice(authentication);
        if (sliceOpt.isEmpty()) {
            out.put("ok", false);
            out.put("message", "로그인이 필요합니다.");
            return out;
        }
        DashboardAuthSlice s = sliceOpt.get();
        if (!dashboardBusinessDayCalendarService.isEligible(s.user().getRole(), s.orgLevel())) {
            out.put("ok", false);
            out.put("message", "영업일 달력을 볼 수 있는 조직이 아닙니다.");
            return out;
        }
        try {
            Map<String, Object> cal = dashboardBusinessDayCalendarService.build(
                    s.user().getRole(), s.orgLevel(), s.orgUnitId(), anchorMonth);
            out.put("ok", true);
            out.put("businessDayCalendar", cal);
        } catch (Exception ex) {
            log.warn("dashboard businessDayCalendar anchor={} failed", anchorMonth, ex);
            out.put("ok", false);
            out.put("message", ex.getMessage() != null ? ex.getMessage() : "영업일 달력 조회 실패");
        }
        return out;
    }

    /**
     * 메인 본문이 중간에서 잘리는 환경용: 인사이트·허브만 작은 JSON으로 재전달.
     */
    public Map<String, Object> buildExtensionsOnly(Authentication authentication) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<DashboardAuthSlice> sliceOpt = resolveAuthSlice(authentication);
        if (sliceOpt.isEmpty()) {
            out.put("ok", false);
            out.put("message", "로그인이 필요합니다.");
            return out;
        }
        DashboardAuthSlice s = sliceOpt.get();
        out.put("ok", true);
        out.put("role", s.user().getRole());
        out.put("orgLevel", s.orgLevel().isEmpty() ? null : s.orgLevel());
        putInsightsAndHqHub(out, s);
        return out;
    }

    private Optional<DashboardAuthSlice> resolveAuthSlice(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return Optional.empty();
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        String orgLevel = org != null && org.get("orgLevel") != null ? org.get("orgLevel").toString().trim() : "";
        String compId = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
        String compNm = org != null && org.get("compNm") != null ? org.get("compNm").toString().trim() : "";
        Long orgUnitId = null;
        if (org != null && org.get("orgUnitId") instanceof Number n) {
            orgUnitId = n.longValue();
        }
        Optional<OrgUnit> resolvedOu = authService.resolveOrgUnitForLoginId(user.getUsername());
        if (resolvedOu.isPresent()) {
            OrgUnit ou = resolvedOu.get();
            if (orgLevel.isEmpty() && ou.getOrgLevel() != null) {
                orgLevel = ou.getOrgLevel().name();
            }
            if (orgUnitId == null) {
                orgUnitId = ou.getId();
            }
            if (compId.isEmpty() && ou.getCode() != null && !ou.getCode().isBlank()) {
                compId = ou.getCode().trim();
            }
            if (compNm.isEmpty() && ou.getName() != null && !ou.getName().isBlank()) {
                compNm = ou.getName().trim();
            }
        }
        Set<String> merchantScope = orgAccessService.visibleMerchantCompCodes(authentication);
        boolean admin = "ADMIN".equalsIgnoreCase(user.getRole());
        boolean unrestricted = admin || merchantScope == null;
        boolean emptyScope = !unrestricted && merchantScope != null && merchantScope.isEmpty();
        LocalDate today = LocalDate.now();
        Set<String> merchantsForQuery = unrestricted
                ? Collections.emptySet()
                : Objects.requireNonNullElse(merchantScope, Collections.emptySet());
        return Optional.of(new DashboardAuthSlice(
                user, orgLevel, compId, compNm, orgUnitId, merchantScope, admin, unrestricted, emptyScope, today, merchantsForQuery));
    }

    private void putInsightsAndHqHub(Map<String, Object> out, DashboardAuthSlice s) {
        AppUser user = s.user();
        String orgLevel = s.orgLevel();
        String compId = s.compId();
        Long orgUnitId = s.orgUnitId();
        boolean admin = s.admin();
        boolean unrestricted = s.unrestricted();
        boolean emptyScope = s.emptyScope();
        LocalDate today = s.today();
        Set<String> merchantsForQuery = s.merchantsForQuery();

        if (admin || "HEADQUARTERS".equalsIgnoreCase(orgLevel)) {
            try {
                out.put("hqHub", dashboardHqHubService.build(admin, unrestricted, merchantsForQuery, emptyScope, orgUnitId, today));
            } catch (Exception ex) {
                log.warn("dashboard hqHub build failed for user={}", user.getUsername(), ex);
                out.put("hqHub", dashboardHqHubService.buildDegraded(admin, ex));
            }
        } else {
            out.put("hqHub", null);
        }

        Map<String, Object> insights;
        try {
            insights = dashboardInsightsService.build(
                    unrestricted, merchantsForQuery, emptyScope, today, orgLevel, compId, orgUnitId);
        } catch (Exception ex) {
            log.warn("dashboard insights build failed for user={}", user.getUsername(), ex);
            insights = dashboardInsightsService.buildDegraded(ex);
        }
        insights.put("llmNarrativeEnabled", Boolean.FALSE);
        out.put("insights", insights);
    }

    private static Map<String, Object> zeros() {
        return Map.of(
                "txnTotal", 0L,
                "txnApproved", 0L,
                "amtApprovedSum", BigDecimal.ZERO.setScale(2)
        );
    }

    private Map<String, Object> aggregatePeriod(boolean unrestricted, Set<String> merchantIds,
                                                LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        Object raw = unrestricted
                ? pgTrnsctnRepository.dashboardAggregateAll(start, endExclusive)
                : pgTrnsctnRepository.dashboardAggregateForMerchants(start, endExclusive, merchantIds);
        Object[] row = DashboardTupleRows.normalizeRow(raw);
        return rowToAgg(row);
    }

    private List<Map<String, Object>> aggregateByCurrency(boolean unrestricted, Set<String> merchantIds,
                                                          LocalDate from, LocalDate to) {
        if (!unrestricted && (merchantIds == null || merchantIds.isEmpty())) {
            return Collections.emptyList();
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        List<Object[]> rawRows = unrestricted
                ? pgTrnsctnRepository.dashboardAggregateByCurrencyAll(start, endExclusive)
                : pgTrnsctnRepository.dashboardAggregateByCurrencyForMerchants(start, endExclusive, merchantIds);
        return DashboardCurrencyAggregate.mergeSalesByCurrencyRows(rawRows);
    }

    private static Map<String, Object> rowToAgg(Object[] row) {
        long total = DashboardTupleRows.readLong(row != null && row.length > 0 ? row[0] : null);
        long appr = DashboardTupleRows.readLong(row != null && row.length > 1 ? row[1] : null);
        BigDecimal sum = DashboardTupleRows.readDecimal(row != null && row.length > 2 ? row[2] : null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("txnTotal", total);
        m.put("txnApproved", appr);
        m.put("amtApprovedSum", sum.setScale(2, RoundingMode.HALF_UP));
        return m;
    }

    private static boolean canViewServerUsageSummary(String role, String orgLevel) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }
        return "HEADQUARTERS".equalsIgnoreCase(orgLevel != null ? orgLevel : "");
    }

    private static List<Map<String, Object>> buildQuickLinks(String role, String orgLevel) {
        List<Map<String, Object>> list = new ArrayList<>();
        String lvl = orgLevel != null ? orgLevel : "";
        if ("ADMIN".equalsIgnoreCase(role) || "HEADQUARTERS".equalsIgnoreCase(lvl)) {
            list.add(link("/hq/serverManage", "서버운영관리"));
            list.add(link("/calc/payList", "결제내역"));
            list.add(link("/hq/settlementAdmin", "정산관리설정"));
            list.add(link("/calc/exCalcList", "정산실행"));
            return list;
        }
        if ("REGIONAL".equalsIgnoreCase(lvl)) {
            list.add(link("/calc/payList", "결제내역"));
            list.add(link("/calc/calcGmList", "가맹점정산내역"));
            list.add(link("/calc/unpaidMng", "미수금관리"));
            return list;
        }
        if ("MASTER_DIST".equalsIgnoreCase(lvl) || "BRANCH".equalsIgnoreCase(lvl)
                || "AGENCY".equalsIgnoreCase(lvl) || "SALES_OFFICE".equalsIgnoreCase(lvl)) {
            list.add(link("/calc/payList", "결제내역"));
            list.add(link("/calc/calcGmList", "가맹점정산내역"));
            list.add(link("/comp/compList", "업체관리"));
            return list;
        }
        if ("MERCHANT".equalsIgnoreCase(lvl)) {
            list.add(link("/calc/payList", "결제내역"));
            list.add(link("/settlement/franchiseList", "가맹점정산내역"));
            list.add(link("/calc/feeList", "수수료내역"));
            return list;
        }
        list.add(link("/calc/payList", "결제내역"));
        return list;
    }

    private static Map<String, Object> link(String url, String label) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("url", url);
        m.put("label", label);
        return m;
    }

    private static Map<String, Object> settlementRunToMap(SettlementRun r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : null);
        m.put("approveAmt", r.getApproveAmt());
        m.put("payAmt", r.getPayAmt());
        m.put("totalFee", r.getTotalFee());
        m.put("includedTxnCnt", r.getIncludedTxnCnt());
        m.put("calcCycleSnapshot", r.getCalcCycleSnapshot());
        m.put("status", r.getStatus());
        m.put("settlementPublishSts", r.getSettlementPublishSts());
        return m;
    }

    private static String buildInsightHint(String orgLevel, boolean admin, boolean emptyScope) {
        if (emptyScope && !admin) {
            return "소속 조직 또는 허용 가맹 범위가 없어 거래 요약이 0으로 표시됩니다.";
        }
        if (admin) {
            return "전사 기준 거래·매출 요약입니다. 서버 트래픽은 일간 수집 데이터 기반입니다.";
        }
        String lvl = orgLevel != null ? orgLevel.toUpperCase(Locale.ROOT) : "";
        return switch (lvl) {
            case "HEADQUARTERS" -> "DASHBOARD: 조직·7일 매출 추이·정산·업무 바로가기와 리스크 요약을 한 화면에서 확인할 수 있습니다.";
            case "REGIONAL" -> "본사 하위 가맹점 기준 결제·승인 금액 요약입니다.";
            case "MASTER_DIST", "BRANCH", "AGENCY", "SALES_OFFICE" -> "담당 가맹점 범위 내 결제·승인 건수 및 금액 요약입니다.";
            case "MERCHANT" -> "가맹점 기준 거래 요약과 정산 실행 이력(정산 달력)을 제공합니다.";
            default -> "로그인 조직 범위 내 거래 요약입니다.";
        };
    }
}
