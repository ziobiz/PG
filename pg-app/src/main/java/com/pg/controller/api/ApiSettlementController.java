package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListItemDto;
import com.pg.api.dto.PayListRowContext;
import com.pg.entity.AppUser;
import com.pg.entity.BalanceDeduction;
import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantReceivable;
import com.pg.entity.MerchantProfile;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRecovery;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.BalanceDeductionRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.SettlementRecoveryRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.AuthService;
import com.pg.service.CollateralLedgerService;
import com.pg.service.OrgAccessService;
import com.pg.service.OrgPagePermissionService;
import com.pg.service.PayListService;
import com.pg.service.SettlementCalcService;
import com.pg.service.SettlementReportService;
import com.pg.service.settlement.FeeListTxnBreakdownCalculator;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.service.settlement.SettlementAutoRunService;
import com.pg.service.settlement.SettlementCycleTiming;
import com.pg.service.settlement.SettlementPeriodResolver;
import com.pg.util.CommissionExtraFeeUtil;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.MerchantFeeVatUtil;
import com.pg.util.PercentDecimalHelper;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/settlement", produces = "application/json")
public class ApiSettlementController {

    private final SettlementCalcService settlementCalcService;
    private final SettlementRunRepository settlementRunRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final BalanceDeductionRepository balanceDeductionRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final AuthService authService;
    private final SettlementReportService settlementReportService;
    private final CollateralLedgerService collateralLedgerService;
    private final PayListService payListService;
    private final OrgAccessService orgAccessService;
    private final SettlementAutoRunService settlementAutoRunService;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;
    private final SettlementRecoveryRepository settlementRecoveryRepository;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final SettlementArrearsService settlementArrearsService;
    private final OrgPagePermissionService orgPagePermissionService;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public ApiSettlementController(SettlementCalcService settlementCalcService,
                                   SettlementRunRepository settlementRunRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   DistributionFeeConfigRepository distributionFeeConfigRepository,
                                   PgTrnsctnRepository pgTrnsctnRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   MerchantPgBindingRepository merchantPgBindingRepository,
                                   CommissionPolicyRepository commissionPolicyRepository,
                                   ChargebackFeePolicyRepository chargebackFeePolicyRepository,
                                   SettlementSettingRepository settlementSettingRepository,
                                   BalanceDeductionRepository balanceDeductionRepository,
                                   HqApiConfigRepository hqApiConfigRepository,
                                   AuthService authService,
                                   SettlementReportService settlementReportService,
                                   CollateralLedgerService collateralLedgerService,
                                   PayListService payListService,
                                   OrgAccessService orgAccessService,
                                   SettlementAutoRunService settlementAutoRunService,
                                   HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                                   FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator,
                                   SettlementRecoveryRepository settlementRecoveryRepository,
                                   MerchantReceivableRepository merchantReceivableRepository,
                                   SettlementArrearsService settlementArrearsService,
                                   OrgPagePermissionService orgPagePermissionService) {
        this.settlementCalcService = settlementCalcService;
        this.settlementRunRepository = settlementRunRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.balanceDeductionRepository = balanceDeductionRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.authService = authService;
        this.settlementReportService = settlementReportService;
        this.collateralLedgerService = collateralLedgerService;
        this.payListService = payListService;
        this.orgAccessService = orgAccessService;
        this.settlementAutoRunService = settlementAutoRunService;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
        this.settlementRecoveryRepository = settlementRecoveryRepository;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.settlementArrearsService = settlementArrearsService;
        this.orgPagePermissionService = orgPagePermissionService;
    }

