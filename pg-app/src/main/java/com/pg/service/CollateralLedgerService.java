package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.RollingReserve;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.RollingReserveRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public CollateralLedgerService(RollingReserveRepository rollingReserveRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   PgTrnsctnRepository pgTrnsctnRepository) {
        this.rollingReserveRepository = rollingReserveRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    /**
     * 보류: 영업일 기준으로 산출된 해제 예정일(자정) 문자열. 해지: 실제 정산 반영 시각.
     */
    public static String formatReleaseBizDateTime(RollingReserve r) {
        if (r == null) {
            return "";
        }
        boolean hold = "HOLD".equalsIgnoreCase(r.getStatus() != null ? r.getStatus() : "");
        if (hold) {
            if (r.getReleaseDate() == null) {
                return "";
            }
            return r.getReleaseDate() + " 00:00:00 (영업일 기준 예정)";
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
            String searchStatus,
            String searchOrderDir,
            int page,
            int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;

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

        Set<String> merchantFilter = null;
        if (searchCompId != null && !searchCompId.isBlank()) {
            merchantFilter = new LinkedHashSet<>();
            merchantFilter.add(searchCompId.trim());
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
            if (effStart == null) continue;
            if (effStart.isBefore(from) || effStart.isAfter(to)) {
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
        for (RollingReserve r : slice) {
            if (!codeToName.containsKey(r.getMerchantId())) {
                orgUnitRepository.findByCode(r.getMerchantId()).ifPresent(o -> codeToName.put(r.getMerchantId(), o.getName()));
            }
        }

        Set<String> trnIds = new LinkedHashSet<>();
        for (RollingReserve r : slice) {
            if (r.getTrnId() != null && !r.getTrnId().isBlank()) {
                trnIds.add(r.getTrnId().trim());
            }
        }
        Map<String, String> routeByTrn = new HashMap<>();
        if (!trnIds.isEmpty()) {
            for (PgTrnsctn t : pgTrnsctnRepository.findAllById(trnIds)) {
                String rid = t.getTrnId();
                routeByTrn.put(rid, t.getRouteNo() != null ? t.getRouteNo() : "");
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int rowNo = fromIdx + 1;
        for (RollingReserve r : slice) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rowNo", rowNo++);
            String mid = r.getMerchantId();
            m.put("compId", mid);
            m.put("compNm", codeToName.getOrDefault(mid, mid));
            String tid = r.getTrnId() != null ? r.getTrnId().trim() : "";
            m.put("trnId", tid);
            m.put("routeNo", tid.isEmpty() ? "" : routeByTrn.getOrDefault(tid, ""));
            m.put("reserveAmt", r.getReserveAmt() != null ? r.getReserveAmt().longValue() : 0L);
            m.put("rollingPct", r.getRollingPct() != null ? r.getRollingPct().stripTrailingZeros().toPlainString() : "");
            m.put("holdBusinessDays", r.getHoldBusinessDays() != null ? r.getHoldBusinessDays() : "");
            LocalDate hsd = r.getHoldStartDate() != null ? r.getHoldStartDate() : effectiveHoldStart(r);
            m.put("holdStartDt", hsd != null ? hsd.toString() : "");
            m.put("releaseDt", r.getReleaseDate() != null ? r.getReleaseDate().toString() : "");
            m.put("releaseBizDtTime", formatReleaseBizDateTime(r));
            boolean hold = "HOLD".equalsIgnoreCase(r.getStatus() != null ? r.getStatus() : "");
            m.put("remainingBizDays", hold ? remainingBusinessDaysAfterUntil(today, r.getReleaseDate()) : 0);
            m.put("status", r.getStatus() != null ? r.getStatus() : "");
            m.put("statusNm", hold ? "보류" : "해지(정산반영)");
            LocalDateTime ra = r.getReleasedAt();
            m.put("releasedAt", ra != null ? ra.toString() : "");
            m.put("settlementNote", hold
                    ? "해지일(" + (r.getReleaseDate() != null ? r.getReleaseDate() : "-") + ") 이후 정산 실행 시 지급액에 합산"
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
