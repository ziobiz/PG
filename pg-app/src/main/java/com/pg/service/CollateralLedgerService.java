package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.RollingReserve;
import com.pg.entity.SettlementSetting;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.RollingReserveRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.settlement.SettlementCycleTiming;
import com.pg.service.settlement.SettlementPeriodResolver;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
import com.pg.util.RouteNoDisplayUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 가맹점별 담보금(롤링) 적립·해지 내역 조회.
 * 적립은 {@link SettlementCalcService} 정산 실행 시 거래 건별로 생성됩니다.
 */
@Service
public class CollateralLedgerService {

    private static final DateTimeFormatter RELEASED_AT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RollingReserveRepository rollingReserveRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final SettlementSettingRepository settlementSettingRepository;

    public CollateralLedgerService(RollingReserveRepository rollingReserveRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   PgTrnsctnRepository pgTrnsctnRepository,
                                   CommissionPolicyRepository commissionPolicyRepository,
                                   HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                                   SettlementSettingRepository settlementSettingRepository) {
        this.rollingReserveRepository = rollingReserveRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.settlementSettingRepository = settlementSettingRepository;
    }

    private FeeListRoundingPolicy settlementLedgerRoundPolicy() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(s -> FeeCurrencyRoundResolver.from(s).forCurrency(PayDisplayCurrency.alphaFromSettings(s)))
                .orElseGet(FeeListRoundingPolicy::defaults);
    }

    private String resolveStatementCurrency(String compId) {
        if (compId == null || compId.isBlank()) {
            return "KRW";
        }
        return commissionPolicyRepository.findByScope(compId.trim())
                .map(CommissionPolicy::getCurrencyCode)
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toUpperCase(Locale.ROOT))
                .orElse("KRW");
    }

    /**
     * 보류: {@code release_date}는 적용일 기준 영업일 보류일수({@link com.pg.util.BusinessDayCalendar})로 이미 산출됨.
     * 정산주기가 {@link SettlementCycleTiming#isCalcStartTimeApplicableForAuto(String)} true인 경우(예: D1~D30·주간 등)
     * 에만 시각을 가맹 {@link SettlementSetting#getCalcStartTime()} 으로 표시하고, D0·RT·T0·분·시 격자 등은 00:00 예정 유지.
     * 해지(RELEASED): 실제 정산 반영 시각({@code released_at}).
     */
    private String formatReleaseBizDateTime(RollingReserve r, SettlementSetting merchantSetting) {
        if (r == null) {
            return "";
        }
        boolean hold = "HOLD".equalsIgnoreCase(r.getStatus() != null ? r.getStatus() : "");
        if (hold) {
            if (r.getReleaseDate() == null) {
                return "";
            }
            String cycleNorm = "";
            if (merchantSetting != null && merchantSetting.getCalcCycle() != null) {
                cycleNorm = SettlementPeriodResolver.normalizeCalcCycle(merchantSetting.getCalcCycle());
            }
            LocalTime start = merchantSetting != null ? merchantSetting.getCalcStartTime() : null;
            boolean useCalcStart = start != null && SettlementCycleTiming.isCalcStartTimeApplicableForAuto(cycleNorm);
            if (!useCalcStart) {
                return r.getReleaseDate() + " 00:00:00 (영업일 기준 예정)";
            }
            return r.getReleaseDate() + " " + start + " (영업일·정산개시시각 기준 예정)";
        }
        if (r.getReleasedAt() != null) {
            return RELEASED_AT_FMT.format(r.getReleasedAt());
        }
        if (r.getReleaseDate() != null) {
            return r.getReleaseDate() + " 00:00:00";
        }
        return "";
    }

    /**
     * 오늘 기준 남은 영업일 수: (오늘, 해지일] 구간의 주말 제외 일수. 해지일 당일이면 0.
     * 공휴일 미반영({@link SettlementReportService#addBusinessDays} 와 동일 근사).
     */
    public static int remainingBusinessDaysAfterUntil(LocalDate today, LocalDate releaseDate) {
        if (releaseDate == null || today == null) return 0;
        if (today.isAfter(releaseDate)) return 0;
        int n = 0;
        LocalDate d = today;
        while (d.isBefore(releaseDate)) {
            d = d.plusDays(1);
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                n++;
            }
        }
        return n;
    }

    private static LocalDate effectiveHoldStart(RollingReserve r) {
        if (r.getHoldStartDate() != null) {
            return r.getHoldStartDate();
        }
        if (r.getCreatedAt() != null) {
            return r.getCreatedAt().toLocalDate();
        }
        return null;
    }

    public PageResult<Map<String, Object>> search(
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchCompId,
            String searchCompNm,
            String searchFieldType,
            String searchKeyword,
            String searchStatus,
            String searchOrderDir,
            int page,
            int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;

        String effFt = "ALL";
        String effKw = "";
        if (searchFieldType != null && !searchFieldType.isBlank()) {
            effFt = searchFieldType.trim().toUpperCase(Locale.ROOT);
            effKw = searchKeyword != null ? searchKeyword.trim() : "";
        } else if (searchCompId != null && !searchCompId.isBlank()) {
            effFt = "COMP_ID";
            effKw = searchCompId.trim();
        } else if (searchCompNm != null && !searchCompNm.isBlank()) {
            effFt = "COMP_NM";
            effKw = searchCompNm.trim();
        }
        if ("COMP_NM".equals(effFt) && effKw.isEmpty()) {
            effFt = "ALL";
        }
        if ("SETTLE_DAY".equals(effFt)) {
            effFt = "SETTLE_TARGET_DAY";
        }

        LocalDate from = searchFromDate;
        LocalDate to = searchToDate;
        if (from == null && to == null) {
            to = LocalDate.now();
            from = to.minusMonths(3);
        } else if (from == null) {
            from = to.minusYears(1);
        } else if (to == null) {
            to = LocalDate.now();
        }

        LocalDate holdDayTarget = null;
        LocalDate releaseDayTarget = null;
        boolean periodByReleaseDate = "SETTLE_RUN_DAY".equals(effFt) && effKw.isEmpty();
        if ("SETTLE_TARGET_DAY".equals(effFt) && !effKw.isEmpty()) {
            Integer dom = parseSettlementDayOfMonthKeyword(effKw);
            if (dom == null) {
                return emptyPage(page, size);
            }
            LocalDate monthAnchor = searchFromDate != null ? searchFromDate
                    : (searchToDate != null ? searchToDate : LocalDate.now());
            holdDayTarget = resolveSettlementDayInMonth(monthAnchor, dom);
            if (holdDayTarget == null) {
                return emptyPage(page, size);
            }
            effFt = "ALL";
            effKw = "";
        } else if ("SETTLE_RUN_DAY".equals(effFt) && !effKw.isEmpty()) {
            Integer dom = parseSettlementDayOfMonthKeyword(effKw);
            if (dom == null) {
                return emptyPage(page, size);
            }
            LocalDate monthAnchor = searchFromDate != null ? searchFromDate
                    : (searchToDate != null ? searchToDate : LocalDate.now());
            releaseDayTarget = resolveSettlementDayInMonth(monthAnchor, dom);
            if (releaseDayTarget == null) {
                return emptyPage(page, size);
            }
            effFt = "ALL";
            effKw = "";
        }

        Set<String> merchantFilter = null;
        if (searchCompId != null && !searchCompId.isBlank()) {
            merchantFilter = new LinkedHashSet<>();
            merchantFilter.add(searchCompId.trim());
        }
        if ("COMP_ID".equals(effFt) && !effKw.isEmpty()) {
            if (merchantFilter == null) {
                merchantFilter = new LinkedHashSet<>();
            }
            merchantFilter.add(effKw.trim());
            effFt = "ALL";
            effKw = "";
        }
        if ("COMP_NM".equals(effFt) && !effKw.isEmpty()) {
            List<OrgUnit> hits = orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(
                    OrgLevel.MERCHANT, effKw.trim());
            Set<String> codes = hits.stream().map(OrgUnit::getCode).filter(Objects::nonNull).collect(Collectors.toSet());
            if (merchantFilter == null) {
                merchantFilter = codes;
            } else {
                merchantFilter.retainAll(codes);
            }
            if (merchantFilter.isEmpty()) {
                return emptyPage(page, size);
            }
            effFt = "ALL";
            effKw = "";
        }
        if (searchCompNm != null && !searchCompNm.isBlank()) {
            List<OrgUnit> hits = orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(
                    OrgLevel.MERCHANT, searchCompNm.trim());
            Set<String> codes = hits.stream().map(OrgUnit::getCode).filter(Objects::nonNull).collect(Collectors.toSet());
            if (merchantFilter == null) {
                merchantFilter = codes;
            } else {
                merchantFilter.retainAll(codes);
            }
            if (merchantFilter.isEmpty()) {
                return emptyPage(page, size);
            }
        }

        String statusNorm = normalizeStatus(searchStatus);
        LocalDate today = LocalDate.now();
        final String effFtFinal = effFt;
        final String effKwFinal = effKw;

        List<RollingReserve> all = rollingReserveRepository.findAll();
        List<RollingReserve> filtered = new ArrayList<>();
        for (RollingReserve r : all) {
            if (merchantFilter != null && !merchantFilter.contains(r.getMerchantId())) {
                continue;
            }
            if (statusNorm != null && !statusNorm.equalsIgnoreCase(r.getStatus() != null ? r.getStatus() : "")) {
                continue;
            }
            LocalDate effStart = effectiveHoldStart(r);
            LocalDate releaseDate = r.getReleaseDate();
            if (holdDayTarget != null) {
                if (effStart == null || !holdDayTarget.equals(effStart)) {
                    continue;
                }
            } else if (releaseDayTarget != null) {
                if (releaseDate == null || !releaseDayTarget.equals(releaseDate)) {
                    continue;
                }
            } else {
                LocalDate rangeDate = periodByReleaseDate ? releaseDate : effStart;
                if (rangeDate == null) {
                    continue;
                }
                if (rangeDate.isBefore(from) || rangeDate.isAfter(to)) {
                    continue;
                }
            }
            if (!collateralListRowMatches(r, effFtFinal, effKwFinal)) {
                continue;
            }
            filtered.add(r);
        }

        boolean asc = searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim());
        filtered.sort(asc
                ? Comparator.comparing(RollingReserve::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(RollingReserve::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        int total = filtered.size();
        int fromIdx = Math.max(0, (page - 1) * size);
        int toIdx = Math.min(total, fromIdx + size);
        List<RollingReserve> slice = fromIdx < total ? filtered.subList(fromIdx, toIdx) : List.of();

        Map<String, String> codeToName = new HashMap<>();
        Map<String, SettlementSetting> settingByMid = new HashMap<>();
        for (RollingReserve r : slice) {
            String midKey = r.getMerchantId() != null ? r.getMerchantId().trim() : "";
            if (midKey.isEmpty()) {
                continue;
            }
            if (!codeToName.containsKey(midKey)) {
                orgUnitRepository.findByCode(midKey)
                        .or(() -> orgUnitRepository.findByCodeIgnoreCase(midKey))
                        .ifPresent(o -> codeToName.put(midKey, o.getName()));
            }
            if (!settingByMid.containsKey(midKey)) {
                orgUnitRepository.findByCode(midKey)
                        .or(() -> orgUnitRepository.findByCodeIgnoreCase(midKey))
                        .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                        .ifPresent(ss -> settingByMid.put(midKey, ss));
            }
        }

        Set<String> trnIds = new LinkedHashSet<>();
        for (RollingReserve r : slice) {
            if (r.getTrnId() != null && !r.getTrnId().isBlank()) {
                trnIds.add(r.getTrnId().trim());
            }
        }
        Map<String, String> routeByTrn = new HashMap<>();
        Map<String, String> curByTrn = new HashMap<>();
        if (!trnIds.isEmpty()) {
            for (PgTrnsctn t : pgTrnsctnRepository.findAllById(trnIds)) {
                String rid = t.getTrnId();
                routeByTrn.put(rid, t.getRouteNo() != null ? t.getRouteNo() : "");
                String cty = t.getCurType();
                curByTrn.put(rid, cty != null && !cty.isBlank() ? cty.trim().toUpperCase(Locale.ROOT) : "");
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int rowNo = fromIdx + 1;
        FeeListRoundingPolicy rp = settlementLedgerRoundPolicy();
        for (RollingReserve r : slice) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rowNo", rowNo++);
            String mid = r.getMerchantId();
            m.put("compId", mid);
            String midTrim = mid != null ? mid.trim() : "";
            m.put("compNm", codeToName.getOrDefault(midTrim, mid));
            String tid = r.getTrnId() != null ? r.getTrnId().trim() : "";
            m.put("trnId", tid);
            m.put("routeNo", tid.isEmpty() ? "-" : RouteNoDisplayUtil.formatForDisplay(routeByTrn.getOrDefault(tid, "")));
            String curCell = tid.isEmpty() ? "" : curByTrn.getOrDefault(tid, "");
            if (curCell == null || curCell.isBlank()) {
                curCell = resolveStatementCurrency(mid);
            }
            m.put("curType", curCell);
            m.put("reserveAmt", FeeListRoundingPolicy.round(r.getReserveAmt() != null ? r.getReserveAmt() : BigDecimal.ZERO, rp).doubleValue());
            m.put("rollingPct", r.getRollingPct() != null ? r.getRollingPct().stripTrailingZeros().toPlainString() : "");
            m.put("holdBusinessDays", r.getHoldBusinessDays() != null ? r.getHoldBusinessDays() : "");
            LocalDate hsd = r.getHoldStartDate() != null ? r.getHoldStartDate() : effectiveHoldStart(r);
            m.put("holdStartDt", hsd != null ? hsd.toString() : "");
            LocalDate releaseDate = r.getReleaseDate();
            String releaseDateStr = releaseDate != null ? releaseDate.toString() : "";
            String settlementNoteDateParam = releaseDate != null ? releaseDateStr : "-";
            m.put("releaseDt", releaseDateStr);
            m.put("releaseBizDtTime", formatReleaseBizDateTime(r, settingByMid.get(midTrim)));
            boolean hold = "HOLD".equalsIgnoreCase(r.getStatus() != null ? r.getStatus() : "");
            m.put("remainingBizDays", hold ? remainingBusinessDaysAfterUntil(today, releaseDate) : 0);
            m.put("status", r.getStatus() != null ? r.getStatus() : "");
            m.put("statusNm", hold ? "보류" : "해지(정산반영)");
            LocalDateTime ra = r.getReleasedAt();
            m.put("releasedAt", ra != null ? ra.toString() : "");
            m.put("settlementNoteTpl", hold ? "HOLD_AFTER_RELEASE" : "REFLECTED_ON_SETTLEMENT");
            m.put("settlementNoteDate", settlementNoteDateParam);
            m.put("settlementNote", hold
                    ? "해지일(" + settlementNoteDateParam + ") 이후 정산 실행 시 지급액에 합산"
                    : "정산 실행 시 지급액에 반영됨");
            rows.add(m);
        }

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(total);
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) total / Math.max(1, size))));
        return pr;
    }

    private static String normalizeStatus(String s) {
        if (s == null || s.isBlank() || "ALL".equalsIgnoreCase(s.trim())) {
            return null;
        }
        return s.trim().toUpperCase(Locale.ROOT);
    }

    private static Integer parseSettlementDayOfMonthKeyword(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (!s.matches("\\d{1,2}")) {
            return null;
        }
        int dom;
        try {
            dom = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
        if (dom < 1 || dom > 31) {
            return null;
        }
        return dom;
    }

    private static LocalDate resolveSettlementDayInMonth(LocalDate monthAnchor, int dayOfMonth) {
        LocalDate anchor = monthAnchor != null ? monthAnchor : LocalDate.now();
        try {
            return anchor.withDayOfMonth(dayOfMonth);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static boolean collateralListRowMatches(RollingReserve r, String fieldType, String keyword) {
        String ft = fieldType == null || fieldType.isBlank() ? "ALL" : fieldType.trim().toUpperCase(Locale.ROOT);
        String kw = keyword == null ? "" : keyword.trim();
        if (!"ALL".equals(ft) && kw.isEmpty()) {
            return true;
        }
        if ("ALL".equals(ft) && kw.isEmpty()) {
            return true;
        }
        String kLow = kw.toLowerCase(Locale.ROOT);
        String mid = r.getMerchantId() != null ? r.getMerchantId().trim().toLowerCase(Locale.ROOT) : "";
        String tid = r.getTrnId() != null ? r.getTrnId().trim().toLowerCase(Locale.ROOT) : "";
        LocalDate holdStart = effectiveHoldStart(r);
        LocalDate releaseDate = r.getReleaseDate();
        return switch (ft) {
            case "COMP_ID", "MID" -> mid.contains(kLow);
            case "COMP_NM" -> false;
            case "SETTLE_TARGET_DAY", "SETTLE_DAY" -> {
                Integer dom = parseSettlementDayOfMonthKeyword(kw);
                if (dom == null) {
                    yield false;
                }
                yield (holdStart != null && holdStart.getDayOfMonth() == dom);
            }
            case "SETTLE_RUN_DAY" -> {
                Integer dom = parseSettlementDayOfMonthKeyword(kw);
                if (dom == null) {
                    yield false;
                }
                yield (releaseDate != null && releaseDate.getDayOfMonth() == dom);
            }
            case "ALL" -> mid.contains(kLow) || tid.contains(kLow)
                    || (holdStart != null && holdStart.toString().contains(kw))
                    || (releaseDate != null && releaseDate.toString().contains(kw));
            default -> true;
        };
    }

    private static PageResult<Map<String, Object>> emptyPage(int page, int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return pr;
    }
}