    private FeeCurrencyRoundResolver resolveFeeCurrencyRoundResolver() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(FeeCurrencyRoundResolver::from)
                .orElseGet(() -> FeeCurrencyRoundResolver.from(null));
    }

    private void attachFeeCurrencyMeta(PageResult<Map<String, Object>> pr) {
        if (pr == null) {
            return;
        }
        Map<String, Object> meta = pr.getMeta() != null ? new LinkedHashMap<>(pr.getMeta()) : new LinkedHashMap<>();
        meta.put("feeCurrencyFormatByCur", resolveFeeCurrencyRoundResolver().toClientByCurrencyMap());
        pr.setMeta(meta);
    }

    private String resolveMerchantStatementCurrency(String compId) {
        if (compId == null || compId.isBlank()) {
            return "KRW";
        }
        CommissionPolicy pol = resolveCommissionPolicyForMerchant(compId.trim());
        String c = pol.getCurrencyCode();
        return c != null && !c.isBlank() ? c.trim().toUpperCase(Locale.ROOT) : "KRW";
    }

    private static BigDecimal feeListMoney(double x, FeeListRoundingPolicy rp) {
        return FeeListRoundingPolicy.round(BigDecimal.valueOf(x), rp);
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

    /** 화면 {@code searchOrderDir}: ASC 만 오름차순, 그 외·null 은 내림차순(기본) */
    private static Sort.Direction sortDirectionFromSearchOrderDir(String searchOrderDir) {
        return (searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim()))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private static Comparator<Map<String, Object>> mapRowsCalcDtPrimaryComparator(String searchOrderDir) {
        Comparator<Map<String, Object>> primary = Comparator.comparing(m -> String.valueOf(m.getOrDefault("calcDt", "")));
        return sortDirectionFromSearchOrderDir(searchOrderDir) == Sort.Direction.DESC ? primary.reversed() : primary;
    }

    /**
     * 정산리포트 — 노출 가능 여부(로그인 조직 기준).
     */
    @GetMapping("/report/access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportAccess(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                settlementReportService.reportAccessMeta(resolveOrgForReport(authentication))));
    }

    /**
     * 정산리포트 — 정산집계: 결제일·가맹·통화별 집계(시트식) 또는 본사(REGIONAL) 합산.
     * searchReportKind: MERCHANT_STMT(가맹) | REGIONAL_PAYOUT(총본사→본사)
     */
    @GetMapping("/report/aggregate")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> reportAggregate(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchMerchantId,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchMasterId,
            @RequestParam(required = false) String searchRegionalId,
            @RequestParam(required = false) String searchRegionalCompId,
            @RequestParam(required = false) String searchCurType,
            @RequestParam(required = false, defaultValue = "MERCHANT_STMT") String searchReportKind,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        String mid = (searchMerchantId != null && !searchMerchantId.isBlank()) ? searchMerchantId
                : (searchCompId != null ? searchCompId : null);
        String rid = (searchRegionalId != null && !searchRegionalId.isBlank()) ? searchRegionalId
                : (searchRegionalCompId != null ? searchRegionalCompId : null);
        return ResponseEntity.ok(ApiResponse.ok(
                settlementReportService.aggregate(resolveOrgForReport(authentication),
                        searchFromDate, searchToDate, mid, searchMasterId, rid, searchCurType, searchReportKind, page, size)));
    }

    /**
     * 정산리포트 — 정산실시: 배치 정산 실행 결과(SettlementRun).
     */
    @GetMapping("/report/execute")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> reportExecute(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchMerchantId,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchMasterId,
            @RequestParam(required = false) String searchRegionalId,
            @RequestParam(required = false) String searchRegionalCompId,
            @RequestParam(required = false, defaultValue = "MERCHANT_STMT") String searchReportKind,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        String mid = (searchMerchantId != null && !searchMerchantId.isBlank()) ? searchMerchantId
                : (searchCompId != null ? searchCompId : null);
        String rid = (searchRegionalId != null && !searchRegionalId.isBlank()) ? searchRegionalId
                : (searchRegionalCompId != null ? searchRegionalCompId : null);
        return ResponseEntity.ok(ApiResponse.ok(
                settlementReportService.executeReport(resolveOrgForReport(authentication),
                        searchFromDate, searchToDate, mid, searchMasterId, rid, searchReportKind, page, size)));
    }

    /**
     * 정산리포트 — 정산집계표: 기간 합계 1행.
     */
    @GetMapping("/report/summary")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> reportSummary(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchMerchantId,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchMasterId,
            @RequestParam(required = false) String searchRegionalId,
            @RequestParam(required = false) String searchRegionalCompId,
            @RequestParam(required = false) String searchCurType,
            @RequestParam(required = false, defaultValue = "MERCHANT_STMT") String searchReportKind) {
        String mid = (searchMerchantId != null && !searchMerchantId.isBlank()) ? searchMerchantId
                : (searchCompId != null ? searchCompId : null);
        String rid = (searchRegionalId != null && !searchRegionalId.isBlank()) ? searchRegionalId
                : (searchRegionalCompId != null ? searchRegionalCompId : null);
        return ResponseEntity.ok(ApiResponse.ok(
                settlementReportService.summary(resolveOrgForReport(authentication),
                        searchFromDate, searchToDate, mid, searchMasterId, rid, searchCurType, searchReportKind)));
    }

    /**
     * 정산 확정(가맹) 리포트 목록 — 가맹점정산내역에 노출되는 조건과 동일하되, 상태가 CALCULATED(확정)인 실행 건만.
     * 지급보류 적치 건은 제외합니다.
     */
    @GetMapping("/report/confirmedRuns")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> reportConfirmedRuns(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                buildConfirmedSettlementReportPage(authentication, searchFromDate, searchToDate, searchCompNm, searchCompId, searchOrderDir, page, size)));
    }

    /**
     * 정산 확정 리포트 1건 상세 — 집계 구간 거래 기준 매출·취소·환불 등 건수·금액과 실행 저장값(지급액 등)을 함께 반환.
     */
    @GetMapping("/report/confirmedRunDetail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportConfirmedRunDetail(
            Authentication authentication,
            @RequestParam long settlementRunId) {
        Optional<SettlementRun> opt = settlementRunRepository.findById(settlementRunId);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("정산 실행 건을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        SettlementRun r = opt.get();
        String mid = r.getMerchantId() != null ? r.getMerchantId().trim() : "";
        if (mid.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드가 없습니다.", "VALIDATION"));
        }
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && !allowedMerchants.contains(mid)) {
            return ResponseEntity.ok(ApiResponse.fail("조회 권한이 없습니다.", "FORBIDDEN"));
        }
        if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
            return ResponseEntity.ok(ApiResponse.fail("지급보류 적치 건은 리포트 상세를 열 수 없습니다.", "FORBIDDEN"));
        }
        if (!"CALCULATED".equalsIgnoreCase(String.valueOf(r.getStatus()))) {
            return ResponseEntity.ok(ApiResponse.fail("확정(CALCULATED)된 정산만 리포트로 조회할 수 있습니다.", "INVALID_STATE"));
        }
        LocalDate qFrom = r.getPeriodFrom() != null ? r.getPeriodFrom() : r.getCalcDt();
        LocalDate qTo = r.getPeriodTo() != null ? r.getPeriodTo() : r.getCalcDt();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listRow", toConfirmedSettlementReportListRow(r, qFrom, qTo));
        List<PgTrnsctn> txs = pgTrnsctnRepository.findForSettlement(mid, r.resolvePeriodStartAt(), r.resolvePeriodEndAt());
        payload.put("txBreakdown", buildSettlementReportTxBreakdown(txs));
        payload.put("runTotals", buildSettlementRunTotalsForReport(r));
        payload.put("meta", settlementReportService.reportMeta());
        return ResponseEntity.ok(ApiResponse.ok(payload));
    }

    private Optional<OrgUnit> resolveOrgForReport(Authentication authentication) {
        String username = null;
        if (authentication != null && authentication.getPrincipal() instanceof AppUser u) {
            username = u.getUsername();
        }
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return authService.resolveOrgUnitForLoginId(username);
    }

    /**
     * 유통망 정산: 로그인 소속 조직·하위 가맹(가맹점정산내역과 동일한 {@link OrgAccessService#visibleMerchantCompCodes} 범위)만.
     * 집계 행마다 해당 조직 단계에 매핑되는 수수료 구간만 합산(본사 행에 총판 구간 금액을 섞지 않음). 총본사(HEADQUARTERS) 단계 포함.
     * searchCompDiv: HEADQUARTERS/REGIONAL/MASTER_DIST/BRANCH/AGENCY/SALES_OFFICE — 비우면 경로상 각 단계별로 모두 집계.
     */
    @GetMapping("/distributionList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> distributionList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompDiv,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String username = null;
        if (authentication != null && authentication.getPrincipal() instanceof AppUser u) {
            username = u.getUsername();
        }
        if (username == null || username.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }
        Optional<OrgUnit> userOrgOpt = authService.resolveOrgUnitForLoginId(username);
        if (userOrgOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }
        OrgUnit userOrg = userOrgOpt.get();
        if (userOrg.getOrgLevel() == OrgLevel.MERCHANT) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }

        List<OrgUnit> allOrgs = orgUnitRepository.findAll();
        Map<Long, OrgUnit> idToOu = allOrgs.stream().collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        Set<Long> userSubtree = new HashSet<>();
        userSubtree.add(userOrg.getId());
        userSubtree.addAll(collectDescendantOrgIds(userOrg.getId(), allOrgs));

        Set<String> allowedMerchantCodes = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchantCodes != null && allowedMerchantCodes.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }

        String levelFilter = searchCompDiv != null ? searchCompDiv.trim() : "";

        List<SettlementRun> list = settlementCalcService.listRuns(searchFromDate, searchToDate);
        Map<String, DistributionAgg> aggMap = new LinkedHashMap<>();
        for (SettlementRun r : list) {
            if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            OrgUnit merchant = orgUnitRepository.findByCode(r.getMerchantId()).orElse(null);
            if (merchant == null || merchant.getOrgLevel() != OrgLevel.MERCHANT) {
                continue;
            }
            String mid = r.getMerchantId() != null ? r.getMerchantId().trim() : "";
            if (mid.isEmpty()) {
                continue;
            }
            if (allowedMerchantCodes != null) {
                if (!allowedMerchantCodes.contains(mid)) {
                    continue;
                }
            } else if (!userSubtree.contains(merchant.getId())) {
                continue;
            }
            Map<String, Object> dr = toDistributionRow(r);
            OrgUnit cur = parentOrg(merchant.getParentId(), idToOu);
            while (cur != null && userSubtree.contains(cur.getId())) {
                if (!isDistributionRollupLevel(cur.getOrgLevel())) {
                    cur = parentOrg(cur.getParentId(), idToOu);
                    continue;
                }
                if (!levelFilter.isEmpty() && cur.getOrgLevel() != null
                        && !levelFilter.equalsIgnoreCase(cur.getOrgLevel().name())) {
                    cur = parentOrg(cur.getParentId(), idToOu);
                    continue;
                }
                final LocalDate runCalcDt = r.getCalcDt();
                final String rollupOrgCode = cur.getCode();
                String key = Objects.requireNonNullElse(rollupOrgCode, "") + "|" + runCalcDt;
                DistributionAgg agg = aggMap.computeIfAbsent(key,
                        k -> new DistributionAgg(runCalcDt, rollupOrgCode));
                agg.merge(r, dr, cur.getOrgLevel());
                cur = parentOrg(cur.getParentId(), idToOu);
            }
        }

        List<Map<String, Object>> allRows = new ArrayList<>();
        for (DistributionAgg agg : aggMap.values()) {
            if (agg.rowOrgCode == null || agg.rowOrgCode.isBlank()) {
                continue;
            }
            OrgUnit rowOrg = orgUnitRepository.findByCode(agg.rowOrgCode).orElse(null);
            if (rowOrg == null) {
                continue;
            }
            if (searchCompId != null && !searchCompId.isBlank()) {
                if (rowOrg.getCode() == null || !rowOrg.getCode().contains(searchCompId.trim())) {
                    continue;
                }
            }
            if (searchCompNm != null && !searchCompNm.isBlank()) {
                if (rowOrg.getName() == null || !rowOrg.getName().contains(searchCompNm.trim())) {
                    continue;
                }
            }
            allRows.add(toAggregatedDistributionRow(rowOrg, agg, idToOu));
        }

        allRows.sort(mapRowsCalcDtPrimaryComparator(searchOrderDir)
                .thenComparing(m -> String.valueOf(m.getOrDefault("compId", ""))));

        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(allRows.size(), from + Math.max(1, size));
        List<Map<String, Object>> rows = new ArrayList<>();
        if (from < allRows.size()) {
            rows.addAll(allRows.subList(from, to));
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(allRows.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) allRows.size() / Math.max(1, size))));
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    /**
     * 가맹점정산내역: 정산일({@code calc_dt}) 구간의 정산실행 결과({@link SettlementRun})를 행으로 반환합니다.
     * 본사·총판·지사·대리점·영업점 등 유통 구간 수익은 {@link #distributionList} 에서 동일 실행분을 집계합니다.
     */
    @GetMapping("/franchiseList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> franchiseList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        List<SettlementRun> runs = settlementCalcService.listRuns(fromDate, toDate);
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (SettlementRun r : runs) {
            String mid = r.getMerchantId();
            if (mid == null || mid.isBlank()) {
                continue;
            }
            if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (allowedMerchants != null && !allowedMerchants.contains(mid.trim())) {
                continue;
            }
            Map<String, Object> row = toFranchiseSettlementRunRow(r);
            if (searchCompId != null && !searchCompId.isBlank()) {
                String compId = String.valueOf(row.getOrDefault("compId", ""));
                if (!compId.contains(searchCompId.trim())) {
                    continue;
                }
            }
            if (searchCompNm != null && !searchCompNm.isBlank()) {
                String compNm = String.valueOf(row.getOrDefault("compNm", ""));
                if (!compNm.contains(searchCompNm.trim())) {
                    continue;
                }
            }
            allRows.add(row);
        }
        allRows.sort(mapRowsCalcDtPrimaryComparator(searchOrderDir)
                .thenComparing(m -> String.valueOf(m.getOrDefault("compId", "")))
                .thenComparing(m -> String.valueOf(m.getOrDefault("trnId", ""))));

        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(allRows.size(), from + Math.max(1, size));
        List<Map<String, Object>> rows = new ArrayList<>();
        if (from < allRows.size()) {
            rows.addAll(allRows.subList(from, to));
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(allRows.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) allRows.size() / Math.max(1, size))));
        attachFeeCurrencyMeta(pr);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    /**
     * 보증금(지급보류 적치) 내역 — 지급보류(Y) 가맹점의 정산 실행 행만(가맹점정산내역과 동일 컬럼 + 비고).
     */
    @GetMapping("/payoutHoldList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> payoutHoldList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                buildPayoutHoldListPage(authentication, searchFromDate, searchToDate, searchCompNm, searchCompId, searchOrderDir, page, size)));
    }

    /**
     * 레거시 경로 — {@link #payoutHoldList} 와 동일(보증금내역 화면·클라이언트 호환). 롤링 담보는 {@link #collateralList} 사용.
     */
    @GetMapping("/holdList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> holdList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                buildPayoutHoldListPage(authentication, searchFromDate, searchToDate, searchCompNm, searchCompId, searchOrderDir, page, size)));
    }

    private PageResult<Map<String, Object>> buildPayoutHoldListPage(
            Authentication authentication,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchCompNm,
            String searchCompId,
            String searchOrderDir,
            int page,
            int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return emptyPage(page, size);
        }
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        List<SettlementRun> runs = settlementCalcService.listRuns(fromDate, toDate);
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (SettlementRun r : runs) {
            String mid = r.getMerchantId();
            if (mid == null || mid.isBlank()) {
                continue;
            }
            if (!"Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (allowedMerchants != null && !allowedMerchants.contains(mid.trim())) {
                continue;
            }
            Map<String, Object> row = toFranchiseSettlementRunRow(r);
            if (searchCompId != null && !searchCompId.isBlank()) {
                String compId = String.valueOf(row.getOrDefault("compId", ""));
                if (!compId.contains(searchCompId.trim())) {
                    continue;
                }
            }
            if (searchCompNm != null && !searchCompNm.isBlank()) {
                String compNm = String.valueOf(row.getOrDefault("compNm", ""));
                if (!compNm.contains(searchCompNm.trim())) {
                    continue;
                }
            }
            allRows.add(row);
        }
        allRows.sort(mapRowsCalcDtPrimaryComparator(searchOrderDir)
                .thenComparing(m -> String.valueOf(m.getOrDefault("compId", "")))
                .thenComparing(m -> String.valueOf(m.getOrDefault("trnId", ""))));

        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(allRows.size(), from + Math.max(1, size));
        List<Map<String, Object>> rows = new ArrayList<>();
        if (from < allRows.size()) {
            rows.addAll(allRows.subList(from, to));
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(allRows.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) allRows.size() / Math.max(1, size))));
        attachFeeCurrencyMeta(pr);
        return pr;
    }

    /**
     * 정산보류 적치 건을 가맹점정산내역에 표시되도록 해제합니다. 가맹점 설정의 지급보류(Y)는 변경하지 않습니다.
     */
    @PostMapping("/payoutHold/release")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> releasePayoutHold(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("조회 가능한 가맹점이 없습니다.", "FORBIDDEN"));
        }
        List<Long> ids = new ArrayList<>();
        Object raw = body != null ? body.get("settlementRunIds") : null;
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Number n) {
                    ids.add(n.longValue());
                } else if (o != null) {
                    try {
                        ids.add(Long.parseLong(o.toString().trim()));
                    } catch (NumberFormatException ignored) {
                        /* skip */
                    }
                }
            }
        }
        if (ids.isEmpty() && body != null && body.get("settlementRunId") != null) {
            try {
                ids.add(Long.parseLong(body.get("settlementRunId").toString().trim()));
            } catch (NumberFormatException ignored) {
                /* skip */
            }
        }
        if (ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("settlementRunIds 또는 settlementRunId가 필요합니다.", "BAD_REQUEST"));
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String stamp = LocalDateTime.now(SEOUL).format(dtf);
        int released = 0;
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            SettlementRun r = settlementRunRepository.findById(id).orElse(null);
            if (r == null) {
                continue;
            }
            String mid = r.getMerchantId() != null ? r.getMerchantId().trim() : "";
            if (mid.isEmpty()) {
                continue;
            }
            if (allowedMerchants != null && !allowedMerchants.contains(mid)) {
                return ResponseEntity.ok(ApiResponse.fail("해당 정산 건에 대한 권한이 없습니다: " + id, "FORBIDDEN"));
            }
            if (!"Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            r.setPayoutHoldYn("N");
            String prev = r.getPayoutHoldRemark() != null ? r.getPayoutHoldRemark().trim() : "";
            String suffix = "해제됨(" + stamp + "): 가맹점정산내역 반영";
            r.setPayoutHoldRemark(prev.isEmpty() ? suffix : prev + " | " + suffix);
            settlementRunRepository.save(r);
            released++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("releasedCount", released);
        out.put("requestedCount", ids.size());
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    @GetMapping("/recallMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> recallMng(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }
        List<Map<String, Object>> all = new ArrayList<>();
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);
        FeePolicy hqPolicy = resolveHqFeePolicy();
        FeeCurrencyRoundResolver feeResolver = resolveFeeCurrencyRoundResolver();
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        for (PgTrnsctn t : pgTrnsctnRepository.findForSettlement(null, fromDt, toDt)) {
            String s = t.getStatus() != null ? t.getStatus().trim() : "";
            boolean recallTarget = "20".equals(s) || "21".equals(s) || "22".equals(s) || "30".equals(s) || "31".equals(s);
            if (!recallTarget) continue;
            String compId = t.getMerchantId();
            if (compId == null || compId.isBlank()) {
                continue;
            }
            if (allowedMerchants != null && !allowedMerchants.contains(compId.trim())) {
                continue;
            }
            if (searchCompId != null && !searchCompId.isBlank() && !compId.contains(searchCompId.trim())) {
                continue;
            }
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            if (ou == null) continue;
            SettlementSetting feeVatSs = settlementSettingRepository.findByOrgUnitId(ou.getId()).orElse(null);
            BigDecimal txnAmtBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
            CommissionPolicy pol = resolveCommissionPolicyForMerchant(compId);
            String rowCur = t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW";
            FeeListRoundingPolicy feeListRp = feeResolver.forCurrency(rowCur);
            FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                    t, compId, pol, monthCbCountCache, tiersByPolicyId, feeVatSs, feeListRp);
            BigDecimal feeAmtBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), feeListRp);
            BigDecimal feeVatBd = br.feeVatBd();
            BigDecimal recallAmtBd = hqPolicy.recallIncludeFeeYn
                    ? txnAmtBd.add(feeAmtBd).add(feeVatBd).max(BigDecimal.ZERO)
                    : txnAmtBd.max(BigDecimal.ZERO);
            BigDecimal deductAmtBd = recallAmtBd.negate();
            Map<String, Object> m = new HashMap<>();
            m.put("trnId", t.getTrnId());
            m.put("calcDt", t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate().toString() : "");
            m.put("compId", compId);
            m.put("compNm", ou.getName() != null ? ou.getName() : compId);
            m.put("settleAmt", txnAmtBd);
            m.put("recallAmt", recallAmtBd);
            m.put("deductAmt", deductAmtBd);
            m.put("curType", rowCur);
            m.put("status", s);
            m.put("statusNm", switch (s) {
                case "20" -> "취소";
                case "21" -> "무효";
                case "22" -> "수동무효";
                case "30" -> "환불";
                case "31" -> "강제환불";
                default -> s;
            });
            m.put("feeIncludedYn", hqPolicy.recallIncludeFeeYn ? "Y" : "N");
            m.put("vatAppliedYn", br.feeVatBd().signum() > 0 ? "Y" : "N");
            all.add(m);
        }
        all.sort(mapRowsCalcDtPrimaryComparator(searchOrderDir));
        PageResult<Map<String, Object>> prRecall = pageOf(all, page, size);
        attachFeeCurrencyMeta(prRecall);
        return ResponseEntity.ok(ApiResponse.ok(prRecall));
    }

    /** 환수금관리: 정산 후 환불 등으로 자동 등록된 환수 대기·차감 내역 */
    @GetMapping("/recoveryList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> recoveryList(
            Authentication authentication,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }
        Specification<SettlementRecovery> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (allowedMerchants != null) {
                parts.add(root.get("merchantId").in(allowedMerchants));
            }
            if (searchCompId != null && !searchCompId.isBlank()) {
                String esc = escapeSqlLike(searchCompId.trim());
                parts.add(cb.like(cb.lower(root.get("merchantId")), "%" + esc.toLowerCase(Locale.ROOT) + "%", '\\'));
            }
            return cb.and(parts.toArray(new Predicate[0]));
        };
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        Pageable pageable = PageRequest.of(p - 1, s, Sort.by(sortDirectionFromSearchOrderDir(searchOrderDir), "id"));
        Page<SettlementRecovery> slice = settlementRecoveryRepository.findAll(spec, pageable);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SettlementRecovery r : slice.getContent()) {
            String compId = r.getMerchantId();
            OrgUnit ou = compId != null ? orgUnitRepository.findByCode(compId).orElse(null) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("compId", compId);
            m.put("compNm", ou != null ? ou.getName() : compId);
            m.put("curType", resolveMerchantStatementCurrency(compId));
            m.put("trnId", r.getTrnId());
            m.put("recallAmount", r.getRecallAmount() != null ? r.getRecallAmount() : 0L);
            m.put("remainingAmount", r.getRemainingAmount() != null ? r.getRemainingAmount() : 0L);
            m.put("appliedAmount", r.getAppliedAmount() != null ? r.getAppliedAmount() : 0L);
            m.put("status", r.getStatus());
            m.put("reasonCode", r.getReasonCode());
            m.put("feeIncludedYn", r.getFeeIncludedYn());
            m.put("vatAppliedYn", r.getVatAppliedYn());
            m.put("memo", r.getMemo() != null ? r.getMemo() : "");
            m.put("createdAt", r.getCreatedAt() != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r.getCreatedAt()) : "");
            rows.add(m);
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(p);
        pr.setSize(s);
        pr.setTotalElements(slice.getTotalElements());
        pr.setTotalPages(Math.max(1, slice.getTotalPages()));
        attachFeeCurrencyMeta(pr);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    /** 미수금관리: 수동 등록 미수금 목록 */
    @GetMapping("/receivableList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> receivableList(
            Authentication authentication,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(receivableListCore(authentication, searchCompId, searchOrderDir, page, size)));
    }

    private PageResult<Map<String, Object>> receivableListCore(
            Authentication authentication,
            String searchCompId,
            String searchOrderDir,
            int page,
            int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return emptyPage(page, size);
        }
        Specification<MerchantReceivable> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (allowedMerchants != null) {
                parts.add(root.get("merchantId").in(allowedMerchants));
            }
            if (searchCompId != null && !searchCompId.isBlank()) {
                String esc = escapeSqlLike(searchCompId.trim());
                parts.add(cb.like(cb.lower(root.get("merchantId")), "%" + esc.toLowerCase(Locale.ROOT) + "%", '\\'));
            }
            return cb.and(parts.toArray(new Predicate[0]));
        };
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        Pageable pageable = PageRequest.of(p - 1, s, Sort.by(sortDirectionFromSearchOrderDir(searchOrderDir), "id"));
        Page<MerchantReceivable> slice = merchantReceivableRepository.findAll(spec, pageable);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MerchantReceivable r : slice.getContent()) {
            String compId = r.getMerchantId();
            OrgUnit ou = compId != null ? orgUnitRepository.findByCode(compId).orElse(null) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("compId", compId);
            m.put("compNm", ou != null ? ou.getName() : compId);
            m.put("curType", resolveMerchantStatementCurrency(compId));
            m.put("title", r.getTitle() != null ? r.getTitle() : "");
            m.put("totalAmount", r.getTotalAmount() != null ? r.getTotalAmount() : 0L);
            m.put("remainingAmount", r.getRemainingAmount() != null ? r.getRemainingAmount() : 0L);
            m.put("appliedAmount", r.getAppliedAmount() != null ? r.getAppliedAmount() : 0L);
            m.put("status", r.getStatus());
            m.put("reasonCode", r.getReasonCode());
            m.put("memo", r.getMemo() != null ? r.getMemo() : "");
            m.put("createdBy", r.getCreatedBy() != null ? r.getCreatedBy() : "");
            m.put("createdAt", r.getCreatedAt() != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r.getCreatedAt()) : "");
            rows.add(m);
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(p);
        pr.setSize(s);
        pr.setTotalElements(slice.getTotalElements());
        pr.setTotalPages(Math.max(1, slice.getTotalPages()));
        attachFeeCurrencyMeta(pr);
        return pr;
    }

    /** 미수금 수동 등록 (차지백·과태료 등 reason_code 로 구분) */
    @PostMapping("/receivable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receivableCreate(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        String compId = body.get("compId") != null ? String.valueOf(body.get("compId")).trim() : "";
        if (compId.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("업체코드(compId)는 필수입니다.", "VALIDATION"));
        }
        long amount;
        try {
            amount = Long.parseLong(String.valueOf(body.getOrDefault("amount", "0")).trim());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail("금액(amount) 형식이 올바르지 않습니다.", "VALIDATION"));
        }
        if (amount <= 0) {
            return ResponseEntity.ok(ApiResponse.fail("금액은 0보다 커야 합니다.", "VALIDATION"));
        }
        if (orgUnitRepository.findByCode(compId).isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("존재하지 않는 업체코드입니다.", "NOT_FOUND"));
        }
        boolean admin = authentication != null && authentication.getPrincipal() instanceof AppUser au
                && "ADMIN".equalsIgnoreCase(au.getRole());
        if (!admin) {
            if (authentication.getPrincipal() instanceof AppUser actor
                    && !orgPagePermissionService.canManuallyManageMerchantReceivable(actor)) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "미수금 수동 등록은 본사권한설정에서 「미수금관리」화면 권한이 수정(M) 이상인 계정만 가능합니다.", "FORBIDDEN"));
            }
            Set<String> vis = orgAccessService.visibleMerchantCompCodes(authentication);
            if (vis.isEmpty() || !vis.contains(compId.trim())) {
                return ResponseEntity.ok(ApiResponse.fail("선택한 가맹점에 대한 등록 권한이 없습니다.", "FORBIDDEN"));
            }
        }
        String username = (authentication != null && authentication.getPrincipal() instanceof AppUser u)
                ? u.getUsername() : "";
        String title = body.get("title") != null ? String.valueOf(body.get("title")).trim() : "미수금";
        String reasonCode = body.get("reasonCode") != null ? String.valueOf(body.get("reasonCode")).trim() : "MANUAL";
        String memo = body.get("memo") != null ? String.valueOf(body.get("memo")).trim() : "";
        try {
            MerchantReceivable r = settlementArrearsService.createReceivable(compId, amount, title, reasonCode, memo, username);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "id", r.getId(),
                    "compId", compId,
                    "amount", amount,
                    "message", "등록되었습니다."
            )));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    @PostMapping("/receivable/{id}/writeOff")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receivableWriteOff(
            Authentication authentication,
            @PathVariable long id) {
        MerchantReceivable r = merchantReceivableRepository.findById(id).orElse(null);
        if (r == null) {
            return ResponseEntity.ok(ApiResponse.fail("미수금을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (authentication != null && authentication.getPrincipal() instanceof AppUser actorW
                && !orgPagePermissionService.canManuallyManageMerchantReceivable(actorW)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "미수금 처리는 본사권한설정에서 「미수금관리」화면 권한이 수정(M) 이상인 계정만 가능합니다.", "FORBIDDEN"));
        }
        if (!canAccessReceivable(authentication, r.getMerchantId())) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        settlementArrearsService.writeOffReceivable(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "id", id)));
    }

    @PostMapping("/receivable/{id}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receivableCancel(
            Authentication authentication,
            @PathVariable long id) {
        MerchantReceivable r = merchantReceivableRepository.findById(id).orElse(null);
        if (r == null) {
            return ResponseEntity.ok(ApiResponse.fail("미수금을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (authentication != null && authentication.getPrincipal() instanceof AppUser actorC
                && !orgPagePermissionService.canManuallyManageMerchantReceivable(actorC)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "미수금 처리는 본사권한설정에서 「미수금관리」화면 권한이 수정(M) 이상인 계정만 가능합니다.", "FORBIDDEN"));
        }
        if (!canAccessReceivable(authentication, r.getMerchantId())) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        try {
            settlementArrearsService.cancelReceivable(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "id", id)));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    private boolean canAccessReceivable(Authentication authentication, String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        if (authentication != null && authentication.getPrincipal() instanceof AppUser au
                && "ADMIN".equalsIgnoreCase(au.getRole())) {
            return true;
        }
        Set<String> vis = orgAccessService.visibleMerchantCompCodes(authentication);
        return vis != null && vis.contains(merchantId.trim());
    }

    /** 수수료내역: 가맹점 거래 1건마다 본사 기본정책의 모든 수수료 항목 계산 표시 (DB 페이징 — 전량 적재 금지) */
    @GetMapping("/feeList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> feeList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);

        final Set<String> merchantNameFilter;
        if (searchCompNm != null && !searchCompNm.isBlank()) {
            Set<String> nm = new HashSet<>();
            for (OrgUnit ou : orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, searchCompNm.trim())) {
                if (ou.getCode() == null || ou.getCode().isBlank()) {
                    continue;
                }
                String code = ou.getCode().trim();
                if (allowedMerchants == null || allowedMerchants.contains(code)) {
                    nm.add(code);
                }
            }
            if (nm.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
            }
            merchantNameFilter = nm;
        } else {
            merchantNameFilter = null;
        }

        Specification<PgTrnsctn> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            parts.add(cb.between(root.get("createdAt"), fromDt, toDt));
            parts.add(cb.isNotNull(root.get("merchantId")));
            parts.add(cb.notEqual(root.get("merchantId"), ""));
            if (allowedMerchants != null) {
                parts.add(root.get("merchantId").in(allowedMerchants));
            }
            if (merchantNameFilter != null) {
                parts.add(root.get("merchantId").in(merchantNameFilter));
            }
            if (searchCompId != null && !searchCompId.isBlank()) {
                String esc = escapeSqlLike(searchCompId.trim());
                parts.add(cb.like(root.get("merchantId"), "%" + esc + "%", '\\'));
            }
            Subquery<Long> ouExists = query.subquery(Long.class);
            Root<OrgUnit> ouRoot = ouExists.from(OrgUnit.class);
            ouExists.select(cb.literal(1L));
            ouExists.where(cb.equal(ouRoot.get("code"), root.get("merchantId")));
            parts.add(cb.exists(ouExists));
            return cb.and(parts.toArray(new Predicate[0]));
        };

        int pageSize = Math.min(500, Math.max(1, size));
        int pageOneBased = Math.max(1, page);
        Pageable pageable = PageRequest.of(pageOneBased - 1, pageSize,
                Sort.by(sortDirectionFromSearchOrderDir(searchOrderDir), "createdAt")
                        .and(Sort.by(sortDirectionFromSearchOrderDir(searchOrderDir), "trnId")));
        Page<PgTrnsctn> slice = pgTrnsctnRepository.findAll(spec, pageable);

        FeeCurrencyRoundResolver feeResolver = resolveFeeCurrencyRoundResolver();
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();

        List<String> mids = slice.getContent().stream()
                .map(PgTrnsctn::getMerchantId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        Map<String, PayListRowContext> ctxByMerchant = mids.isEmpty()
                ? Collections.emptyMap()
                : payListService.buildPayListRowContextMap(mids);
        Map<String, CommissionPolicy> polCache = new HashMap<>();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (PgTrnsctn t : slice.getContent()) {
            if (t.getMerchantId() == null || t.getMerchantId().isBlank()) {
                continue;
            }
            rows.add(buildFeeListRowMap(t, monthCbCountCache, tiersByPolicyId, ctxByMerchant, polCache, feeResolver));
        }

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(slice.getNumber() + 1);
        pr.setSize(slice.getSize());
        pr.setTotalElements(slice.getTotalElements());
        pr.setTotalPages(Math.max(1, slice.getTotalPages()));
        attachFeeCurrencyMeta(pr);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    private static String escapeSqlLike(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 수수료내역 가맹점 차감 행 — 취소·무효·환불·실패 등.
     * 지급예상액은 0, 총수수료·부가세는 과금액(양수), 정산액은 −(총수수료+부가세).
     */
    private static boolean isFeeListMerchantDeductionStatus(String st) {
        if (st == null || st.isBlank()) {
            return false;
        }
        return switch (st.trim()) {
            case "20", "21", "22", "30", "31", "40", "41", "42", "F0", "99" -> true;
            default -> false;
        };
    }

    private Map<String, Object> buildFeeListRowMap(PgTrnsctn t,
                                                   Map<String, Long> monthCbCountCache,
                                                   Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
                                                   Map<String, PayListRowContext> ctxByMerchant,
                                                   Map<String, CommissionPolicy> polCache,
                                                   FeeCurrencyRoundResolver feeResolver) {
        String compId = t.getMerchantId().trim();
        PayListRowContext payCtx = ctxByMerchant.get(compId);
        SettlementSetting feeVatSs = payCtx != null ? payCtx.getSettlement() : null;
        CommissionPolicy pol = polCache.computeIfAbsent(compId, this::resolveCommissionPolicyForMerchant);
        Map<String, Object> payRow = PayListItemDto.from(t, payCtx);
        Object payCurDisp = payRow.get("currency");
        String payCurKey = payCurDisp != null && !String.valueOf(payCurDisp).isBlank()
                ? String.valueOf(payCurDisp).trim()
                : (t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW");
        FeeListRoundingPolicy feeListRp = feeResolver.forCurrency(payCurKey);
        BigDecimal amountBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        BigDecimal payRateBd = nz(pol.getPayRate());
        FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                t, compId, pol, monthCbCountCache, tiersByPolicyId, feeVatSs, feeListRp);
        String stRow = t.getStatus() != null ? t.getStatus().trim() : "";
        BigDecimal totalFeeMag = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), feeListRp);
        BigDecimal feeVatMag = FeeListRoundingPolicy.round(br.feeVatBd(), feeListRp);
        final BigDecimal totalFeeBd;
        final BigDecimal feeVatOut;
        final BigDecimal expectedPayoutBd;
        if ("10".equals(stRow)) {
            totalFeeBd = totalFeeMag;
            feeVatOut = feeVatMag;
            expectedPayoutBd = FeeListRoundingPolicy.round(amountBd.subtract(totalFeeBd).subtract(feeVatOut), feeListRp);
        } else if (isFeeListMerchantDeductionStatus(stRow)) {
            /* 차감 행: 지급예상 0. 총수수료·부가세는 과금 규모(양수), 정산액만 가맹 차감(음수). */
            expectedPayoutBd = FeeListRoundingPolicy.round(BigDecimal.ZERO, feeListRp);
            totalFeeBd = totalFeeMag;
            feeVatOut = feeVatMag;
        } else {
            totalFeeBd = totalFeeMag;
            feeVatOut = feeVatMag;
            expectedPayoutBd = FeeListRoundingPolicy.round(amountBd.subtract(totalFeeBd).subtract(feeVatOut), feeListRp);
        }
        BigDecimal rollingHoldEstBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.rollingHoldEst()), feeListRp);
        final BigDecimal settlementAmtBd;
        if ("10".equals(stRow)) {
            settlementAmtBd = FeeListRoundingPolicy.round(expectedPayoutBd.subtract(rollingHoldEstBd), feeListRp);
        } else if (isFeeListMerchantDeductionStatus(stRow)) {
            settlementAmtBd = FeeListRoundingPolicy.round(totalFeeMag.add(feeVatMag).negate(), feeListRp);
        } else {
            settlementAmtBd = FeeListRoundingPolicy.round(expectedPayoutBd.subtract(rollingHoldEstBd), feeListRp);
        }
        double extraFeesSum = br.extraFee1() + br.extraFee2() + br.extraFee3() + br.extraFee4();
        double txnFixedFeesSum;
        double pctFeesSum;
        if ("10".equals(stRow) || "21".equals(stRow) || "22".equals(stRow) || "30".equals(stRow) || "31".equals(stRow)
                || "40".equals(stRow) || "41".equals(stRow) || "42".equals(stRow)) {
            txnFixedFeesSum = br.perTxFee();
            pctFeesSum = br.payFee() + br.usdtFee() + br.fxFee() + extraFeesSum;
        } else {
            txnFixedFeesSum = 0d;
            pctFeesSum = 0d;
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("compNm", payRow.get("compNm"));
        m.put("compId", payRow.get("compId"));
        m.put("trnDate", payRow.get("trnDate"));
        m.put("trnTime", payRow.get("trnTime"));
        m.put("routeNo", payRow.get("routeNo"));
        m.put("chillTransactionId", payRow.get("chillTransactionId"));
        m.put("trnId", payRow.get("trnId"));
        m.put("status", t.getStatus());
        m.put("statusNm", payDivName(t.getStatus()));
        m.put("payDivNm", payRow.get("payDivNm"));
        m.put("chillPaymentStatus", payRow.get("chillPaymentStatus"));
        m.put("amount", amountBd);
        m.put("payCur", payCurKey);
        m.put("curType", payCurKey);
        m.put("policyCur", pol.getCurrencyCode() != null && !pol.getCurrencyCode().isBlank() ? pol.getCurrencyCode().trim() : "KRW");
        m.put("txnFixedFeesSum", feeListMoney(txnFixedFeesSum, feeListRp).doubleValue());
        m.put("pctFeesSum", feeListMoney(pctFeesSum, feeListRp).doubleValue());
        m.put("perTxFee", feeListMoney(br.perTxFee(), feeListRp).doubleValue());
        m.put("usageFee", feeListMoney(br.usageFee(), feeListRp).doubleValue());
        m.put("failFee", feeListMoney(br.failFee(), feeListRp).doubleValue());
        m.put("cancelFee", feeListMoney(br.cancelFee(), feeListRp).doubleValue());
        m.put("voidFee", feeListMoney(br.voidFee(), feeListRp).doubleValue());
        m.put("manualVoidFee", feeListMoney(br.manualVoidFee(), feeListRp).doubleValue());
        m.put("refundFee", feeListMoney(br.refundFee(), feeListRp).doubleValue());
        m.put("remittanceTransferFee", feeListMoney(br.remittanceTransferFee(), feeListRp).doubleValue());
        m.put("usdtTransferFeeUsd", feeListMoney(br.usdtTransferFeeUsd(), feeListRp).doubleValue());
        m.put("payFeeRate", PercentDecimalHelper.toPlainOneDecimal(payRateBd));
        m.put("payFee", feeListMoney(br.payFee(), feeListRp).doubleValue());
        m.put("usdtFeeRate", PercentDecimalHelper.toPlainOneDecimal(nz(pol.getFeeUsdt())));
        m.put("usdtFee", feeListMoney(br.usdtFee(), feeListRp).doubleValue());
        m.put("fxFeeRate", PercentDecimalHelper.toPlainOneDecimal(nz(pol.getFeeFx())));
        m.put("fxFee", feeListMoney(br.fxFee(), feeListRp).doubleValue());
        m.put("fee3dsRate", PercentDecimalHelper.toPlainAmountOneDecimal(nz(pol.getFee3dsRate())));
        m.put("fee3dsFee", feeListMoney(br.fee3dsFee(), feeListRp).doubleValue());
        m.put("settlementPerTxFee", feeListMoney(br.settlementPerTxFee(), feeListRp).doubleValue());
        m.put("chargebackFee", feeListMoney(br.chargebackFee(), feeListRp).doubleValue());
        m.put("rollingPctPlain", br.rollingPctPlain());
        m.put("rollingDays", br.rollingDays());
        m.put("rollingHoldEst", rollingHoldEstBd.doubleValue());
        m.put("extraFee1", feeListMoney(br.extraFee1(), feeListRp).doubleValue());
        m.put("extraFee2", feeListMoney(br.extraFee2(), feeListRp).doubleValue());
        m.put("extraFee3", feeListMoney(br.extraFee3(), feeListRp).doubleValue());
        m.put("extraFee4", feeListMoney(br.extraFee4(), feeListRp).doubleValue());
        m.put("extraFees", feeListMoney(extraFeesSum, feeListRp).doubleValue());
        m.put("totalFee", totalFeeBd.doubleValue());
        m.put("feeVat", feeVatOut.doubleValue());
        m.put("expectedPayout", expectedPayoutBd.doubleValue());
        m.put("settlementAmt", settlementAmtBd.doubleValue());
        m.put("vatAppliedYn", br.feeVatBd().signum() > 0 ? "Y" : "N");
        return m;
    }

    @GetMapping("/balanceMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> balanceMng(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(balanceListCore(authentication, searchCompId, searchCompNm, searchOrderDir, page, size, true)));
    }

    /** 잔액/미수금관리 수동 차감(선택 차감/직접입력 공용) */
    @PostMapping("/balance/deduct")
    public ResponseEntity<ApiResponse<Map<String, Object>>> balanceDeduct(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        String compId = body.get("compId") != null ? String.valueOf(body.get("compId")).trim() : "";
        if (compId.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("업체코드(compId)는 필수입니다.", "VALIDATION"));
        }
        long amount;
        try {
            amount = Long.parseLong(String.valueOf(body.getOrDefault("amount", "0")).trim());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail("차감 금액(amount) 형식이 올바르지 않습니다.", "VALIDATION"));
        }
        if (amount <= 0) {
            return ResponseEntity.ok(ApiResponse.fail("차감 금액은 0보다 커야 합니다.", "VALIDATION"));
        }
        if (orgUnitRepository.findByCode(compId).isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("존재하지 않는 업체코드입니다.", "NOT_FOUND"));
        }
        boolean admin = authentication != null && authentication.getPrincipal() instanceof AppUser au
                && "ADMIN".equalsIgnoreCase(au.getRole());
        if (!admin) {
            Set<String> vis = orgAccessService.visibleMerchantCompCodes(authentication);
            if (vis.isEmpty() || !vis.contains(compId.trim())) {
                return ResponseEntity.ok(ApiResponse.fail("선택한 가맹점에 대한 차감 권한이 없습니다.", "FORBIDDEN"));
            }
        }
        String username = (authentication != null && authentication.getPrincipal() instanceof AppUser u)
                ? u.getUsername() : "";
        String memo = body.get("memo") != null ? String.valueOf(body.get("memo")).trim() : "";
        BalanceDeduction d = new BalanceDeduction();
        d.setMerchantId(compId);
        d.setAmount(amount);
        d.setMemo(memo.isBlank() ? "수동 차감" : memo);
        d.setCreatedBy(username);
        balanceDeductionRepository.save(d);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "success", true,
                "message", "차감 처리되었습니다.",
                "compId", compId,
                "amount", amount
        )));
    }

    @GetMapping("/balanceList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> balanceList(
            Authentication authentication,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(balanceListCore(authentication, searchCompId, searchCompNm, searchOrderDir, page, size, false)));
    }

    @GetMapping("/unpaidMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> unpaidMng(
            Authentication authentication,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> pr = receivableListCore(authentication, searchCompId, searchOrderDir, page, size);
        /* 화면 호환: settleAmt·deductCnt = 잔여 미수금, deductStatus = 상태. 업체명 검색은 클라이언트 필터 또는 compId 검색 사용 */
        for (Map<String, Object> m : pr.getList()) {
            long rem = asLong(m.get("remainingAmount"));
            m.put("settleAmt", rem);
            m.put("deductCnt", rem);
            m.put("deductStatus", String.valueOf(m.getOrDefault("status", "")));
        }
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    /**
     * 담보금(롤링) 내역 — 가맹점·적용일·보류 영업일·해지일·남은 영업일·상태.
     * 비율·일수는 본사 수수료 정책 또는 가맹 정산설정(보류율 본사따름 N)에서 결정.
     */
    @GetMapping("/collateralList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> collateralList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchStatus,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                collateralLedgerService.search(searchFromDate, searchToDate, searchCompId, searchCompNm, searchStatus, searchOrderDir, page, size)));
    }

    @GetMapping("/execute")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> executeList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<SettlementRun> list = new ArrayList<>(settlementCalcService.listRuns(searchFromDate, searchToDate));
        Sort.Direction sd = sortDirectionFromSearchOrderDir(searchOrderDir);
        Comparator<SettlementRun> byDt = Comparator.comparing(SettlementRun::getCalcDt, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<SettlementRun> byId = Comparator.comparing(SettlementRun::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        if (sd == Sort.Direction.DESC) {
            list.sort(byDt.reversed().thenComparing(byId.reversed()));
        } else {
            list.sort(byDt.thenComparing(byId));
        }
        int from = (page - 1) * size;
        int to = Math.min(from + size, list.size());
        List<Map<String, Object>> rows = list.subList(from, to).stream()
                .map(r -> toMap(r, searchFromDate, searchToDate))
                .collect(Collectors.toList());
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(list.size());
        pr.setTotalPages(size > 0 ? (int) Math.ceil((double) list.size() / size) : 1);
        attachFeeCurrencyMeta(pr);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    @PostMapping("/execute/run")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> executeRun(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String merchantId) {
        /* 기간 지정 시: 기존 수동 실행(레거시 유지) */
        if (fromDate != null || toDate != null) {
            LocalDate runFrom = fromDate != null ? fromDate : LocalDate.now().minusDays(1);
            LocalDate runTo = toDate != null ? toDate : LocalDate.now();
            List<SettlementRun> runs = settlementCalcService.execute(runFrom, runTo, merchantId);
            List<Map<String, Object>> list = runs.stream().map(r -> toMap(r, runFrom, runTo)).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.ok(list));
        }

        /* 기간 미지정 시: calcCycle·AUTO·마감시간 등과 동일 규칙으로 자동 실행(스케줄 배치와 공유) */
        LocalDate today = LocalDate.now(SEOUL);
        List<SettlementRun> allRuns = settlementAutoRunService.runDueSettlements(today, merchantId, true);
        List<Map<String, Object>> list = allRuns.stream().map(r -> toMap(r, null, null)).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /**
     * 정산실행 그리드용 행 맵. {@link SettlementRun#getPeriodFrom()}/{@link SettlementRun#getPeriodTo()} 가 있으면
     * period* 키에 반영합니다. {@code targetPeriodText}는 정산주기 RT면 거래번호·승인번호·마감시각 한 줄, 그 외에는 기간·마감 문구 또는 조회 기간·정산일입니다.
     */
    private Map<String, Object> toMap(SettlementRun r, LocalDate queryFrom, LocalDate queryTo) {
        Map<String, Object> m = new HashMap<>();
        m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : null);
        String mid = r.getMerchantId();
        m.put("compId", mid);
        OrgUnit ouExec = mid != null ? orgUnitRepository.findByCode(mid).orElse(null) : null;
        m.put("compNm", ouExec != null ? ouExec.getName() : (mid != null ? mid : "-"));
        m.put("curType", resolveMerchantStatementCurrency(mid));
        m.put("targetAmt", r.getApproveAmt() != null && r.getCancelAmt() != null ? r.getApproveAmt().subtract(r.getCancelAmt()).toString() : "0");
        m.put("status", r.getStatus());
        m.put("payAmount", r.getPayAmt() != null ? r.getPayAmt().longValue() : 0);
        m.put("approveAmt", r.getApproveAmt() != null ? r.getApproveAmt().longValue() : 0);
        m.put("cancelAmt", r.getCancelAmt() != null ? r.getCancelAmt().longValue() : 0);
        m.put("totalFee", r.getTotalFee() != null ? r.getTotalFee().longValue() : 0);
        m.put("rollingReserveAmt", r.getRollingReserveAmt() != null ? r.getRollingReserveAmt().longValue() : 0);
        String calcCycleRaw = "";
        if (ouExec != null) {
            Optional<SettlementSetting> ssOpt = settlementSettingRepository.findByOrgUnitId(ouExec.getId());
            if (ssOpt.isPresent()) {
                SettlementSetting ss = ssOpt.get();
                calcCycleRaw = ss.getCalcCycle() != null ? ss.getCalcCycle() : "";
                m.put("calcCycle", calcCycleRaw);
                m.put("calcMethod", labelCalcProcType(ss.getCalcProcType()));
            } else {
                m.put("calcCycle", "");
                m.put("calcMethod", "");
            }
            m.put("pgRootNo", resolveMerchantPgRootNo(ouExec.getId()));
        } else {
            m.put("calcCycle", "");
            m.put("calcMethod", "");
            m.put("pgRootNo", "-");
        }
        m.put("periodFrom", r.getPeriodFrom() != null ? r.getPeriodFrom().toString() : null);
        m.put("periodTo", r.getPeriodTo() != null ? r.getPeriodTo().toString() : null);
        m.put("periodEndAt", r.getPeriodEndAt() != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r.getPeriodEndAt()) : null);
        m.put("targetPeriodText", buildSettlementTargetPeriodLabel(r, queryFrom, queryTo, calcCycleRaw));
        return m;
    }

    private static String labelCalcProcType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "AUTO" -> "자동";
            case "MANUAL" -> "수동";
            case "FUMBANKING" -> "펌뱅킹";
            default -> raw.trim();
        };
    }

    /**
     * 정산대상기간 표시. RT(건당)는 수수료내역과 맞춰 거래번호·승인번호·마감시각 한 줄만 사용하고,
     * {@code yyyy-MM-dd ~ yyyy-MM-dd · 마감 …} 형식은 쓰지 않습니다.
     */
    private String buildSettlementTargetPeriodLabel(SettlementRun r, LocalDate queryFrom, LocalDate queryTo, String calcCycleRaw) {
        String norm = SettlementPeriodResolver.normalizeCalcCycle(calcCycleRaw != null ? calcCycleRaw : "");
        if (SettlementCycleTiming.isRtPerTransactionCode(norm)
                && r.getPeriodFrom() != null
                && r.getPeriodTo() != null
                && r.getPeriodFrom().equals(r.getPeriodTo())
                && r.getPeriodEndAt() != null) {
            return buildRtSettlementTargetPeriodLine(r);
        }
        return buildSettlementTargetPeriodLabelLegacy(r, queryFrom, queryTo);
    }

    private String buildRtSettlementTargetPeriodLine(SettlementRun r) {
        LocalDateTime closeAt = r.getPeriodEndAt();
        Optional<PgTrnsctn> txOpt = resolvePgTxnForRtRun(r);
        if (txOpt.isEmpty()) {
            return formatRtPeriodLine("-", "-", closeAt);
        }
        PgTrnsctn t = txOpt.get();
        String trn = blankToDash(t.getTrnId());
        String appr = firstNonBlank(t.getApprovalNo(), t.getChillTransactionId());
        return formatRtPeriodLine(trn, appr, closeAt);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "-";
    }

    private static String blankToDash(String s) {
        if (s == null || s.isBlank()) {
            return "-";
        }
        return s.trim();
    }

    private static String formatRtPeriodLine(String trnId, String approvalLabel, LocalDateTime closeAt) {
        String closeStr = closeAt != null
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(closeAt)
                : "-";
        return "거래번호 " + trnId + " / 승인번호 " + approvalLabel + " / 마감 " + closeStr;
    }

    /**
     * RT 건당 실행 행에 대응하는 승인 거래 1건을 찾습니다(마감 시각·금액으로 근접 매칭).
     */
    private Optional<PgTrnsctn> resolvePgTxnForRtRun(SettlementRun r) {
        if (r.getMerchantId() == null || r.getMerchantId().isBlank() || r.getPeriodEndAt() == null) {
            return Optional.empty();
        }
        String mid = r.getMerchantId().trim();
        LocalDateTime end = r.getPeriodEndAt();
        LocalDateTime winStart = end.minusSeconds(5);
        LocalDateTime winEnd = end.plusSeconds(5);
        List<PgTrnsctn> inWin = pgTrnsctnRepository.findForSettlement(mid, winStart, winEnd);
        List<PgTrnsctn> candidates = inWin.stream()
                .filter(t -> "10".equals(t.getStatus() != null ? t.getStatus().trim() : ""))
                .filter(t -> "Y".equalsIgnoreCase(String.valueOf(t.getSettledYn()).trim()))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> exactTime = candidates.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().equals(end))
                .findFirst();
        if (exactTime.isPresent()) {
            return exactTime;
        }
        BigDecimal ap = r.getApproveAmt();
        if (ap != null && ap.signum() > 0) {
            Optional<PgTrnsctn> byAmt = candidates.stream()
                    .filter(t -> {
                        BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
                        return amt.compareTo(ap) == 0;
                    })
                    .findFirst();
            if (byAmt.isPresent()) {
                return byAmt;
            }
        }
        return candidates.stream().min(Comparator.comparingLong(t -> {
            if (t.getCreatedAt() == null) {
                return Long.MAX_VALUE;
            }
            return Math.abs(Duration.between(t.getCreatedAt(), end).toNanos());
        }));
    }

    private static String buildSettlementTargetPeriodLabelLegacy(SettlementRun r, LocalDate queryFrom, LocalDate queryTo) {
        if (r.getPeriodFrom() != null && r.getPeriodTo() != null) {
            String base = r.getPeriodFrom() + " ~ " + r.getPeriodTo();
            if (r.getPeriodEndAt() != null) {
                return base + " · 마감 " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(r.getPeriodEndAt());
            }
            return base;
        }
        if (queryFrom != null && queryTo != null) {
            return queryFrom + " ~ " + queryTo;
        }
        if (r.getCalcDt() != null) {
            return "정산일 " + r.getCalcDt();
        }
        return "";
    }

    /** 정산실행(calcOne)과 동일: 가맹 정산설정에서 본사 담보율 미따름 시 보유율·보유일 */
    private record MerchantRollingEffective(BigDecimal rollingPct, int rollingDays) {}

    private MerchantRollingEffective resolveMerchantRollingEffective(String merchantId, CommissionPolicy policy) {
        BigDecimal[] rollingPctRef = new BigDecimal[]{ nz(policy.getRollingPct()) };
        int[] rollingDaysRef = new int[]{ policy.getRollingDays() != null ? policy.getRollingDays() : 0 };
        if (merchantId != null && !merchantId.isBlank()) {
            orgUnitRepository.findByCode(merchantId.trim()).ifPresent(ou ->
                    settlementSettingRepository.findByOrgUnitId(ou.getId()).ifPresent(ss -> {
                        if ("N".equalsIgnoreCase(ss.getHoldRateFollowHq() != null ? ss.getHoldRateFollowHq().trim() : "")) {
                            if (ss.getHoldRate() != null && ss.getHoldRate().compareTo(BigDecimal.ZERO) > 0) {
                                rollingPctRef[0] = ss.getHoldRate();
                            }
                            if (ss.getHoldDays() != null && ss.getHoldDays() > 0) {
                                rollingDaysRef[0] = ss.getHoldDays();
                            }
                        }
                    }));
        }
        return new MerchantRollingEffective(rollingPctRef[0], rollingDaysRef[0]);
    }

    /**
     * 가맹점정산내역: 정산 실행에 포함된 거래 구간으로 수수료내역과 같은 건별 합산을 보조하고,
     * 건당·정산건당·기타%·부가세·보유율은 실행 로직과 맞춥니다.
     */
    private void applyFranchiseSettlementFeeBreakdown(SettlementRun r, Map<String, Object> row, String compId) {
        if (compId == null || compId.isBlank()) {
            return;
        }
        LocalDateTime fromDt = r.resolvePeriodStartAt();
        LocalDateTime toDt = r.resolvePeriodEndAt();
        List<PgTrnsctn> txs = pgTrnsctnRepository.findForSettlement(compId.trim(), fromDt, toDt);

        FeeCurrencyRoundResolver feeResolver = resolveFeeCurrencyRoundResolver();
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        CommissionPolicy pol = resolveCommissionPolicyForMerchant(compId);
        MerchantRollingEffective rollingEff = resolveMerchantRollingEffective(compId, pol);
        SettlementSetting feeVatSs = orgUnitRepository.findByCode(compId.trim())
                .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                .orElse(null);

        int feeCnt = 0;
        BigDecimal sumExtra = BigDecimal.ZERO;
        for (PgTrnsctn t : txs) {
            String rowCur = t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW";
            FeeListRoundingPolicy feeListRp = feeResolver.forCurrency(rowCur);
            FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                    t, compId, pol, monthCbCountCache, tiersByPolicyId, feeVatSs, feeListRp);
            BigDecimal totalFeeBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), feeListRp);
            if (totalFeeBd.signum() > 0) {
                feeCnt++;
            }
            double ex = br.extraFee1() + br.extraFee2() + br.extraFee3() + br.extraFee4();
            sumExtra = sumExtra.add(feeListMoney(ex, feeListRp));
        }
        row.put("feeCnt", feeCnt);

        BigDecimal perTxTotal = nz(pol.getPerTxFee()).multiply(BigDecimal.valueOf(txs.size())).setScale(0, RoundingMode.HALF_UP);
        row.put("perTxFeeAmt", perTxTotal.longValue());

        BigDecimal settlePerTxTotal = nz(pol.getFeeSettlementPerTx()).multiply(BigDecimal.valueOf(txs.size())).setScale(0, RoundingMode.HALF_UP);
        row.put("settlementPerTxFeeAmt", settlePerTxTotal.longValue());

        row.put("extraFeesAmt", sumExtra.setScale(0, RoundingMode.HALF_UP).longValue());

        BigDecimal feeVat = MerchantFeeVatUtil.vatOnFeeAmount(nz(r.getTotalFee()), feeVatSs, 0);
        row.put("feeVat", feeVat.setScale(0, RoundingMode.HALF_UP).longValue());

        long hold = nz(r.getRollingReserveAmt()).longValue();
        BigDecimal netBd = nz(r.getApproveAmt()).subtract(nz(r.getCancelAmt()));
        if (rollingEff.rollingDays() > 0 && rollingEff.rollingPct().signum() > 0) {
            row.put("holdRate", rollingEff.rollingPct().setScale(4, RoundingMode.HALF_UP));
        } else if (netBd.signum() > 0 && hold > 0) {
            row.put("holdRate", BigDecimal.valueOf(hold).multiply(BigDecimal.valueOf(100))
                    .divide(netBd, 4, RoundingMode.HALF_UP));
        } else {
            row.put("holdRate", BigDecimal.ZERO);
        }
    }

    private String resolveMerchantPgRootNo(Long orgUnitId) {
        if (orgUnitId == null) {
            return "-";
        }
        List<MerchantPgBinding> binds = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        Optional<String> primary = binds.stream()
                .filter(b -> "Y".equalsIgnoreCase(String.valueOf(b.getOperationalYn())))
                .map(MerchantPgBinding::getRootNo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst();
        if (primary.isPresent()) {
            return primary.get();
        }
        return binds.stream()
                .map(MerchantPgBinding::getRootNo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("-");
    }

    /**
     * 가맹점정산 그리드와 동일 키로 {@link SettlementRun} 한 건을 매핑합니다.
     * 금액·공제수수료·보류액·지급액은 실행 저장값이며, 건수·부가세·건당·정산건당·기타%·보유율은 동일 집계 구간 거래로
     * 수수료내역·정산실행(calcOne) 규칙에 맞춰 채웁니다.
     */
    private Map<String, Object> toFranchiseSettlementRunRow(SettlementRun r) {
        Map<String, Object> row = new LinkedHashMap<>();
        String compId = r.getMerchantId() != null ? r.getMerchantId().trim() : "";
        OrgUnit ou = compId.isEmpty() ? null : orgUnitRepository.findByCode(compId).orElse(null);
        row.put("compId", compId);
        row.put("compNm", ou != null ? ou.getName() : (compId.isEmpty() ? "-" : compId));
        row.put("curType", resolveMerchantStatementCurrency(compId));
        String bizNo = "-";
        if (ou != null) {
            Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(ou.getId());
            if (mpOpt.isPresent()) {
                String rn = mpOpt.get().getRegNo();
                if (rn != null && !rn.isBlank()) {
                    bizNo = rn.trim();
                }
            }
        }
        row.put("bizNo", bizNo);
        BigDecimal approve = nz(r.getApproveAmt());
        BigDecimal cancel = nz(r.getCancelAmt());
        BigDecimal netBd = approve.subtract(cancel);
        long net = netBd.longValue();
        long fee = nz(r.getTotalFee()).longValue();
        long hold = nz(r.getRollingReserveAmt()).longValue();
        long settle = nz(r.getPayAmt()).longValue();
        row.put("amount", net);
        row.put("feeAmt", fee);
        row.put("holdAmt", hold);
        row.put("settleAmt", settle);
        if (netBd.signum() > 0 && fee > 0) {
            row.put("feeRate", BigDecimal.valueOf(fee).multiply(BigDecimal.valueOf(100))
                    .divide(netBd, 4, RoundingMode.HALF_UP));
        } else {
            row.put("feeRate", BigDecimal.ZERO);
        }
        applyFranchiseSettlementFeeBreakdown(r, row, compId);
        String calcCycle = "";
        if (ou != null) {
            calcCycle = settlementSettingRepository.findByOrgUnitId(ou.getId())
                    .map(ss -> ss.getCalcCycle() != null ? ss.getCalcCycle() : "")
                    .orElse("");
        }
        row.put("calcCycle", calcCycle);
        Long runId = r.getId();
        row.put("trnId", runId != null ? "RUN-" + runId : "-");
        row.put("payDivNm", "정산실행");
        row.put("payNo", runId != null ? String.valueOf(runId) : "-");
        row.put("payCard", "-");
        row.put("cardAprvNo", "-");
        row.put("payCardNo", "-");
        row.put("instalMonth", "-");
        row.put("payMethod", "-");
        row.put("corpNm", "-");
        row.put("pgNm", "-");
        row.put("terminalId", "-");
        row.put("productNm", "-");
        row.put("customerNm", "-");
        row.put("customerTel", "-");
        row.put("approveDt", "-");
        row.put("cancelDt", "-");
        row.put("payStatus", r.getStatus() != null ? r.getStatus() : "-");
        String regionalNm = "";
        String masterNm = "";
        String branchNm = "";
        OrgUnit cur = ou;
        for (int i = 0; i < 8 && cur != null; i++) {
            if (cur.getOrgLevel() != null) {
                switch (cur.getOrgLevel()) {
                    case REGIONAL -> regionalNm = nullToDashName(cur.getName());
                    case MASTER_DIST -> masterNm = nullToDashName(cur.getName());
                    case BRANCH -> branchNm = nullToDashName(cur.getName());
                    default -> {
                    }
                }
            }
            cur = cur.getParentId() != null ? orgUnitRepository.findById(cur.getParentId()).orElse(null) : null;
        }
        row.put("regionalNm", regionalNm.isBlank() ? "-" : regionalNm);
        row.put("masterNm", masterNm.isBlank() ? "-" : masterNm);
        row.put("branchNm", branchNm.isBlank() ? "-" : branchNm);
        String calcDtStr;
        if (r.getCreatedAt() != null) {
            calcDtStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r.getCreatedAt());
        } else if (r.getCalcDt() != null) {
            calcDtStr = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r.getCalcDt().atStartOfDay());
        } else {
            calcDtStr = "";
        }
        row.put("calcDt", calcDtStr);
        row.put("settlementRunId", runId != null ? runId : "");
        row.put("payoutHoldRemark", r.getPayoutHoldRemark() != null ? r.getPayoutHoldRemark() : "");
        return row;
    }

    private static String nullToDashName(String name) {
        return name != null && !name.isBlank() ? name.trim() : "";
    }

    private Map<String, Object> toDistributionRow(SettlementRun r) {
        Map<String, Object> m = new HashMap<>();
        String compId = r.getMerchantId();
        BigDecimal settleAmt = r.getPayAmt() != null ? r.getPayAmt() : BigDecimal.ZERO;
        DistributionFeeConfig cfg = distributionFeeConfigRepository.findByCompId(compId).orElse(null);
        BigDecimal hqRate = cfg != null && cfg.getHqRate() != null ? cfg.getHqRate() : BigDecimal.ZERO;
        BigDecimal regionalRate = cfg != null && cfg.getRegionalRate() != null ? cfg.getRegionalRate() : BigDecimal.ZERO;
        BigDecimal masterRate = cfg != null && cfg.getMasterRate() != null ? cfg.getMasterRate() : BigDecimal.ZERO;
        BigDecimal branchRate = cfg != null && cfg.getBranchRate() != null ? cfg.getBranchRate() : BigDecimal.ZERO;
        BigDecimal agencyRate = cfg != null && cfg.getAgencyRate() != null ? cfg.getAgencyRate() : BigDecimal.ZERO;
        BigDecimal salesOfficeRate = cfg != null && cfg.getSalesOfficeRate() != null ? cfg.getSalesOfficeRate() : BigDecimal.ZERO;
        m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : "");
        m.put("compId", compId);
        OrgUnit merchantOu = orgUnitRepository.findByCode(compId).orElse(null);
        m.put("compNm", merchantOu != null ? merchantOu.getName() : (compId != null ? compId : "-"));
        m.put("settleAmt", settleAmt.longValue());
        m.put("hqRate", hqRate);
        m.put("regionalRate", regionalRate);
        m.put("masterRate", masterRate);
        m.put("branchRate", branchRate);
        m.put("agencyRate", agencyRate);
        m.put("hqFee", pct(settleAmt, hqRate));
        m.put("regionalFee", pct(settleAmt, regionalRate));
        m.put("masterFee", pct(settleAmt, masterRate));
        m.put("branchFee", pct(settleAmt, branchRate));
        m.put("agencyFee", pct(settleAmt, agencyRate));
        m.put("salesOfficeFee", pct(settleAmt, salesOfficeRate));
        String hq = "", regional = "", master = "", branch = "", agency = "";
        OrgUnit cur = orgUnitRepository.findByCode(compId).orElse(null);
        for (int i = 0; i < 8 && cur != null; i++) {
            if (cur.getOrgLevel() != null) {
                switch (cur.getOrgLevel()) {
                    case HEADQUARTERS -> hq = cur.getName();
                    case REGIONAL -> regional = cur.getName();
                    case MASTER_DIST -> master = cur.getName();
                    case BRANCH -> branch = cur.getName();
                    case AGENCY, SALES_OFFICE -> agency = cur.getName();
                    default -> {}
                }
            }
            cur = cur.getParentId() != null ? orgUnitRepository.findById(cur.getParentId()).orElse(null) : null;
        }
        m.put("hqNm", hq);
        m.put("regionalNm", regional);
        m.put("masterNm", master);
        m.put("branchNm", branch);
        m.put("agencyNm", agency);
        return m;
    }

    private long pct(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null) return 0L;
        return amount.multiply(rate).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP).longValue();
    }

    private static final class DistributionAgg {
        final LocalDate calcDt;
        final String rowOrgCode;
        long settleAmt;
        long hqFee;
        long regionalFee;
        long masterFee;
        long branchFee;
        long agencyFee;
        /** 가맹 정산 건수(승인 건수 표시용) */
        long runCnt;
        long approveAmtSum;
        long cancelAmtSum;
        /** 취소 금액이 있는 가맹 건수 */
        long cancelRunCnt;
        long aprvFeeSum;
        long canFeeSum;

        DistributionAgg(LocalDate calcDt, String rowOrgCode) {
            this.calcDt = calcDt;
            this.rowOrgCode = rowOrgCode;
        }

        void merge(SettlementRun r, Map<String, Object> dr, OrgLevel rollupLevel) {
            if (rollupLevel == null) {
                return;
            }
            settleAmt += asLongStatic(dr.get("settleAmt"));
            long slice = feeSliceForRollup(dr, rollupLevel);
            switch (rollupLevel) {
                case HEADQUARTERS -> hqFee += slice;
                case REGIONAL -> regionalFee += slice;
                case MASTER_DIST -> masterFee += slice;
                case BRANCH -> branchFee += slice;
                case AGENCY, SALES_OFFICE -> agencyFee += slice;
                default -> { }
            }
            runCnt++;
            BigDecimal ap = r.getApproveAmt() != null ? r.getApproveAmt() : BigDecimal.ZERO;
            BigDecimal ca = r.getCancelAmt() != null ? r.getCancelAmt() : BigDecimal.ZERO;
            approveAmtSum += ap.longValue();
            cancelAmtSum += ca.longValue();
            if (ca.signum() > 0) {
                cancelRunCnt++;
            }
            long feeSum = slice;
            BigDecimal denom = ap.add(ca);
            if (denom.signum() == 0) {
                aprvFeeSum += feeSum;
            } else {
                aprvFeeSum += BigDecimal.valueOf(feeSum).multiply(ap).divide(denom, 0, RoundingMode.HALF_UP).longValue();
                canFeeSum += BigDecimal.valueOf(feeSum).multiply(ca).divide(denom, 0, RoundingMode.HALF_UP).longValue();
            }
        }

        /** 유통망 행(rollup) 조직 단계에 해당하는 수수료 구간만 합산 — 타 단계 금액을 같은 행에 섞지 않음 */
        private static long feeSliceForRollup(Map<String, Object> dr, OrgLevel rollupLevel) {
            return switch (rollupLevel) {
                case HEADQUARTERS -> asLongStatic(dr.get("hqFee"));
                case REGIONAL -> asLongStatic(dr.get("regionalFee"));
                case MASTER_DIST -> asLongStatic(dr.get("masterFee"));
                case BRANCH -> asLongStatic(dr.get("branchFee"));
                case AGENCY -> asLongStatic(dr.get("agencyFee"));
                case SALES_OFFICE -> asLongStatic(dr.get("salesOfficeFee"));
                default -> 0L;
            };
        }

        private static long asLongStatic(Object v) {
            if (v == null) return 0L;
            if (v instanceof Number n) return n.longValue();
            try {
                return Long.parseLong(String.valueOf(v));
            } catch (Exception e) {
                return 0L;
            }
        }
    }

    private static boolean isDistributionRollupLevel(OrgLevel l) {
        if (l == null) return false;
        return l == OrgLevel.HEADQUARTERS || l == OrgLevel.REGIONAL || l == OrgLevel.MASTER_DIST || l == OrgLevel.BRANCH
                || l == OrgLevel.AGENCY || l == OrgLevel.SALES_OFFICE;
    }

    private static OrgUnit parentOrg(Long parentId, Map<Long, OrgUnit> idToOu) {
        if (parentId == null) return null;
        return idToOu.get(parentId);
    }

    private static List<Long> collectDescendantOrgIds(Long rootId, List<OrgUnit> all) {
        Map<Long, List<OrgUnit>> byParent = all.stream()
                .filter(o -> o.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        List<Long> out = new ArrayList<>();
        collectDescRec(rootId, byParent, out);
        return out;
    }

    private static void collectDescRec(Long id, Map<Long, List<OrgUnit>> byParent, List<Long> out) {
        for (OrgUnit ch : byParent.getOrDefault(id, List.of())) {
            out.add(ch.getId());
            collectDescRec(ch.getId(), byParent, out);
        }
    }

    private Map<String, Object> toAggregatedDistributionRow(OrgUnit rowOrg, DistributionAgg agg, Map<Long, OrgUnit> idToOu) {
        Map<String, Object> m = new HashMap<>();
        m.put("calcDt", agg.calcDt != null ? agg.calcDt.toString() : "");
        m.put("settleMonth", agg.calcDt != null ? agg.calcDt.format(DateTimeFormatter.ofPattern("yyyy-MM")) : "");
        m.put("compId", rowOrg.getCode());
        m.put("compNm", rowOrg.getName());
        m.put("orgDivNm", rowOrg.getOrgLevel() != null ? rowOrg.getOrgLevel().getNameKo() : "");
        m.put("settleAmt", agg.settleAmt);
        m.put("hqFee", agg.hqFee);
        m.put("regionalFee", agg.regionalFee);
        m.put("masterFee", agg.masterFee);
        m.put("branchFee", agg.branchFee);
        m.put("agencyFee", agg.agencyFee);
        m.put("hqRate", BigDecimal.ZERO);
        m.put("regionalRate", BigDecimal.ZERO);
        m.put("masterRate", BigDecimal.ZERO);
        m.put("branchRate", BigDecimal.ZERO);
        m.put("agencyRate", BigDecimal.ZERO);
        fillHierarchyColumnsFrom(rowOrg, m, idToOu);

        long apAmt = agg.approveAmtSum;
        long caAmt = agg.cancelAmtSum;
        m.put("aprvCnt", agg.runCnt);
        m.put("aprvAmt", apAmt);
        m.put("aprvFeeCnt", agg.runCnt);
        m.put("aprvFeePct", feePctString(agg.aprvFeeSum, apAmt));
        m.put("aprvFeeSum", agg.aprvFeeSum);
        m.put("aprvFeeVat", vatFromFee(agg.aprvFeeSum));

        m.put("canCnt", agg.cancelRunCnt);
        m.put("canAmt", caAmt);
        m.put("canFeeCnt", agg.cancelRunCnt);
        m.put("canFeePct", feePctString(agg.canFeeSum, caAmt));
        m.put("canFeeSum", agg.canFeeSum);
        m.put("canFeeVat", vatFromFee(agg.canFeeSum));
        return m;
    }

    private static long vatFromFee(long feeSum) {
        return BigDecimal.valueOf(feeSum).divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).longValue();
    }

    /** 수수료 ÷ 금액 × 100 (%) */
    private static String feePctString(long fee, long amt) {
        if (amt <= 0) return "0";
        return BigDecimal.valueOf(fee).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(amt), 2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 본사~영업점 컬럼: 행 조직(rowOrg)은 해당 단계 칸에, 나머지는 상위 조직만 채움 */
    private void fillHierarchyColumnsFrom(OrgUnit rowOrg, Map<String, Object> m, Map<Long, OrgUnit> idToOu) {
        String hq = "", regional = "", master = "", branch = "", agency = "";
        OrgUnit cur = parentOrg(rowOrg.getParentId(), idToOu);
        for (int i = 0; i < 16 && cur != null; i++) {
            if (cur.getOrgLevel() != null) {
                switch (cur.getOrgLevel()) {
                    case HEADQUARTERS -> hq = nzName(hq, cur.getName());
                    case REGIONAL -> regional = nzName(regional, cur.getName());
                    case MASTER_DIST -> master = nzName(master, cur.getName());
                    case BRANCH -> branch = nzName(branch, cur.getName());
                    case AGENCY, SALES_OFFICE -> agency = nzName(agency, cur.getName());
                    default -> { }
                }
            }
            cur = parentOrg(cur.getParentId(), idToOu);
        }
        if (rowOrg.getOrgLevel() != null) {
            switch (rowOrg.getOrgLevel()) {
                case HEADQUARTERS -> hq = rowOrg.getName() != null ? rowOrg.getName() : hq;
                case REGIONAL -> regional = rowOrg.getName() != null ? rowOrg.getName() : regional;
                case MASTER_DIST -> master = rowOrg.getName() != null ? rowOrg.getName() : master;
                case BRANCH -> branch = rowOrg.getName() != null ? rowOrg.getName() : branch;
                case AGENCY, SALES_OFFICE -> agency = rowOrg.getName() != null ? rowOrg.getName() : agency;
                default -> { }
            }
        }
        m.put("hqNm", hq);
        m.put("regionalNm", regional);
        m.put("masterNm", master);
        m.put("branchNm", branch);
        m.put("agencyNm", agency);
    }

    private static String nzName(String existing, String name) {
        if (name == null || name.isBlank()) return existing == null ? "" : existing;
        if (existing != null && !existing.isBlank()) return existing;
        return name;
    }

    private long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }

    private PageResult<Map<String, Object>> balanceListCore(
            Authentication authentication,
            String searchCompId,
            String searchCompNm,
            String searchOrderDir,
            int page,
            int size,
            boolean combined) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return emptyPage(page, size);
        }
        List<Map<String, Object>> all = new ArrayList<>();
        List<SettlementRun> runs = settlementCalcService.listRuns(LocalDate.now().minusMonths(6), LocalDate.now());
        Map<String, Long> holdByMerchant = new HashMap<>();
        Map<String, Long> unpaidByMerchant = new HashMap<>();
        for (SettlementRun r : runs) {
            String compId = r.getMerchantId();
            if (compId == null || compId.isBlank()) continue;
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            if (ou == null) continue;
            SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(ou.getId()).orElse(null);
            boolean payHold = ss != null && "Y".equalsIgnoreCase(ss.getPayHoldYn());
            long payAmt = r.getPayAmt() != null ? r.getPayAmt().longValue() : 0L;
            long unpaid = r.getTotalFee() != null ? r.getTotalFee().longValue() : 0L;
            if (payHold && payAmt > 0 && !"Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                holdByMerchant.put(compId, holdByMerchant.getOrDefault(compId, 0L) + payAmt);
            }
            if (unpaid > 0) {
                unpaidByMerchant.put(compId, unpaidByMerchant.getOrDefault(compId, 0L) + unpaid);
            }
        }

        Map<String, Long> deductedByMerchant = new HashMap<>();
        for (BalanceDeduction d : balanceDeductionRepository.findAll()) {
            String mid = d.getMerchantId();
            long amt = d.getAmount() != null ? d.getAmount() : 0L;
            if (mid == null || mid.isBlank() || amt <= 0) continue;
            deductedByMerchant.put(mid, deductedByMerchant.getOrDefault(mid, 0L) + amt);
        }

        Set<String> merchants = new LinkedHashSet<>();
        merchants.addAll(holdByMerchant.keySet());
        merchants.addAll(unpaidByMerchant.keySet());
        merchants.addAll(deductedByMerchant.keySet());
        for (String compId : merchants) {
            if (allowedMerchants != null && !allowedMerchants.contains(compId.trim())) {
                continue;
            }
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            String compNm = ou != null ? ou.getName() : compId;
            if (searchCompId != null && !searchCompId.isBlank() && (compId == null || !compId.contains(searchCompId.trim()))) continue;
            if (searchCompNm != null && !searchCompNm.isBlank() && (compNm == null || !compNm.contains(searchCompNm.trim()))) continue;
            long bal = holdByMerchant.getOrDefault(compId, 0L);
            long unpaid = unpaidByMerchant.getOrDefault(compId, 0L);
            long deducted = deductedByMerchant.getOrDefault(compId, 0L);
            long remain = bal - deducted;
            if (remain < 0) remain = 0;
            Map<String, Object> m = new HashMap<>();
            m.put("compId", compId);
            m.put("compNm", compNm);
            if (combined) {
                m.put("balcAmount", bal);
                m.put("unpaidAmount", unpaid);
                m.put("deductedAmount", deducted);
                m.put("remainAmount", remain);
            } else {
                m.put("condition", "ETC");
                m.put("chargeType", "지급보류");
                m.put("payMethod", "계좌");
                m.put("chargeNm", compNm);
                m.put("chargeAmt", remain);
                m.put("sumChargeAmt", remain);
            }
            all.add(m);
        }
        Comparator<Map<String, Object>> byComp = Comparator.comparing(m -> String.valueOf(m.getOrDefault("compId", "")));
        if (sortDirectionFromSearchOrderDir(searchOrderDir) == Sort.Direction.DESC) {
            byComp = byComp.reversed();
        }
        all.sort(byComp);
        return pageOf(all, page, size);
    }

    private PageResult<Map<String, Object>> pageOf(List<Map<String, Object>> all, int page, int size) {
        int s = Math.max(1, size);
        int p = Math.max(1, page);
        int from = Math.max(0, (p - 1) * s);
        int to = Math.min(all.size(), from + s);
        List<Map<String, Object>> rows = from < all.size() ? all.subList(from, to) : new ArrayList<>();
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(p);
        pr.setSize(s);
        pr.setTotalElements(all.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) all.size() / s)));
        return pr;
    }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    /** 가맹점별 수수료 정책(없으면 DEFAULT). 수수료내역·정산 표시에 사용 */
    private CommissionPolicy resolveCommissionPolicyForMerchant(String merchantId) {
        if (merchantId != null && !merchantId.isBlank()) {
            String m = merchantId.trim();
            return commissionPolicyRepository.findByScope(m)
                    .orElseGet(() -> commissionPolicyRepository.findByScope("DEFAULT").orElseGet(CommissionPolicy::new));
        }
        return commissionPolicyRepository.findByScope("DEFAULT").orElseGet(CommissionPolicy::new);
    }

    private record FeePolicy(
            BigDecimal perTxFee, BigDecimal usageRate, BigDecimal failFee,
            BigDecimal cancelRate, BigDecimal refundRate, BigDecimal payRate,
            BigDecimal feeSettlementPerTx, BigDecimal feeUsdt, BigDecimal feeFx,
            boolean recallIncludeFeeYn, boolean settlementVatApplyYn
    ) {}

    /** 회수관리 등: VAT·회수 시 수수료 포함 여부(본사 설정). 환수 시 수수료 금액은 수수료내역과 동일한 건별 합산. */
    private FeePolicy resolveHqFeePolicy() {
        CommissionPolicy p = commissionPolicyRepository.findByScope("DEFAULT").orElseGet(CommissionPolicy::new);
        HqApiConfig c = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        boolean recallIncludeFeeYn = c != null && "Y".equalsIgnoreCase(c.getRecallIncludeFeeYn());
        boolean settlementVatApplyYn = c == null || !"N".equalsIgnoreCase(c.getSettlementVatApplyYn());
        return new FeePolicy(
                nz(p.getPerTxFee()), nz(p.getUsageRate()), nz(p.getFailFee()),
                nz(p.getCancelRate()), nz(p.getRefundRate()), nz(p.getPayRate()),
                nz(p.getFeeSettlementPerTx()), nz(p.getFeeUsdt()), nz(p.getFeeFx()),
                recallIncludeFeeYn, settlementVatApplyYn
        );
    }

    /** 원금에 소수가 있으면 연동 추정 수수료·부가세도 동일 스케일(최소 2, 최대 8). 정수 원금(KRW 등)은 0. */
    private static int derivedFeeScale(BigDecimal principal) {
        if (principal == null) {
            return 0;
        }
        int s = principal.scale();
        return s > 0 ? Math.min(8, Math.max(2, s)) : 0;
    }

    private PageResult<Map<String, Object>> buildConfirmedSettlementReportPage(
            Authentication authentication,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchCompNm,
            String searchCompId,
            String searchOrderDir,
            int page,
            int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            PageResult<Map<String, Object>> empty = emptyPage(page, size);
            empty.setMeta(settlementReportService.reportMeta());
            return empty;
        }
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        List<SettlementRun> runs = settlementCalcService.listRuns(fromDate, toDate);
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (SettlementRun r : runs) {
            if (!"CALCULATED".equalsIgnoreCase(String.valueOf(r.getStatus()))) {
                continue;
            }
            String mid = r.getMerchantId();
            if (mid == null || mid.isBlank()) {
                continue;
            }
            if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (allowedMerchants != null && !allowedMerchants.contains(mid.trim())) {
                continue;
            }
            Map<String, Object> row = toConfirmedSettlementReportListRow(r, fromDate, toDate);
            if (searchCompId != null && !searchCompId.isBlank()) {
                String compId = String.valueOf(row.getOrDefault("compId", ""));
                if (!compId.contains(searchCompId.trim())) {
                    continue;
                }
            }
            if (searchCompNm != null && !searchCompNm.isBlank()) {
                String compNm = String.valueOf(row.getOrDefault("compNm", ""));
                if (!compNm.contains(searchCompNm.trim())) {
                    continue;
                }
            }
            allRows.add(row);
        }
        allRows.sort(mapRowsCalcDtPrimaryComparator(searchOrderDir)
                .thenComparing(m -> String.valueOf(m.getOrDefault("compId", "")))
                .thenComparing(m -> String.valueOf(m.getOrDefault("settlementRunId", ""))));

        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(allRows.size(), from + Math.max(1, size));
        List<Map<String, Object>> rows = new ArrayList<>();
        if (from < allRows.size()) {
            rows.addAll(allRows.subList(from, to));
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(allRows.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) allRows.size() / Math.max(1, size))));
        pr.setMeta(settlementReportService.reportMeta());
        return pr;
    }

    private Map<String, Object> toConfirmedSettlementReportListRow(SettlementRun r, LocalDate queryFrom, LocalDate queryTo) {
        Map<String, Object> row = toFranchiseSettlementRunRow(r);
        String calcCycleRep = "";
        String midRep = r.getMerchantId();
        if (midRep != null && !midRep.isBlank()) {
            calcCycleRep = orgUnitRepository.findByCode(midRep.trim())
                    .flatMap(ou -> settlementSettingRepository.findByOrgUnitId(ou.getId()))
                    .map(ss -> ss.getCalcCycle() != null ? ss.getCalcCycle() : "")
                    .orElse("");
        }
        row.put("targetPeriodText", buildSettlementTargetPeriodLabel(r, queryFrom, queryTo, calcCycleRep));
        row.put("reportRowKind", "CONFIRMED_SETTLEMENT");
        BigDecimal ap = nz(r.getApproveAmt());
        BigDecimal ca = nz(r.getCancelAmt());
        row.put("approveAmt", ap.longValue());
        row.put("cancelAmt", ca.longValue());
        row.put("netPay", ap.subtract(ca).longValue());
        row.put("payAmount", r.getPayAmt() != null ? r.getPayAmt().longValue() : 0L);
        return row;
    }

    private static long settlementReportTxAmount(PgTrnsctn t) {
        BigDecimal a = t.getAmtKrw();
        if (a == null) {
            a = t.getIcopayAmt();
        }
        if (a == null) {
            a = t.getTotalAmt();
        }
        if (a == null) {
            return 0L;
        }
        return a.abs().setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private Map<String, Object> buildSettlementReportTxBreakdown(List<PgTrnsctn> txs) {
        long approveAmt = 0L;
        long cancelAmt = 0L;
        long refundAmt = 0L;
        long otherAmt = 0L;
        int approveCnt = 0;
        int cancelCnt = 0;
        int refundCnt = 0;
        int otherCnt = 0;
        for (PgTrnsctn t : txs) {
            String st = t.getStatus();
            long amt = settlementReportTxAmount(t);
            if ("10".equals(st)) {
                approveAmt += amt;
                approveCnt++;
            } else if ("20".equals(st)) {
                cancelAmt += amt;
                cancelCnt++;
            } else if ("30".equals(st) || "31".equals(st)) {
                refundAmt += amt;
                refundCnt++;
            } else {
                otherAmt += amt;
                otherCnt++;
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("approveCnt", approveCnt);
        m.put("approveAmt", approveAmt);
        m.put("cancelCnt", cancelCnt);
        m.put("cancelAmt", cancelAmt);
        m.put("refundCnt", refundCnt);
        m.put("refundAmt", refundAmt);
        m.put("otherCnt", otherCnt);
        m.put("otherAmt", otherAmt);
        m.put("txnTotalCnt", txs.size());
        return m;
    }

    private Map<String, Object> buildSettlementRunTotalsForReport(SettlementRun r) {
        Map<String, Object> m = new LinkedHashMap<>();
        BigDecimal ap = nz(r.getApproveAmt());
        BigDecimal ca = nz(r.getCancelAmt());
        BigDecimal netBd = ap.subtract(ca);
        m.put("approveAmt", ap.longValue());
        m.put("cancelAmt", ca.longValue());
        m.put("netPay", netBd.longValue());
        m.put("totalFee", r.getTotalFee() != null ? r.getTotalFee().longValue() : 0L);
        m.put("rollingReserveAmt", r.getRollingReserveAmt() != null ? r.getRollingReserveAmt().longValue() : 0L);
        m.put("payAmount", r.getPayAmt() != null ? r.getPayAmt().longValue() : 0L);
        m.put("status", r.getStatus());
        m.put("settlementRunId", r.getId());
        return m;
    }

    private String payDivName(String status) {
        return payDivLabel(status);
    }

    private String payDivLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "10" -> "결제";
            case "20" -> "취소";
            case "21" -> "무효";
            case "22" -> "수동무효";
            case "40" -> "자동무효";
            case "41" -> "이메일무효";
            case "42" -> "자동환불";
            case "30", "31" -> "환불";
            case "F0", "99" -> "실패";
            default -> status;
        };
    }
}
