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
import com.pg.entity.MerchantReceivableRecoveryRequest;
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
import com.pg.repository.MerchantReceivableRecoveryRequestRepository;
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
import com.pg.service.CommissionService;
import com.pg.service.CollateralLedgerService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.OrgAccessService;
import com.pg.service.OrgPagePermissionService;
import com.pg.service.PayListService;
import com.pg.service.SettlementCalcService;
import com.pg.service.ReceivableRecoveryModeService;
import com.pg.service.SettlementReportService;
import com.pg.service.settlement.FeeListTxnAmountService;
import com.pg.service.settlement.FeeListTxnBreakdownCalculator;
import com.pg.service.settlement.SettlementArrearsService;
import com.pg.service.settlement.SettlementAutoRunService;
import com.pg.service.settlement.SettlementBusinessHolidayService;
import com.pg.service.settlement.SettlementExpectedDateResolver;
import com.pg.service.settlement.SettlementRunFeeReconciliationService;
import com.pg.service.settlement.SettlementPublishCadence;
import com.pg.service.settlement.SettlementCycleTiming;
import com.pg.service.settlement.SettlementPeriodResolver;
import com.pg.util.CommissionExtraFeeUtil;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.MerchantFeeVatUtil;
import com.pg.util.PayDisplayCurrency;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.PercentDecimalHelper;
import com.pg.util.TrnTimeDualZoneDisplay;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
    private final FeeListTxnAmountService feeListTxnAmountService;
    private final SettlementRecoveryRepository settlementRecoveryRepository;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final MerchantReceivableRecoveryRequestRepository merchantReceivableRecoveryRequestRepository;
    private final SettlementArrearsService settlementArrearsService;
    private final OrgPagePermissionService orgPagePermissionService;
    private final SettlementRunFeeReconciliationService settlementRunFeeReconciliationService;
    private final ReceivableRecoveryModeService receivableRecoveryModeService;
    private final CommissionService commissionService;
    private final SettlementBusinessHolidayService settlementBusinessHolidayService;

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
                                   FeeListTxnAmountService feeListTxnAmountService,
                                   SettlementRecoveryRepository settlementRecoveryRepository,
                                   MerchantReceivableRepository merchantReceivableRepository,
                                   MerchantReceivableRecoveryRequestRepository merchantReceivableRecoveryRequestRepository,
                                   SettlementArrearsService settlementArrearsService,
                                   OrgPagePermissionService orgPagePermissionService,
                                   SettlementRunFeeReconciliationService settlementRunFeeReconciliationService,
                                   ReceivableRecoveryModeService receivableRecoveryModeService,
                                   CommissionService commissionService,
                                   SettlementBusinessHolidayService settlementBusinessHolidayService) {
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
        this.feeListTxnAmountService = feeListTxnAmountService;
        this.settlementRecoveryRepository = settlementRecoveryRepository;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.merchantReceivableRecoveryRequestRepository = merchantReceivableRecoveryRequestRepository;
        this.settlementArrearsService = settlementArrearsService;
        this.orgPagePermissionService = orgPagePermissionService;
        this.settlementRunFeeReconciliationService = settlementRunFeeReconciliationService;
        this.receivableRecoveryModeService = receivableRecoveryModeService;
        this.commissionService = commissionService;
        this.settlementBusinessHolidayService = settlementBusinessHolidayService;
    }

    private Set<LocalDate> holidaysForMerchant(Map<Long, Set<LocalDate>> holidayCache, PayListRowContext payCtx) {
        if (holidayCache == null || payCtx == null || payCtx.getProfile() == null
                || payCtx.getProfile().getOrgUnitId() == null) {
            return Set.of();
        }
        long orgUnitId = payCtx.getProfile().getOrgUnitId();
        return holidayCache.computeIfAbsent(orgUnitId,
                settlementBusinessHolidayService::resolveNonBusinessDatesForMerchantOrgUnitId);
    }

    private ZoneId resolveLedgerDisplayZoneId() {
        return HqLedgerSysSettingsService.resolveDisplayZoneIdFromSettings(
                hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc().orElse(null));
    }

    private FeeCurrencyRoundResolver resolveFeeCurrencyRoundResolver() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(FeeCurrencyRoundResolver::from)
                .orElseGet(() -> FeeCurrencyRoundResolver.from(null));
    }

    /** 정산관리 금액: 전산설정 기준통화 + 수수료·정산 소수 규칙 */
    private FeeListRoundingPolicy resolveSettlementLedgerRoundPolicy() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(s -> FeeCurrencyRoundResolver.from(s).forCurrency(PayDisplayCurrency.alphaFromSettings(s)))
                .orElseGet(FeeListRoundingPolicy::defaults);
    }

    private static double settlementMoneyDouble(BigDecimal bd, FeeListRoundingPolicy rp) {
        if (bd == null) {
            return 0d;
        }
        return FeeListRoundingPolicy.round(bd, rp).doubleValue();
    }

    private static double settlementPctFeeDouble(BigDecimal base, BigDecimal ratePct, FeeListRoundingPolicy rp) {
        if (base == null || ratePct == null || rp == null || ratePct.signum() == 0) {
            return 0d;
        }
        return FeeListRoundingPolicy.round(
                base.multiply(ratePct).divide(BigDecimal.valueOf(100), 16, RoundingMode.HALF_UP), rp).doubleValue();
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
        if (!allowedMerchantsContains(allowedMerchants, mid)) {
            return ResponseEntity.ok(ApiResponse.fail("조회 권한이 없습니다.", "FORBIDDEN"));
        }
        if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
            return ResponseEntity.ok(ApiResponse.fail("지급보류 적치 건은 리포트 상세를 열 수 없습니다.", "FORBIDDEN"));
        }
        if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
            return ResponseEntity.ok(ApiResponse.fail("정산배포(DISTRIBUTED)된 실행만 조회할 수 있습니다.", "FORBIDDEN"));
        }
        if (!"CALCULATED".equalsIgnoreCase(String.valueOf(r.getStatus()))) {
            return ResponseEntity.ok(ApiResponse.fail("확정(CALCULATED)된 정산만 리포트로 조회할 수 있습니다.", "INVALID_STATE"));
        }
        LocalDate qFrom = r.getPeriodFrom() != null ? r.getPeriodFrom() : r.getCalcDt();
        LocalDate qTo = r.getPeriodTo() != null ? r.getPeriodTo() : r.getCalcDt();
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<Long, BigDecimal> detailDeficit = indexAutoDeficitTotalsByRunId(Collections.singletonList(r));
        payload.put("listRow", toConfirmedSettlementReportListRow(r, qFrom, qTo, detailDeficit));
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
            if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
                continue;
            }
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
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
                String curKey = String.valueOf(dr.getOrDefault("curType", "")).trim().toUpperCase(Locale.ROOT);
                if (curKey.isEmpty()) {
                    curKey = "_";
                }
                /* 통화별로 분리하지 않으면 KRW·USD 등이 한 행에 합산되어 금액·%가 왜곡됩니다. */
                String key = Objects.requireNonNullElse(rollupOrgCode, "") + "|" + runCalcDt + "|" + curKey;
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
                .thenComparing(m -> String.valueOf(m.getOrDefault("compId", "")))
                .thenComparing(m -> String.valueOf(m.getOrDefault("curType", ""))));

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
     * M5·H1·TM·TH 등은 주기 마감({@code period_end_at} 기록)된 실행만 표시하고, RT는 건별 표시합니다.
     * 본사·총판·지사·대리점·영업점 등 유통 구간 수익은 {@link #distributionList} 에서 동일 실행분을 집계합니다.
     */
    @GetMapping("/franchiseList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> franchiseList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchKeyword,
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
        Map<Long, BigDecimal> autoDeficitByRun = indexAutoDeficitTotalsByRunId(runs);
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
        final String effFtFinal = effFt;
        final String effKwFinal = effKw;
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (SettlementRun r : runs) {
            String mid = r.getMerchantId();
            if (mid == null || mid.isBlank()) {
                continue;
            }
            if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
                continue;
            }
            if (!allowedMerchantsContains(allowedMerchants, mid.trim())) {
                continue;
            }
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
                continue;
            }
            Map<String, Object> row = toFranchiseSettlementRunRow(r, fromDate, toDate, autoDeficitByRun);
            if (!franchiseListRowMatches(row, effFtFinal, effKwFinal)) {
                continue;
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
     * 지급보류 적치 내역(정산보류내역 화면) — 지급보류(Y) 가맹점의 정산 실행 행만(가맹점정산내역과 동일 컬럼 + 비고).
     */
    @GetMapping("/payoutHoldList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> payoutHoldList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                buildPayoutHoldListPage(authentication, searchFromDate, searchToDate, searchCompNm, searchCompId,
                        searchFieldType, searchKeyword, searchOrderDir, page, size)));
    }

    /**
     * 레거시 경로 — {@link #payoutHoldList} 와 동일 API. 외부·구클라이언트 호환용. 롤링 담보는 {@link #collateralList} 사용.
     */
    @GetMapping("/holdList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> holdList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                buildPayoutHoldListPage(authentication, searchFromDate, searchToDate, searchCompNm, searchCompId,
                        searchFieldType, searchKeyword, searchOrderDir, page, size)));
    }

    private PageResult<Map<String, Object>> buildPayoutHoldListPage(
            Authentication authentication,
            LocalDate searchFromDate,
            LocalDate searchToDate,
            String searchCompNm,
            String searchCompId,
            String searchFieldType,
            String searchKeyword,
            String searchOrderDir,
            int page,
            int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return emptyPage(page, size);
        }
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
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
        final String effFtFinal = effFt;
        final String effKwFinal = effKw;
        List<SettlementRun> runs = settlementCalcService.listRuns(fromDate, toDate);
        Map<Long, BigDecimal> autoDeficitByRun = indexAutoDeficitTotalsByRunId(runs);
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (SettlementRun r : runs) {
            String mid = r.getMerchantId();
            if (mid == null || mid.isBlank()) {
                continue;
            }
            if (!"Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            if (!allowedMerchantsContains(allowedMerchants, mid.trim())) {
                continue;
            }
            Map<String, Object> row = toFranchiseSettlementRunRow(r, fromDate, toDate, autoDeficitByRun);
            if (!franchiseListRowMatches(row, effFtFinal, effKwFinal)) {
                continue;
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
            if (!allowedMerchantsContains(allowedMerchants, mid)) {
                return ResponseEntity.ok(ApiResponse.fail("해당 정산 건에 대한 권한이 없습니다: " + id, "FORBIDDEN"));
            }
            if (!"Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                continue;
            }
            r.setPayoutHoldYn("N");
            String prev = r.getPayoutHoldRemark() != null ? r.getPayoutHoldRemark().trim() : "";
            String suffix = "해제됨(" + stamp + "): 가맹점정산내역 반영";
            r.setPayoutHoldRemark(prev.isEmpty() ? suffix : prev + " | " + suffix);
            r.setSettlementPublishSts("DISTRIBUTED");
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
            if (!allowedMerchantsContains(allowedMerchants, compId.trim())) {
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
            boolean incFees = SettlementArrearsService.recallAmountIncludesFeeComponents(
                    hqPolicy.recallIncludeFeeYn, feeAmtBd, feeVatBd);
            BigDecimal recallAmtBd = txnAmtBd.max(BigDecimal.ZERO);
            if (incFees) {
                recallAmtBd = recallAmtBd.add(feeAmtBd).add(feeVatBd);
            }
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
            m.put("feeIncludedYn", incFees ? "Y" : "N");
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
        FeeListRoundingPolicy srp = resolveSettlementLedgerRoundPolicy();
        for (SettlementRecovery r : slice.getContent()) {
            String compId = r.getMerchantId();
            OrgUnit ou = compId != null ? orgUnitRepository.findByCode(compId).orElse(null) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("compId", compId);
            m.put("compNm", ou != null ? ou.getName() : compId);
            m.put("curType", resolveMerchantStatementCurrency(compId));
            m.put("trnId", r.getTrnId());
            m.put("recallAmount", settlementMoneyDouble(r.getRecallAmount(), srp));
            m.put("remainingAmount", settlementMoneyDouble(r.getRemainingAmount(), srp));
            m.put("appliedAmount", settlementMoneyDouble(r.getAppliedAmount(), srp));
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
        FeeListRoundingPolicy srpRcv = resolveSettlementLedgerRoundPolicy();
        List<Long> recvIds = slice.getContent().stream().map(MerchantReceivable::getId).filter(Objects::nonNull).toList();
        Set<Long> pendingByReceivableId = new HashSet<>();
        if (!recvIds.isEmpty()) {
            for (MerchantReceivableRecoveryRequest q : merchantReceivableRecoveryRequestRepository.findByMerchantReceivableIdInAndStatus(
                    recvIds, MerchantReceivableRecoveryRequest.STATUS_PENDING)) {
                pendingByReceivableId.add(q.getMerchantReceivableId());
            }
        }
        for (MerchantReceivable r : slice.getContent()) {
            String compId = r.getMerchantId();
            OrgUnit ou = compId != null ? orgUnitRepository.findByCode(compId).orElse(null) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("compId", compId);
            m.put("compNm", ou != null ? ou.getName() : compId);
            m.put("curType", resolveMerchantStatementCurrency(compId));
            m.put("title", r.getTitle() != null ? r.getTitle() : "");
            m.put("totalAmount", settlementMoneyDouble(r.getTotalAmount(), srpRcv));
            m.put("remainingAmount", settlementMoneyDouble(r.getRemainingAmount(), srpRcv));
            m.put("appliedAmount", settlementMoneyDouble(r.getAppliedAmount(), srpRcv));
            m.put("status", r.getStatus());
            m.put("reasonCode", r.getReasonCode());
            m.put("memo", r.getMemo() != null ? r.getMemo() : "");
            m.put("createdBy", r.getCreatedBy() != null ? r.getCreatedBy() : "");
            m.put("createdAt", r.getCreatedAt() != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r.getCreatedAt()) : "");
            String recvMode = resolveReceivableRecoveryModeForMerchantCode(compId);
            m.put("receivableRecoveryMode", recvMode);
            boolean pend = r.getId() != null && pendingByReceivableId.contains(r.getId());
            m.put("recoveryPendingYn", pend ? "Y" : "N");
            m.put("receivablePhaseNm", receivableListRowPhaseNm(recvMode, r, pend));
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

    /** 미수금 수동 등록·FIFO 차감. {@code direction}=ADD(기본)|DEDUCT — DEDUCT 는 잔여 미수금을 등록 순으로 줄입니다. */
    @PostMapping("/receivable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receivableCreate(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        String compId = body.get("compId") != null ? String.valueOf(body.get("compId")).trim() : "";
        if (compId.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("업체코드(compId)는 필수입니다.", "VALIDATION"));
        }
        String dirRaw = body.get("direction") != null ? String.valueOf(body.get("direction")).trim() : "ADD";
        String dir = dirRaw.isEmpty() ? "ADD" : dirRaw.toUpperCase(Locale.ROOT);
        boolean deduct = "DEDUCT".equals(dir) || "SUBTRACT".equals(dir) || "MINUS".equals(dir);
        BigDecimal amount;
        try {
            amount = new BigDecimal(String.valueOf(body.getOrDefault("amount", "0")).trim());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail("금액(amount) 형식이 올바르지 않습니다.", "VALIDATION"));
        }
        if (amount.signum() <= 0) {
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
        FeeListRoundingPolicy srp = resolveSettlementLedgerRoundPolicy();
        if (deduct) {
            String memo = body.get("memo") != null ? String.valueOf(body.get("memo")).trim() : "";
            try {
                Map<String, Object> red = settlementArrearsService.manualReduceReceivableFifo(compId, amount, memo, username);
                BigDecimal applied = (BigDecimal) red.get("appliedTotal");
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("direction", "DEDUCT");
                out.put("compId", compId);
                out.put("appliedTotal", settlementMoneyDouble(applied, srp));
                out.put("changedRowCount", red.get("changedRowCount"));
                out.put("message", "미수금이 차감되었습니다.");
                return ResponseEntity.ok(ApiResponse.ok(out));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
            }
        }
        String title = body.get("title") != null ? String.valueOf(body.get("title")).trim() : "미수금";
        String reasonCode = body.get("reasonCode") != null ? String.valueOf(body.get("reasonCode")).trim() : "MANUAL";
        String memo = body.get("memo") != null ? String.valueOf(body.get("memo")).trim() : "";
        try {
            MerchantReceivable r = settlementArrearsService.createReceivable(compId, amount, title, reasonCode, memo, username);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "id", r.getId(),
                    "direction", "ADD",
                    "compId", compId,
                    "amount", settlementMoneyDouble(r.getTotalAmount(), srp),
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

    /**
     * 수동 미수금 환수 가맹만: 해당 미수금 건을 다음 정산 마감 시 지급액에서 차감하도록 요청합니다.
     */
    @PostMapping("/receivable/{id}/recoveryRequest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receivableRecoveryRequest(
            Authentication authentication,
            @PathVariable long id) {
        MerchantReceivable r = merchantReceivableRepository.findById(id).orElse(null);
        if (r == null) {
            return ResponseEntity.ok(ApiResponse.fail("미수금을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        if (authentication != null && authentication.getPrincipal() instanceof AppUser actorR
                && !orgPagePermissionService.canManuallyManageMerchantReceivable(actorR)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "미수금 처리는 본사권한설정에서 「미수금관리」화면 권한이 수정(M) 이상인 계정만 가능합니다.", "FORBIDDEN"));
        }
        if (!canAccessReceivable(authentication, r.getMerchantId())) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        String username = (authentication != null && authentication.getPrincipal() instanceof AppUser u)
                ? u.getUsername() : "";
        try {
            settlementArrearsService.requestManualReceivableRecovery(id, username);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "id", id, "message", "다음 정산 마감 시 환수 반영됩니다.")));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** 가맹 미수금 환수 모드(총판·본사 기본 상속 + 가맹 개별 우선). */
    private String resolveReceivableRecoveryModeForMerchantCode(String compId) {
        return receivableRecoveryModeService.resolveEffectiveModeForMerchantCode(compId);
    }

    private static String receivableListRowPhaseNm(String recoveryMode, MerchantReceivable r, boolean pendingRequest) {
        String st = r.getStatus() != null ? r.getStatus().trim() : "";
        BigDecimal rem = r.getRemainingAmount() != null ? r.getRemainingAmount() : BigDecimal.ZERO;
        boolean open = "PENDING".equals(st) || "PARTIAL".equals(st);
        if ("MANUAL".equalsIgnoreCase(recoveryMode)) {
            if (pendingRequest) {
                return "환수처리";
            }
            if (open && rem.signum() > 0) {
                return "미요청";
            }
            return "-";
        }
        if (open && rem.signum() > 0) {
            return "자동화중";
        }
        return "-";
    }

    /**
     * @param deficitReceivableAmt 해당 실행에 연결된 지급부족 자동미수({@code AUTO_DEFICIT:runId}) 발생액, 없으면 0
     * @param payAmt 정산 실행 저장 지급액(음수 가능)
     */
    private static String franchiseReceivableProcessNm(
            String recoveryMode,
            BigDecimal appliedAmt,
            String runStatus,
            BigDecimal deficitReceivableAmt,
            BigDecimal payAmt) {
        BigDecimal a = appliedAmt != null ? appliedAmt : BigDecimal.ZERO;
        BigDecimal def = deficitReceivableAmt != null ? deficitReceivableAmt : BigDecimal.ZERO;
        BigDecimal pay = payAmt != null ? payAmt : BigDecimal.ZERO;
        if (a.signum() > 0) {
            boolean calculated = runStatus != null && "CALCULATED".equalsIgnoreCase(runStatus.trim());
            if (calculated) {
                return "완료";
            }
            if (recoveryMode != null && recoveryMode.trim().equalsIgnoreCase("MANUAL")) {
                return "처리중";
            }
            return "자동화중";
        }
        /* 지급액이 음수여서 자동 미수금만 등록된 행: 차기 정산에서의 회수 방식 안내 */
        if (pay.signum() < 0 && def.signum() > 0) {
            if (recoveryMode != null && recoveryMode.trim().equalsIgnoreCase("MANUAL")) {
                return "환수처리·차기마감";
            }
            return "차기정산자동";
        }
        return "-";
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
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchStatusGroup,
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

        String effFt = "ALL";
        String effKw = "";
        if (searchFieldType != null && !searchFieldType.isBlank()) {
            effFt = searchFieldType.trim().toUpperCase(Locale.ROOT);
            effKw = searchKeyword != null ? searchKeyword.trim() : "";
        } else {
            if (searchCompId != null && !searchCompId.isBlank()) {
                effFt = "COMP_ID";
                effKw = searchCompId.trim();
            } else if (searchCompNm != null && !searchCompNm.isBlank()) {
                effFt = "COMP_NM";
                effKw = searchCompNm.trim();
            }
        }
        if ("COMP_NM".equals(effFt) && effKw.isEmpty()) {
            effFt = "ALL";
        }
        final String effFtFinal = effFt;
        final String effKwFinal = effKw;

        String statusGroup = searchStatusGroup != null && !searchStatusGroup.isBlank()
                ? searchStatusGroup.trim().toUpperCase(Locale.ROOT) : "ALL";

        final Set<String> merchantNameFilter;
        if ("COMP_NM".equals(effFtFinal) && !effKwFinal.isEmpty()) {
            Set<String> nm = new HashSet<>();
            for (OrgUnit ou : orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, effKwFinal)) {
                if (ou.getCode() == null || ou.getCode().isBlank()) {
                    continue;
                }
                String code = ou.getCode().trim();
                if (allowedMerchantsContains(allowedMerchants, code)) {
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
            addFeeListStatusGroupPredicate(parts, cb, root, statusGroup);

            if (!"COMP_NM".equals(effFtFinal)) {
                Predicate fieldPred = buildFeeListFieldSearchPredicate(root, query, cb, effFtFinal, effKwFinal);
                if (fieldPred != null) {
                    parts.add(fieldPred);
                }
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
        Map<Long, Set<LocalDate>> holidayCache = new HashMap<>();

        List<Map<String, Object>> rows = new ArrayList<>();
        int rowNoStart = (pageOneBased - 1) * pageSize + 1;
        int rowIdx = 0;
        for (PgTrnsctn t : slice.getContent()) {
            if (t.getMerchantId() == null || t.getMerchantId().isBlank()) {
                continue;
            }
            Map<String, Object> feeRow = buildFeeListRowMap(t, monthCbCountCache, tiersByPolicyId, ctxByMerchant, polCache,
                    feeResolver, holidayCache);
            feeRow.put("rowNo", rowNoStart + rowIdx);
            rowIdx++;
            rows.add(feeRow);
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

    private static final int DAILY_FEE_SUMMARY_MAX_DAYS = 93;
    private static final int DAILY_FEE_PAGE_SIZE = 500;
    private static final int DAILY_FEE_MAX_PAGES_PER_DAY = 400;
    private static final String[] DAILY_FEE_SUM_NUMERIC_KEYS = {
            "txnFixedFeesSum", "pctFeesSum", "usdtFee", "fxFee", "fee3dsFee", "rollingHoldEst",
            "failFee", "cancelFee", "voidFee", "manualVoidFee", "refundFee", "chargebackFee",
            "totalFee", "feeVat", "expectedPayout", "settlementAmt"
    };

    private Specification<PgTrnsctn> feeListSpecification(Set<String> allowedMerchants,
                                                          LocalDateTime fromDt,
                                                          LocalDateTime toDt,
                                                          String effFtFinal,
                                                          String effKwFinal,
                                                          Set<String> merchantNameFilter,
                                                          String statusGroup) {
        return (root, query, cb) -> {
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
            addFeeListStatusGroupPredicate(parts, cb, root, statusGroup);
            if (!"COMP_NM".equals(effFtFinal)) {
                Predicate fieldPred = buildFeeListFieldSearchPredicate(root, query, cb, effFtFinal, effKwFinal);
                if (fieldPred != null) {
                    parts.add(fieldPred);
                }
            }
            Subquery<Long> ouExists = query.subquery(Long.class);
            Root<OrgUnit> ouRoot = ouExists.from(OrgUnit.class);
            ouExists.select(cb.literal(1L));
            ouExists.where(cb.equal(ouRoot.get("code"), root.get("merchantId")));
            parts.add(cb.exists(ouExists));
            return cb.and(parts.toArray(new Predicate[0]));
        };
    }

    /**
     * 수수료내역과 동일 필터·산식으로, 적재일(createdAt) 기준 일자별 합계를 반환합니다.
     */
    @GetMapping("/dailyFeeSummary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dailyFeeSummary(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchStatusGroup,
            @RequestParam(required = false) String searchOrderDir) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("list", List.of());
            return ResponseEntity.ok(ApiResponse.ok(empty));
        }
        LocalDate fromDate = searchFromDate;
        LocalDate toDate = searchToDate;
        if (fromDate == null || toDate == null) {
            return ResponseEntity.ok(ApiResponse.fail("거래일자(searchFromDate, searchToDate)는 필수입니다.", "VALIDATION"));
        }
        if (fromDate.isAfter(toDate)) {
            return ResponseEntity.ok(ApiResponse.fail("거래일자 시작이 종료보다 늦을 수 없습니다.", "VALIDATION"));
        }
        long span = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (span > DAILY_FEE_SUMMARY_MAX_DAYS) {
            return ResponseEntity.ok(ApiResponse.fail("조회 기간은 " + DAILY_FEE_SUMMARY_MAX_DAYS + "일 이내로 지정해 주세요.", "VALIDATION"));
        }
        ZoneId ledgerTz = resolveLedgerDisplayZoneId();
        LocalDate today = LocalDate.now(ledgerTz);
        LocalDate effectiveTo = toDate.isAfter(today) ? today : toDate;
        if (fromDate.isAfter(effectiveTo)) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("list", List.of());
            empty.put("meta", Map.of("note", "조회 구간에 포함된 일자가 없습니다."));
            return ResponseEntity.ok(ApiResponse.ok(empty));
        }

        String effFt = "ALL";
        String effKw = "";
        if (searchFieldType != null && !searchFieldType.isBlank()) {
            effFt = searchFieldType.trim().toUpperCase(Locale.ROOT);
            effKw = searchKeyword != null ? searchKeyword.trim() : "";
        } else {
            if (searchCompId != null && !searchCompId.isBlank()) {
                effFt = "COMP_ID";
                effKw = searchCompId.trim();
            } else if (searchCompNm != null && !searchCompNm.isBlank()) {
                effFt = "COMP_NM";
                effKw = searchCompNm.trim();
            }
        }
        if ("COMP_NM".equals(effFt) && effKw.isEmpty()) {
            effFt = "ALL";
        }
        final String effFtFinal = effFt;
        final String effKwFinal = effKw;
        String statusGroup = searchStatusGroup != null && !searchStatusGroup.isBlank()
                ? searchStatusGroup.trim().toUpperCase(Locale.ROOT) : "ALL";

        final Set<String> merchantNameFilter;
        if ("COMP_NM".equals(effFtFinal) && !effKwFinal.isEmpty()) {
            Set<String> nm = new HashSet<>();
            for (OrgUnit ou : orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, effKwFinal)) {
                if (ou.getCode() == null || ou.getCode().isBlank()) {
                    continue;
                }
                String code = ou.getCode().trim();
                if (allowedMerchantsContains(allowedMerchants, code)) {
                    nm.add(code);
                }
            }
            if (nm.isEmpty()) {
                Map<String, Object> empty = new LinkedHashMap<>();
                empty.put("list", List.of());
                return ResponseEntity.ok(ApiResponse.ok(empty));
            }
            merchantNameFilter = nm;
        } else {
            merchantNameFilter = null;
        }

        FeeCurrencyRoundResolver feeResolver = resolveFeeCurrencyRoundResolver();
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        Map<String, CommissionPolicy> polCache = new HashMap<>();
        Map<Long, Set<LocalDate>> holidayCache = new HashMap<>();
        Sort sort = Sort.by(sortDirectionFromSearchOrderDir(searchOrderDir), "createdAt")
                .and(Sort.by(sortDirectionFromSearchOrderDir(searchOrderDir), "trnId"));

        List<Map<String, Object>> out = new ArrayList<>();
        for (LocalDate d = effectiveTo; !d.isBefore(fromDate); d = d.minusDays(1)) {
            LocalDateTime fromDt = d.atStartOfDay();
            LocalDateTime toDt = d.atTime(LocalTime.MAX);
            Specification<PgTrnsctn> specDay = feeListSpecification(allowedMerchants, fromDt, toDt, effFtFinal, effKwFinal,
                    merchantNameFilter, statusGroup);
            long txnCount = pgTrnsctnRepository.count(specDay);
            long settledY = 0;
            long settledN = 0;
            Map<String, Double> sums = new LinkedHashMap<>();
            for (String k : DAILY_FEE_SUM_NUMERIC_KEYS) {
                sums.put(k, 0d);
            }
            boolean capped = false;
            long scanned = 0;
            for (int pageIdx = 0; pageIdx < DAILY_FEE_MAX_PAGES_PER_DAY; pageIdx++) {
                Pageable pageable = PageRequest.of(pageIdx, DAILY_FEE_PAGE_SIZE, sort);
                Page<PgTrnsctn> slice = pgTrnsctnRepository.findAll(specDay, pageable);
                if (slice.isEmpty()) {
                    break;
                }
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
                for (PgTrnsctn t : slice.getContent()) {
                    if (t.getMerchantId() == null || t.getMerchantId().isBlank()) {
                        continue;
                    }
                    String yn = t.getSettledYn();
                    if (yn != null && "Y".equalsIgnoreCase(yn.trim())) {
                        settledY++;
                    } else {
                        settledN++;
                    }
                    Map<String, Object> row = buildFeeListRowMap(t, monthCbCountCache, tiersByPolicyId, ctxByMerchant, polCache,
                            feeResolver, holidayCache);
                    for (String key : DAILY_FEE_SUM_NUMERIC_KEYS) {
                        Object v = row.get(key);
                        if (v instanceof Number n) {
                            sums.merge(key, n.doubleValue(), Double::sum);
                        }
                    }
                }
                scanned += slice.getNumberOfElements();
                if (!slice.hasNext()) {
                    break;
                }
                if (pageIdx + 1 >= DAILY_FEE_MAX_PAGES_PER_DAY) {
                    capped = true;
                    break;
                }
            }
            String settlementStateLabel;
            if (txnCount <= 0) {
                settlementStateLabel = "—";
            } else if (settledN == 0) {
                settlementStateLabel = "정산완료";
            } else if (settledY == 0) {
                settlementStateLabel = "정산대기";
            } else {
                settlementStateLabel = "부분정산";
            }
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("day", d.toString());
            one.put("txnCount", txnCount);
            for (Map.Entry<String, Double> e : sums.entrySet()) {
                one.put(e.getKey(), e.getValue());
            }
            one.put("settlementStateLabel", settlementStateLabel);
            one.put("scannedRows", scanned);
            one.put("capped", capped);
            out.add(one);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("list", out);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("dailyFeeNote", "일자별 상세는 동일 조건으로 feeList 에 해당 일자만 지정해 조회합니다.");
        payload.put("meta", meta);
        PageResult<Map<String, Object>> prMeta = new PageResult<>();
        attachFeeCurrencyMeta(prMeta);
        if (prMeta.getMeta() != null) {
            meta.putAll(prMeta.getMeta());
        }
        return ResponseEntity.ok(ApiResponse.ok(payload));
    }

    private static void addFeeListStatusGroupPredicate(List<Predicate> parts,
                                                       CriteriaBuilder cb,
                                                       Root<PgTrnsctn> root,
                                                       String group) {
        if (group == null || group.isBlank() || "ALL".equals(group)) {
            return;
        }
        Path<String> st = root.get("status");
        switch (group) {
            case "SUCCESS" -> parts.add(cb.equal(st, "10"));
            case "FAIL" -> parts.add(st.in(List.of("F0", "99")));
            case "CANCEL" -> parts.add(cb.equal(st, "20"));
            case "VOID" -> parts.add(st.in(List.of("21", "40")));
            case "MANUAL_VOID" -> parts.add(st.in(List.of("22", "41")));
            case "REFUND" -> parts.add(st.in(List.of("30", "42")));
            case "FORCE_REFUND" -> parts.add(cb.equal(st, "31"));
            case "EXCLUDE_SUCCESS" -> parts.add(cb.or(cb.isNull(st), cb.notEqual(st, "10")));
            default -> {
            }
        }
    }

    private static Predicate buildFeeListFieldSearchPredicate(Root<PgTrnsctn> root,
                                                              CriteriaQuery<?> query,
                                                              CriteriaBuilder cb,
                                                              String effFt,
                                                              String effKw) {
        if ("COMP_NM".equals(effFt)) {
            return null;
        }
        if (effKw.isEmpty() && !"ALL".equals(effFt)) {
            return null;
        }
        if ("ALL".equals(effFt)) {
            if (effKw.isEmpty()) {
                return null;
            }
            String esc = escapeSqlLike(effKw);
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(root.get("merchantId"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("orderNo"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("chillTransactionId"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("trnId"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("approvalNo"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("routeNo"), "%" + esc + "%", '\\'));
            ors.add(cb.like(cb.upper(root.get("curType")), "%" + esc.toUpperCase(Locale.ROOT) + "%", '\\'));
            ors.add(cb.like(root.get("status"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("chillPaymentStatus"), "%" + esc + "%", '\\'));
            BigDecimal amt = parseAmountSearchKeyword(effKw);
            if (amt != null) {
                ors.add(cb.equal(root.get("amtKrw"), amt));
            }
            Subquery<Long> nameSq = query.subquery(Long.class);
            Root<OrgUnit> ouR = nameSq.from(OrgUnit.class);
            nameSq.select(cb.literal(1L));
            nameSq.where(cb.and(
                    cb.equal(ouR.get("orgLevel"), OrgLevel.MERCHANT),
                    cb.like(ouR.get("name"), "%" + esc + "%", '\\'),
                    cb.equal(ouR.get("code"), root.get("merchantId"))));
            ors.add(cb.exists(nameSq));
            return cb.or(ors.toArray(new Predicate[0]));
        }
        String esc = escapeSqlLike(effKw);
        return switch (effFt) {
            case "COMP_ID", "MID" -> cb.like(root.get("merchantId"), "%" + esc + "%", '\\');
            case "ORDER_NO" -> cb.like(root.get("orderNo"), "%" + esc + "%", '\\');
            case "APPROVAL_NO" -> cb.or(
                    cb.like(root.get("chillTransactionId"), "%" + esc + "%", '\\'),
                    cb.like(root.get("approvalNo"), "%" + esc + "%", '\\'));
            case "ROUTE" -> cb.like(root.get("routeNo"), "%" + esc + "%", '\\');
            case "CURRENCY" -> cb.like(cb.upper(root.get("curType")), "%" + esc.toUpperCase(Locale.ROOT) + "%", '\\');
            case "STATUS" -> cb.or(
                    cb.like(root.get("status"), "%" + esc + "%", '\\'),
                    cb.like(root.get("chillPaymentStatus"), "%" + esc + "%", '\\'));
            case "AMOUNT" -> {
                BigDecimal a = parseAmountSearchKeyword(effKw);
                yield a == null ? cb.disjunction() : cb.equal(root.get("amtKrw"), a);
            }
            default -> cb.like(root.get("merchantId"), "%" + esc + "%", '\\');
        };
    }

    private static BigDecimal parseAmountSearchKeyword(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseCalcDtForExecuteSort(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseRunCreatedAtForExecuteSort(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parseSettlementRunIdForSort(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 정산실행 목록: {@code searchCalcProcType} — 빈값·ALL 은 필터 없음, AUTO·MANUAL 허용(API). 화면은 전체·수동만 노출. */
    private static String normalizeExecuteListCalcProcFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(u)) {
            return "";
        }
        if ("AUTO".equals(u) || "MANUAL".equals(u)) {
            return u;
        }
        return "";
    }

    private static boolean executeListRowMatchesCalcProc(Map<String, Object> row, String wantUpper) {
        if (wantUpper == null || wantUpper.isEmpty()) {
            return true;
        }
        String code = String.valueOf(row.getOrDefault("calcProcType", "")).trim().toUpperCase(Locale.ROOT);
        return wantUpper.equals(code);
    }

    private static String executeListCellLower(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? "" : String.valueOf(v).toLowerCase(Locale.ROOT);
    }

    private static String executeListMoneyNorm(Object v) {
        return String.valueOf(v != null ? v : "").replace(",", "").trim();
    }

    /**
     * 정산실행 목록 행(그리드 맵)에 대한 검색구분·검색어 필터. {@code ALL}+비어 있는 검색어는 통과.
     */
    /**
     * 가맹점정산내역(및 동일 행 맵) — 정산실행과 동일한 검색구분·검색어 규칙.
     * 그리드 키는 {@link #toFranchiseSettlementRunRow} 기준({@code payStatus}, 금액은 amount·feeAmt·receivableAmt 등).
     */
    private static boolean franchiseListRowMatches(Map<String, Object> row, String fieldType, String keyword) {
        String ft = fieldType == null || fieldType.isBlank() ? "ALL" : fieldType.trim().toUpperCase(Locale.ROOT);
        String kw = keyword == null ? "" : keyword.trim();
        if (!"ALL".equals(ft) && kw.isEmpty()) {
            return true;
        }
        if ("ALL".equals(ft) && kw.isEmpty()) {
            return true;
        }
        String kNorm = kw.replace(",", "").trim();
        String kLow = kw.toLowerCase(Locale.ROOT);
        return switch (ft) {
            case "ALL" -> executeListCellLower(row, "calcCycle").contains(kLow)
                    || executeListCellLower(row, "calcMethod").contains(kLow)
                    || executeListCellLower(row, "compNm").contains(kLow)
                    || executeListCellLower(row, "compId").contains(kLow)
                    || executeListCellLower(row, "targetPeriodText").contains(kLow)
                    || executeListCellLower(row, "cadenceGuideKr").contains(kLow)
                    || executeListCellLower(row, "settlementPublishSts").contains(kLow)
                    || executeListCellLower(row, "payoutHoldYn").contains(kLow)
                    || String.valueOf(row.getOrDefault("settlementRunId", "")).toLowerCase(Locale.ROOT).contains(kLow)
                    || executeListCellLower(row, "pgRootNo").contains(kLow)
                    || executeListCellLower(row, "curType").contains(kLow)
                    || executeListCellLower(row, "payStatus").contains(kLow)
                    || ("확정".equals(kw) && executeListCellLower(row, "payStatus").contains("calculated"))
                    || ("미확정".equals(kw) && executeListCellLower(row, "payStatus").contains("pending"))
                    || executeListCellLower(row, "trnId").contains(kLow)
                    || executeListCellLower(row, "payNo").contains(kLow)
                    || executeListCellLower(row, "bizNo").contains(kLow)
                    || executeListCellLower(row, "txnCnt").contains(kLow)
                    || executeListMoneyNorm(row.get("amount")).contains(kNorm)
                    || executeListMoneyNorm(row.get("feeAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("holdAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("settlementBatchFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("remittanceFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("receivableAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("receivableDeductAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("settleAmt")).contains(kNorm)
                    || executeListCellLower(row, "receivableProcessNm").contains(kLow)
                    || executeListCellLower(row, "receivableRecoveryMode").contains(kLow);
            case "CALC_CYCLE" -> executeListCellLower(row, "calcCycle").contains(kLow);
            case "CALC_METHOD" -> executeListCellLower(row, "calcMethod").contains(kLow);
            case "COMP_NM" -> executeListCellLower(row, "compNm").contains(kLow);
            case "COMP_ID", "MID" -> executeListCellLower(row, "compId").contains(kLow);
            case "APPROVAL_NO" -> executeListCellLower(row, "targetPeriodText").contains(kLow)
                    || executeListCellLower(row, "trnId").contains(kLow)
                    || executeListCellLower(row, "payNo").contains(kLow);
            case "ORDER_NO" -> executeListCellLower(row, "payNo").contains(kLow)
                    || executeListCellLower(row, "trnId").contains(kLow);
            case "ROUTE" -> executeListCellLower(row, "pgRootNo").contains(kLow);
            case "CURRENCY" -> executeListCellLower(row, "curType").contains(kLow);
            case "STATUS" -> {
                String psLow = executeListCellLower(row, "payStatus");
                if (psLow.contains(kLow)) {
                    yield true;
                }
                if ("확정".equals(kw)) {
                    yield psLow.contains("calculated");
                }
                if ("미확정".equals(kw)) {
                    yield psLow.contains("pending");
                }
                yield false;
            }
            case "SETTLEMENT_PUBLISH_STS" -> executeListCellLower(row, "settlementPublishSts").contains(kLow);
            case "PAYOUT_HOLD_YN" -> executeListCellLower(row, "payoutHoldYn").contains(kLow);
            case "AMOUNT" -> executeListMoneyNorm(row.get("amount")).contains(kNorm)
                    || executeListMoneyNorm(row.get("feeAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("holdAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("settlementBatchFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("remittanceFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("receivableAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("receivableDeductAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("settleAmt")).contains(kNorm)
                    || executeListCellLower(row, "txnCnt").contains(kLow);
            default -> true;
        };
    }

    private static boolean executeListRowMatches(Map<String, Object> row, String fieldType, String keyword) {
        String ft = fieldType == null || fieldType.isBlank() ? "ALL" : fieldType.trim().toUpperCase(Locale.ROOT);
        String kw = keyword == null ? "" : keyword.trim();
        if (!"ALL".equals(ft) && kw.isEmpty()) {
            return true;
        }
        if ("ALL".equals(ft) && kw.isEmpty()) {
            return true;
        }
        String kNorm = kw.replace(",", "").trim();
        String kLow = kw.toLowerCase(Locale.ROOT);
        return switch (ft) {
            case "ALL" -> executeListCellLower(row, "calcCycle").contains(kLow)
                    || executeListCellLower(row, "calcMethod").contains(kLow)
                    || executeListCellLower(row, "compNm").contains(kLow)
                    || executeListCellLower(row, "compId").contains(kLow)
                    || executeListCellLower(row, "targetPeriodText").contains(kLow)
                    || executeListCellLower(row, "cadenceGuideKr").contains(kLow)
                    || executeListCellLower(row, "settlementPublishSts").contains(kLow)
                    || executeListCellLower(row, "payoutHoldYn").contains(kLow)
                    || String.valueOf(row.getOrDefault("settlementRunId", "")).toLowerCase(Locale.ROOT).contains(kLow)
                    || executeListCellLower(row, "pgRootNo").contains(kLow)
                    || executeListCellLower(row, "curType").contains(kLow)
                    || executeListCellLower(row, "status").contains(kLow)
                    || ("확정".equals(kw) && executeListCellLower(row, "status").contains("calculated"))
                    || ("미확정".equals(kw) && executeListCellLower(row, "status").contains("pending"))
                    || executeListMoneyNorm(row.get("targetAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("payAmount")).contains(kNorm)
                    || executeListMoneyNorm(row.get("approveAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("cancelAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("totalFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("rollingReserveAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("settlementBatchFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("remittanceFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("receivableAmt")).contains(kNorm)
                    || executeListCellLower(row, "txnCnt").contains(kLow);
            case "CALC_CYCLE" -> executeListCellLower(row, "calcCycle").contains(kLow);
            case "CALC_METHOD" -> executeListCellLower(row, "calcMethod").contains(kLow);
            case "COMP_NM" -> executeListCellLower(row, "compNm").contains(kLow);
            case "COMP_ID", "MID" -> executeListCellLower(row, "compId").contains(kLow);
            case "APPROVAL_NO" -> executeListCellLower(row, "targetPeriodText").contains(kLow);
            case "ROUTE" -> executeListCellLower(row, "pgRootNo").contains(kLow);
            case "CURRENCY" -> executeListCellLower(row, "curType").contains(kLow);
            case "STATUS" -> {
                String sLow = executeListCellLower(row, "status");
                if (sLow.contains(kLow)) {
                    yield true;
                }
                if ("확정".equals(kw)) {
                    yield sLow.contains("calculated");
                }
                if ("미확정".equals(kw)) {
                    yield sLow.contains("pending");
                }
                yield false;
            }
            case "SETTLEMENT_PUBLISH_STS" -> executeListCellLower(row, "settlementPublishSts").contains(kLow);
            case "PAYOUT_HOLD_YN" -> executeListCellLower(row, "payoutHoldYn").contains(kLow);
            case "AMOUNT" -> executeListMoneyNorm(row.get("targetAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("payAmount")).contains(kNorm)
                    || executeListMoneyNorm(row.get("approveAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("cancelAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("totalFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("rollingReserveAmt")).contains(kNorm)
                    || executeListMoneyNorm(row.get("settlementBatchFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("remittanceFee")).contains(kNorm)
                    || executeListMoneyNorm(row.get("receivableAmt")).contains(kNorm)
                    || executeListCellLower(row, "txnCnt").contains(kLow);
            default -> true;
        };
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
        return FeeListTxnAmountService.isFeeListMerchantDeductionStatus(st);
    }

    private Map<String, Object> buildFeeListRowMap(PgTrnsctn t,
                                                   Map<String, Long> monthCbCountCache,
                                                   Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
                                                   Map<String, PayListRowContext> ctxByMerchant,
                                                   Map<String, CommissionPolicy> polCache,
                                                   FeeCurrencyRoundResolver feeResolver,
                                                   Map<Long, Set<LocalDate>> holidayCache) {
        String compId = t.getMerchantId().trim();
        PayListRowContext payCtx = ctxByMerchant.get(compId);
        SettlementSetting feeVatSs = payCtx != null ? payCtx.getSettlement() : null;
        CommissionPolicy pol = polCache.computeIfAbsent(compId, this::resolveCommissionPolicyForMerchant);
        Map<String, Object> payRow = PayListItemDto.from(t, payCtx, resolveLedgerDisplayZoneId());
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
        FeeListTxnAmountService.FeeListTxnAmounts feeAmts = feeListTxnAmountService.compute(
                t, payCtx, pol, payCurKey, feeResolver, monthCbCountCache, tiersByPolicyId);
        BigDecimal totalFeeBd = feeAmts.totalFee();
        BigDecimal feeVatOut = feeAmts.feeVat();
        BigDecimal expectedPayoutBd = feeAmts.expectedPayout();
        BigDecimal settlementAmtBd = feeAmts.settlementAmt();
        BigDecimal rollingHoldEstBd = feeAmts.rollingHoldEst();
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
        String calcCycleRaw = "";
        if (feeVatSs != null && feeVatSs.getCalcCycle() != null && !feeVatSs.getCalcCycle().isBlank()) {
            calcCycleRaw = feeVatSs.getCalcCycle().trim();
        }
        m.put("calcCycle", calcCycleRaw);
        LocalDate trnDate = t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : null;
        Object trnDateObj = payRow.get("trnDate");
        if (trnDateObj != null && !String.valueOf(trnDateObj).isBlank()) {
            try {
                trnDate = LocalDate.parse(String.valueOf(trnDateObj).trim());
            } catch (DateTimeParseException ignored) {
                /* createdAt fallback */
            }
        }
        String expectedSettle = "";
        if (!calcCycleRaw.isEmpty() && trnDate != null) {
            expectedSettle = SettlementExpectedDateResolver.formatExpectedSettlementDate(
                    trnDate, t.getCreatedAt(), calcCycleRaw, holidaysForMerchant(holidayCache, payCtx));
        }
        m.put("expectedSettleDate", expectedSettle.isEmpty() ? "—" : expectedSettle);
        m.put("trnDate", payRow.get("trnDate"));
        m.put("trnTime", payRow.get("trnTime"));
        m.put("payCompletedAt", payRow.get("payCompletedAt"));
        m.put("routeNo", payRow.get("routeNo"));
        m.put("chillTransactionId", payRow.get("chillTransactionId"));
        m.put("transactionId", payRow.get("transactionId"));
        m.put("trnId", payRow.get("trnId"));
        m.put("settledYn", payRow.get("settledYn"));
        m.put("status", t.getStatus());
        m.put("statusNm", PayListStatusBarBuckets.pgStatusDisplayLabel(t.getStatus()));
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
     * 정산결과: 정산배포 / 정산대기 화면. {@code publishTab}=PENDING|HOLD.
     * 행은 정산실행과 동일 맵 + 주기 안내({@code cadenceGuideKr})·배포상태.
     */
    @GetMapping("/result/list")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> settlementResultList(
            Authentication authentication,
            @RequestParam("searchPublishTab") String publishTab,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String tab = publishTab != null ? publishTab.trim().toUpperCase(Locale.ROOT) : "";
        if (!"PENDING".equals(tab) && !"HOLD".equals(tab)) {
            return ResponseEntity.ok(ApiResponse.fail("publishTab 은 PENDING 또는 HOLD 여야 합니다.", "BAD_REQUEST"));
        }
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }
        List<SettlementRun> raw = new ArrayList<>(settlementCalcService.listRuns(searchFromDate, searchToDate));
        raw.removeIf(r -> {
            String sts = r.getSettlementPublishSts() != null ? r.getSettlementPublishSts().trim().toUpperCase(Locale.ROOT) : "";
            if ("PENDING".equals(tab)) {
                /* 비어 있으면 배포 전으로 간주(구 스키마·수동 보정 행 호환). HOLD/DISTRIBUTED 는 제외 */
                if (sts.isEmpty()) {
                    return false;
                }
                return !"PENDING".equals(sts);
            }
            return !"HOLD".equals(sts);
        });
        if (allowedMerchants != null) {
            raw.removeIf(r -> {
                String mid = r.getMerchantId();
                return mid == null || mid.isBlank() || !allowedMerchantsContains(allowedMerchants, mid.trim());
            });
        }
        Map<Long, BigDecimal> autoDeficitByRun = indexAutoDeficitTotalsByRunId(raw);
        String effFt = "ALL";
        String effKw = "";
        if (searchFieldType != null && !searchFieldType.isBlank()) {
            effFt = searchFieldType.trim().toUpperCase(Locale.ROOT);
            effKw = searchKeyword != null ? searchKeyword.trim() : "";
        } else if (searchCompId != null && !searchCompId.isBlank()) {
            effFt = "COMP_ID";
            effKw = searchCompId.trim();
        }
        if ("COMP_NM".equals(effFt) && effKw.isEmpty()) {
            effFt = "ALL";
        }
        final String effFtFinal = effFt;
        final String effKwFinal = effKw;
        ExecuteListRowCache rowCache = buildExecuteListRowCache(raw);
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (SettlementRun r : raw) {
            mapped.add(toMap(r, searchFromDate, searchToDate, autoDeficitByRun, rowCache));
        }
        List<Map<String, Object>> filtered = mapped.stream()
                .filter(m -> executeListRowMatches(m, effFtFinal, effKwFinal))
                .collect(Collectors.toList());
        Sort.Direction sd = sortDirectionFromSearchOrderDir(searchOrderDir);
        Comparator<Map<String, Object>> byDt = Comparator.comparing(
                (Map<String, Object> m) -> parseCalcDtForExecuteSort(m.get("calcDt")),
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Map<String, Object>> byMid = Comparator.comparing(
                m -> String.valueOf(m.getOrDefault("compId", "")),
                Comparator.nullsLast(String::compareTo));
        Comparator<Map<String, Object>> cmp = byDt.thenComparing(byMid);
        if (sd == Sort.Direction.DESC) {
            cmp = cmp.reversed();
        }
        filtered.sort(cmp);
        int from = (page - 1) * size;
        int to = Math.min(from + size, filtered.size());
        List<Map<String, Object>> rows = (from < filtered.size() && from < to)
                ? new ArrayList<>(filtered.subList(from, to))
                : new ArrayList<>();
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(filtered.size());
        pr.setTotalPages(size > 0 ? Math.max(1, (int) Math.ceil((double) filtered.size() / size)) : 1);
        attachFeeCurrencyMeta(pr);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    @PostMapping("/result/distribute")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> settlementResultDistribute(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        return settlementResultPublishMutate(authentication, body, true);
    }

    @PostMapping("/result/hold")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> settlementResultHold(
            Authentication authentication,
            @RequestBody Map<String, Object> body) {
        return settlementResultPublishMutate(authentication, body, false);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> settlementResultPublishMutate(
            Authentication authentication,
            Map<String, Object> body,
            boolean distribute) {
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
        String remark = body != null && body.get("remark") != null ? body.get("remark").toString().trim() : "";
        int changed = 0;
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
            if (!allowedMerchantsContains(allowedMerchants, mid)) {
                return ResponseEntity.ok(ApiResponse.fail("해당 정산 건에 대한 권한이 없습니다: " + id, "FORBIDDEN"));
            }
            String publishStsRaw = r.getSettlementPublishSts();
            String publishStsNorm = publishStsRaw != null ? publishStsRaw.trim().toUpperCase(Locale.ROOT) : "";
            boolean publishOpen = publishStsNorm.isEmpty() || "PENDING".equals(publishStsNorm);
            if (distribute) {
                if (!publishOpen) {
                    continue;
                }
                r.setSettlementPublishSts("DISTRIBUTED");
                r.setPayoutHoldYn("N");
            } else {
                if (!publishOpen) {
                    continue;
                }
                r.setSettlementPublishSts("HOLD");
                r.setPayoutHoldYn("Y");
                if (!remark.isEmpty()) {
                    r.setPayoutHoldRemark(remark.length() > 800 ? remark.substring(0, 800) : remark);
                }
            }
            settlementRunRepository.save(r);
            changed++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("changedCount", changed);
        out.put("requestedCount", ids.size());
        out.put("distribute", distribute);
        return ResponseEntity.ok(ApiResponse.ok(out));
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
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchFieldType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String searchCalcProcType,
            @RequestParam(required = false) String searchOrderDir,
            @RequestParam(required = false) String searchExecuteListMode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }

        LocalDate effFrom = searchFromDate != null ? searchFromDate : LocalDate.now().minusYears(1);
        LocalDate effTo = searchToDate != null ? searchToDate : LocalDate.now();

        String effFt = "ALL";
        String effKw = "";
        if (searchFieldType != null && !searchFieldType.isBlank()) {
            effFt = searchFieldType.trim().toUpperCase(Locale.ROOT);
            effKw = searchKeyword != null ? searchKeyword.trim() : "";
        } else if (searchCompId != null && !searchCompId.isBlank()) {
            effFt = "COMP_ID";
            effKw = searchCompId.trim();
        }
        if ("COMP_NM".equals(effFt) && effKw.isEmpty()) {
            effFt = "ALL";
        }
        final String effFtFinal = effFt;
        final String effKwFinal = effKw;
        final String calcProcWant = normalizeExecuteListCalcProcFilter(searchCalcProcType);

        Sort.Direction sd = sortDirectionFromSearchOrderDir(searchOrderDir);
        boolean recentFirst = searchExecuteListMode == null
                || !"PERIOD".equalsIgnoreCase(searchExecuteListMode.trim());

        boolean simpleKeyword = "ALL".equals(effFtFinal) && effKwFinal.isEmpty();
        int safeSize = Math.min(Math.max(size, 1), 500);
        int safePage = Math.max(1, page);

        if (simpleKeyword) {
            Sort sort = buildExecuteListSort(recentFirst, sd);
            Pageable pg = PageRequest.of(safePage - 1, safeSize, sort);
            Page<SettlementRun> runPage = fetchExecuteListPage(effFrom, effTo, allowedMerchants, calcProcWant, pg);
            List<SettlementRun> pageRows = runPage.getContent();
            Map<Long, BigDecimal> autoDeficitByRun = indexAutoDeficitTotalsByRunId(pageRows);
            ExecuteListRowCache cache = buildExecuteListRowCache(pageRows);
            List<Map<String, Object>> rows = new ArrayList<>(pageRows.size());
            for (SettlementRun r : pageRows) {
                rows.add(toMap(r, searchFromDate, searchToDate, autoDeficitByRun, cache));
            }
            PageResult<Map<String, Object>> pr = new PageResult<>();
            pr.setList(rows);
            pr.setPage(safePage);
            pr.setSize(safeSize);
            pr.setTotalElements(runPage.getTotalElements());
            long totalEl = runPage.getTotalElements();
            pr.setTotalPages(totalEl == 0 ? 1 : Math.max(1, runPage.getTotalPages()));
            attachFeeCurrencyMeta(pr);
            return ResponseEntity.ok(ApiResponse.ok(pr));
        }

        List<SettlementRun> raw = new ArrayList<>(settlementCalcService.listRuns(searchFromDate, searchToDate));
        if (allowedMerchants != null) {
            raw.removeIf(r -> {
                String mid = r.getMerchantId();
                return mid == null || mid.isBlank() || !allowedMerchantsContains(allowedMerchants, mid.trim());
            });
        }
        Map<Long, BigDecimal> autoDeficitByRun = indexAutoDeficitTotalsByRunId(raw);

        ExecuteListRowCache rowCache = buildExecuteListRowCache(raw);
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (SettlementRun r : raw) {
            mapped.add(toMap(r, searchFromDate, searchToDate, autoDeficitByRun, rowCache));
        }
        List<Map<String, Object>> mappedForKeyword = calcProcWant.isEmpty()
                ? mapped
                : mapped.stream().filter(m -> executeListRowMatchesCalcProc(m, calcProcWant)).collect(Collectors.toList());
        List<Map<String, Object>> filtered = mappedForKeyword.stream()
                .filter(m -> executeListRowMatches(m, effFtFinal, effKwFinal))
                .collect(Collectors.toList());

        Comparator<Map<String, Object>> cmp;
        if (recentFirst) {
            Comparator<Map<String, Object>> byCalcDt = Comparator.comparing(
                    (Map<String, Object> m) -> parseCalcDtForExecuteSort(m.get("calcDt")),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            Comparator<Map<String, Object>> byCreated = Comparator.comparing(
                    (Map<String, Object> m) -> parseRunCreatedAtForExecuteSort(m.get("runCreatedAt")),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            Comparator<Map<String, Object>> byRid = Comparator.comparing(
                    (Map<String, Object> m) -> parseSettlementRunIdForSort(m.get("settlementRunId")),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            cmp = byCalcDt.thenComparing(byCreated).thenComparing(byRid);
            if (sd == Sort.Direction.DESC) {
                cmp = cmp.reversed();
            }
        } else {
            Comparator<Map<String, Object>> byDt = Comparator.comparing(
                    (Map<String, Object> m) -> parseCalcDtForExecuteSort(m.get("calcDt")),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            Comparator<Map<String, Object>> byMid = Comparator.comparing(
                    m -> String.valueOf(m.getOrDefault("compId", "")),
                    Comparator.nullsLast(String::compareTo));
            cmp = byDt.thenComparing(byMid);
            if (sd == Sort.Direction.DESC) {
                cmp = cmp.reversed();
            }
        }
        filtered.sort(cmp);

        int from = (page - 1) * size;
        int to = Math.min(from + size, filtered.size());
        List<Map<String, Object>> rows = (from < filtered.size() && from < to)
                ? new ArrayList<>(filtered.subList(from, to))
                : new ArrayList<>();

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(filtered.size());
        pr.setTotalPages(size > 0 ? Math.max(1, (int) Math.ceil((double) filtered.size() / size)) : 1);
        attachFeeCurrencyMeta(pr);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    private static final int SETTLEMENT_EXECUTE_RUN_TX_MAX = 2500;

    /** {@code allowedMerchants} 가 null 이면 전체 허용(ADMIN). 코드 대소문자 불일치 허용. */
    private static boolean allowedMerchantsContains(Set<String> allowedMerchants, String merchantId) {
        if (allowedMerchants == null) {
            return true;
        }
        if (allowedMerchants.isEmpty()) {
            return false;
        }
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        String m = merchantId.trim();
        for (String c : allowedMerchants) {
            if (c != null && m.equalsIgnoreCase(c.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 최근정산(RECENT): 정산일(calc_dt) 우선 — 실행 등록 시각만 우선하면 오늘 배치 행이 과거 정산일 실행을 목록 앞에서 밀어내 첫 페이지가 당일분만으로 보이는 문제가 난다.
     * 같은 정산일 내에서는 등록 시각·id 로 안정 정렬.
     */
    private static Sort buildExecuteListSort(boolean recentFirst, Sort.Direction sd) {
        if (recentFirst) {
            return Sort.by(sd, "calcDt", "createdAt", "id");
        }
        return Sort.by(sd, "calcDt", "merchantId");
    }

    private static List<String> buildExecuteListLowMids(Set<String> allowedMerchants) {
        if (allowedMerchants == null) {
            return List.of();
        }
        return allowedMerchants.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private Page<SettlementRun> fetchExecuteListPage(
            LocalDate from,
            LocalDate to,
            Set<String> allowedMerchants,
            String calcProcWant,
            Pageable pg) {
        List<String> lowMids = buildExecuteListLowMids(allowedMerchants);
        boolean restricted = allowedMerchants != null;
        if (restricted && lowMids.isEmpty()) {
            return Page.empty(pg);
        }
        String proc = calcProcWant != null ? calcProcWant.trim() : "";
        if (proc.isEmpty()) {
            if (!restricted) {
                return settlementRunRepository.findByCalcDtBetween(from, to, pg);
            }
            return settlementRunRepository.findByCalcDtBetweenAndMerchantNormIn(from, to, lowMids, pg);
        }
        if (!restricted) {
            return settlementRunRepository.findByCalcDtBetweenAndMerchantCalcProcEquals(
                    from, to, OrgLevel.MERCHANT, proc, pg);
        }
        return settlementRunRepository.findByCalcDtBetweenAndMerchantNormInAndMerchantCalcProcEquals(
                from, to, lowMids, OrgLevel.MERCHANT, proc, pg);
    }

    /**
     * 정산실행 그리드 행 매핑 시 가맹·설정·바인딩·수수료정책을 배치로 적재해 N+1 조회를 줄입니다.
     */
    private record ExecuteListRowCache(
            Map<String, OrgUnit> ouByMerchantNormKey,
            Map<Long, SettlementSetting> ssByOrgUnitId,
            Map<Long, List<MerchantPgBinding>> bindsByOrgUnitId,
            Map<String, CommissionPolicy> policyByMerchantNormKey
    ) {
        OrgUnit resolveOu(String merchantId) {
            if (merchantId == null || merchantId.isBlank()) {
                return null;
            }
            return ouByMerchantNormKey.get(merchantId.trim().toLowerCase(Locale.ROOT));
        }

        Optional<SettlementSetting> settingForOu(Long orgUnitId) {
            if (orgUnitId == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(ssByOrgUnitId.get(orgUnitId));
        }

        List<MerchantPgBinding> bindsForOu(Long orgUnitId) {
            if (orgUnitId == null) {
                return List.of();
            }
            List<MerchantPgBinding> b = bindsByOrgUnitId.get(orgUnitId);
            return b != null ? b : List.of();
        }

        String currency(String merchantId) {
            if (merchantId == null || merchantId.isBlank()) {
                return "KRW";
            }
            CommissionPolicy pol = policyByMerchantNormKey.get(merchantId.trim().toLowerCase(Locale.ROOT));
            if (pol == null) {
                return "KRW";
            }
            String c = pol.getCurrencyCode();
            return c != null && !c.isBlank() ? c.trim().toUpperCase(Locale.ROOT) : "KRW";
        }
    }

    private ExecuteListRowCache buildExecuteListRowCache(List<SettlementRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return new ExecuteListRowCache(Map.of(), Map.of(), Map.of(), Map.of());
        }
        LinkedHashSet<String> mids = new LinkedHashSet<>();
        for (SettlementRun r : runs) {
            String mid = r.getMerchantId();
            if (mid != null && !mid.isBlank()) {
                mids.add(mid.trim());
            }
        }
        if (mids.isEmpty()) {
            return new ExecuteListRowCache(Map.of(), Map.of(), Map.of(), Map.of());
        }
        Map<String, OrgUnit> ouByKey = new HashMap<>();
        for (OrgUnit ou : orgUnitRepository.findByCodeIn(mids)) {
            if (ou != null && ou.getCode() != null) {
                ouByKey.put(ou.getCode().trim().toLowerCase(Locale.ROOT), ou);
            }
        }
        for (String mid : mids) {
            String k = mid.toLowerCase(Locale.ROOT);
            if (!ouByKey.containsKey(k)) {
                orgUnitRepository.findByCodeIgnoreCase(mid).ifPresent(ou -> ouByKey.putIfAbsent(k, ou));
            }
        }
        Set<Long> ouIds = ouByKey.values().stream()
                .map(OrgUnit::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SettlementSetting> ssMap = new HashMap<>();
        if (!ouIds.isEmpty()) {
            for (SettlementSetting ss : settlementSettingRepository.findByOrgUnitIdIn(ouIds)) {
                if (ss != null && ss.getOrgUnitId() != null) {
                    ssMap.putIfAbsent(ss.getOrgUnitId(), ss);
                }
            }
        }
        Map<Long, List<MerchantPgBinding>> bindsMap = new HashMap<>();
        if (!ouIds.isEmpty()) {
            for (MerchantPgBinding b : merchantPgBindingRepository.findByOrgUnitIdInOrderByOrgUnitIdAscSortOrderAsc(ouIds)) {
                if (b == null || b.getOrgUnitId() == null) {
                    continue;
                }
                bindsMap.computeIfAbsent(b.getOrgUnitId(), x -> new ArrayList<>()).add(b);
            }
        }
        Map<String, CommissionPolicy> polMap = new HashMap<>();
        for (String mid : mids) {
            polMap.put(mid.toLowerCase(Locale.ROOT), resolveCommissionPolicyForMerchant(mid));
        }
        return new ExecuteListRowCache(
                Collections.unmodifiableMap(ouByKey),
                Collections.unmodifiableMap(ssMap),
                bindsMap.entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue()))),
                Collections.unmodifiableMap(polMap));
    }

    /**
     * 정산 실행 한 건에 대해, 집계에 사용한 거래 구간과 동일하게 조회한 거래 목록(결제내역 그리드와 동일 필드).
     * 대량 방지를 위해 최대 {@value #SETTLEMENT_EXECUTE_RUN_TX_MAX}건까지 반환하며 초과 시 {@code truncated=true}.
     *
     * @param txnScope 비어 있거나 {@code RUN_WINDOW}(기본): 실행 저장 집계 구간.
     *                 {@code MERCHANT_CALC_DAY}: 해당 실행의 정산일(calc_dt) 달력일 00:00~말일 해당 가맹 전체 거래(정산배포 화면용).
     */
    @GetMapping("/execute/runTransactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeRunTransactions(
            Authentication authentication,
            @RequestParam long settlementRunId,
            @RequestParam(required = false) String txnScope) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("조회 가능한 가맹점이 없습니다.", "FORBIDDEN"));
        }
        Optional<SettlementRun> opt = settlementRunRepository.findById(settlementRunId);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("정산 실행 건을 찾을 수 없습니다.", "NOT_FOUND"));
        }
        SettlementRun r = opt.get();
        String mid = r.getMerchantId();
        if (mid == null || mid.isBlank()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드가 없습니다.", "VALIDATION"));
        }
        String midTrim = mid.trim();
        if (!allowedMerchantsContains(allowedMerchants, midTrim)) {
            return ResponseEntity.ok(ApiResponse.fail("해당 정산 실행에 대한 조회 권한이 없습니다.", "FORBIDDEN"));
        }
        String scope = txnScope != null ? txnScope.trim().toUpperCase(Locale.ROOT) : "";
        boolean merchantCalcDay = "MERCHANT_CALC_DAY".equals(scope);
        LocalDateTime fromAt;
        LocalDateTime toAt;
        if (merchantCalcDay) {
            LocalDate cd = r.getCalcDt() != null ? r.getCalcDt() : LocalDate.now();
            fromAt = cd.atStartOfDay();
            toAt = cd.atTime(LocalTime.MAX);
        } else {
            fromAt = r.resolvePeriodStartAt();
            toAt = r.resolvePeriodEndAt();
        }
        PayListService.SettlementWindowPayRows sw = payListService.listRowsForSettlementWindow(
                midTrim, fromAt, toAt, SETTLEMENT_EXECUTE_RUN_TX_MAX);
        List<Map<String, Object>> rows = new ArrayList<>(sw.getRows());
        Integer inc = r.getIncludedTxnCnt();
        boolean cappedToIncluded = !merchantCalcDay && inc != null && inc > 0 && rows.size() > inc;
        if (cappedToIncluded) {
            rows = new ArrayList<>(rows.subList(0, inc));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("settlementRunId", settlementRunId);
        payload.put("merchantId", midTrim);
        payload.put("txnScope", merchantCalcDay ? "MERCHANT_CALC_DAY" : "RUN_WINDOW");
        payload.put("includedTxnCnt", r.getIncludedTxnCnt());
        payload.put("periodFromAt", fromAt.toString());
        payload.put("periodToAt", toAt.toString());
        payload.put("maxRows", SETTLEMENT_EXECUTE_RUN_TX_MAX);
        payload.put("truncated", sw.isTruncated());
        payload.put("cappedToIncludedTxnCnt", cappedToIncluded);
        payload.put("returnedCount", rows.size());
        payload.put("list", rows);
        /* 정산 집계 구간 전체의 승인(대상) 매출 합 — 상세 목록 행 합과 무관(tb_settlement_run.approve_amt) */
        FeeListRoundingPolicy srpRun = resolveSettlementLedgerRoundPolicy();
        payload.put("runApproveAmt", settlementMoneyDouble(r.getApproveAmt() != null ? r.getApproveAmt() : BigDecimal.ZERO, srpRun));
        payload.put("runSettlementBatchFeeAmt", settlementMoneyDouble(
                r.getSettlementBatchFeeAmt() != null ? r.getSettlementBatchFeeAmt() : BigDecimal.ZERO, srpRun));
        payload.put("runRemittanceFeeAmt", settlementMoneyDouble(
                r.getRemittanceFeeAmt() != null ? r.getRemittanceFeeAmt() : BigDecimal.ZERO, srpRun));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("feeCurrencyFormatByCur", resolveFeeCurrencyRoundResolver().toClientByCurrencyMap());
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        payload.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.ok(payload));
    }

    /** 비자동 정산구분만 계산(AUTO 는 배치 전용). 화면 「수동실행」. */
    @PostMapping("/execute/run")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> executeRun(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false, defaultValue = "false") boolean reconcile) {
        /* 기간 지정 시: 기존 수동 실행(레거시 유지) */
        if (fromDate != null || toDate != null) {
            LocalDate runFrom = fromDate != null ? fromDate : LocalDate.now().minusDays(1);
            LocalDate runTo = toDate != null ? toDate : LocalDate.now();
            if (merchantId != null && !merchantId.isBlank()) {
                Optional<String> gridErr = settlementCalcService.validateManualSettlementExecuteWindow(
                        merchantId.trim(), runTo);
                if (gridErr.isPresent()) {
                    return ResponseEntity.ok(ApiResponse.fail(gridErr.get(), "BAD_REQUEST"));
                }
            }
            List<SettlementRun> runs = settlementCalcService.execute(runFrom, runTo, merchantId, true);
            Map<Long, BigDecimal> autoDef = indexAutoDeficitTotalsByRunId(runs);
            ExecuteListRowCache execCache = buildExecuteListRowCache(runs);
            List<Map<String, Object>> list = new ArrayList<>(runs.size());
            for (SettlementRun r : runs) {
                Map<String, Object> row = toMap(r, runFrom, runTo, autoDef, execCache);
                if (reconcile) {
                    row.put("feeReconciliation", settlementRunFeeReconciliationService.reconcile(r));
                }
                list.add(row);
            }
            return ResponseEntity.ok(ApiResponse.ok(list));
        }

        /* 기간 미지정 시: calcCycle·AUTO·마감시간 등과 동일 규칙으로 자동 실행(스케줄 배치와 공유) */
        LocalDate today = LocalDate.now(SEOUL);
        List<SettlementRun> allRuns = settlementAutoRunService.runDueSettlements(today, merchantId, true);
        Map<Long, BigDecimal> autoDefAll = indexAutoDeficitTotalsByRunId(allRuns);
        ExecuteListRowCache execCacheAll = buildExecuteListRowCache(allRuns);
        List<Map<String, Object>> list = new ArrayList<>(allRuns.size());
        for (SettlementRun r : allRuns) {
            Map<String, Object> row = toMap(r, null, null, autoDefAll, execCacheAll);
            if (reconcile) {
                row.put("feeReconciliation", settlementRunFeeReconciliationService.reconcile(r));
            }
            list.add(row);
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /**
     * 정산 실행 행에 저장된 정산주기(스냅샷). 없으면 빈 문자열.
     * 화면 표시는 {@link #resolveRunCalcCycleForExecuteDisplay}: 스냅샷이 있으면 그대로(변경 후에도 과거 행 유지),
     * 없으면 가맹 현재 설정 주기로 보조 표시(구데이터·스냅샷 도입 이전 행; 당시와 다를 수 있음).
     */
    private static String resolveRunCalcCycleRaw(SettlementRun r) {
        if (r == null) {
            return "";
        }
        String s = r.getCalcCycleSnapshot();
        return s != null && !s.isBlank() ? s.trim() : "";
    }

    /**
     * 정산실행·가맹점정산내역 그리드: 스냅샷 우선, 없으면 {@code settingsCycleFallback}(가맹 현재 설정 정규화 코드).
     */
    private String resolveRunCalcCycleForExecuteDisplay(SettlementRun r, String settingsCycleFallback) {
        String snap = resolveRunCalcCycleRaw(r);
        if (!snap.isEmpty()) {
            return snap;
        }
        if (settingsCycleFallback != null && !settingsCycleFallback.isBlank()) {
            return SettlementPeriodResolver.normalizeCalcCycle(settingsCycleFallback.trim());
        }
        return "";
    }

    private static String normalizeCalcCycleFromSettlementSetting(Optional<SettlementSetting> ssOpt) {
        return ssOpt.map(ss -> {
            String c = ss.getCalcCycle();
            return c != null && !c.isBlank() ? SettlementPeriodResolver.normalizeCalcCycle(c.trim()) : "";
        }).orElse("");
    }

    /**
     * 정산실행 그리드용 행 맵. {@link SettlementRun#getPeriodFrom()}/{@link SettlementRun#getPeriodTo()} 가 있으면
     * period* 키에 반영합니다. {@code targetPeriodText}: RT는 거래번호·승인번호·마감(초 단위) 한 줄,
     * 그 외는 {@code yyyy-MM-dd HH:mm:ss ~ yyyy-MM-dd HH:mm:ss} 집계 구간(격자·당일누적은 시각 포함).
     * {@code receivableAmt}: 지급부족 자동 미수금({@code AUTO_DEFICIT:runId}) 발생액, 없으면 0.
     * {@code txnCnt}: 집계에 포함된 거래 건수({@link SettlementRun#getIncludedTxnCnt()}). 스키마 이전 행은 null.
     */
    private Map<String, Object> toMap(SettlementRun r, LocalDate queryFrom, LocalDate queryTo, Map<Long, BigDecimal> autoDeficitTotalByRunId) {
        return toMap(r, queryFrom, queryTo, autoDeficitTotalByRunId, null);
    }

    private Map<String, Object> toMap(
            SettlementRun r,
            LocalDate queryFrom,
            LocalDate queryTo,
            Map<Long, BigDecimal> autoDeficitTotalByRunId,
            ExecuteListRowCache cache) {
        Map<String, Object> m = new HashMap<>();
        m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : null);
        String mid = r.getMerchantId();
        m.put("compId", mid);
        OrgUnit ouExec = null;
        if (cache != null) {
            ouExec = cache.resolveOu(mid);
        } else if (mid != null) {
            ouExec = orgUnitRepository.findByCode(mid).orElse(null);
        }
        m.put("compNm", ouExec != null ? ouExec.getName() : (mid != null ? mid : "-"));
        m.put("curType", cache != null ? cache.currency(mid) : resolveMerchantStatementCurrency(mid));
        FeeListRoundingPolicy srp = resolveSettlementLedgerRoundPolicy();
        m.put("targetAmt", r.getApproveAmt() != null && r.getCancelAmt() != null
                ? FeeListRoundingPolicy.round(r.getApproveAmt().subtract(r.getCancelAmt()), srp).toPlainString() : "0");
        m.put("status", r.getStatus());
        m.put("approveAmt", settlementMoneyDouble(r.getApproveAmt(), srp));
        m.put("cancelAmt", settlementMoneyDouble(r.getCancelAmt(), srp));
        m.put("totalFee", settlementMoneyDouble(r.getTotalFee(), srp));
        m.put("rollingReserveAmt", settlementMoneyDouble(r.getRollingReserveAmt(), srp));
        m.put("settlementBatchFee", r.getSettlementBatchFeeAmt() != null
                ? settlementMoneyDouble(r.getSettlementBatchFeeAmt(), srp) : null);
        m.put("remittanceFee", null);
        Long runIdForRecv = r.getId();
        BigDecimal receivableBd = BigDecimal.ZERO;
        if (runIdForRecv != null) {
            if (autoDeficitTotalByRunId != null) {
                receivableBd = nz(autoDeficitTotalByRunId.get(runIdForRecv));
            } else if (mid != null && !mid.isBlank()) {
                String memoKey = "AUTO_DEFICIT:" + runIdForRecv;
                Optional<MerchantReceivable> recOpt = merchantReceivableRepository.findByMerchantIdAndReasonCodeAndMemo(
                        mid.trim(), SettlementArrearsService.REASON_AUTO_SETTLEMENT_DEFICIT, memoKey);
                if (recOpt.isPresent() && recOpt.get().getTotalAmount() != null) {
                    receivableBd = recOpt.get().getTotalAmount();
                }
            }
        }
        m.put("receivableAmt", settlementMoneyDouble(receivableBd, srp));
        m.put("payAmount", settlementMoneyDouble(r.getPayAmt(), srp));
        Optional<SettlementSetting> ssOptExec = Optional.empty();
        if (ouExec != null) {
            ssOptExec = cache != null
                    ? cache.settingForOu(ouExec.getId())
                    : settlementSettingRepository.findByOrgUnitId(ouExec.getId());
        }
        String cycleFb = normalizeCalcCycleFromSettlementSetting(ssOptExec);
        String calcCycleRaw = resolveRunCalcCycleForExecuteDisplay(r, cycleFb);
        m.put("calcCycle", calcCycleRaw);
        if (ouExec != null) {
            m.put("calcMethod", ssOptExec.map(ss -> labelCalcProcType(ss.getCalcProcType())).orElse(""));
            m.put("calcProcType", ssOptExec.map(ss -> {
                String v = ss.getCalcProcType();
                return v != null && !v.isBlank() ? v.trim().toUpperCase(Locale.ROOT) : "";
            }).orElse(""));
            m.put("pgRootNo", cache != null
                    ? resolveMerchantPgRootNoFromBinds(cache.bindsForOu(ouExec.getId()))
                    : resolveMerchantPgRootNo(ouExec.getId()));
        } else {
            m.put("calcMethod", "");
            m.put("calcProcType", "");
            m.put("pgRootNo", "-");
        }
        m.put("periodFrom", r.getPeriodFrom() != null ? r.getPeriodFrom().toString() : null);
        m.put("periodTo", r.getPeriodTo() != null ? r.getPeriodTo().toString() : null);
        m.put("periodEndAt", r.getPeriodEndAt() != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r.getPeriodEndAt()) : null);
        m.put("calcCycleSnapshot", resolveRunCalcCycleRaw(r));
        m.put("targetPeriodText", buildSettlementTargetPeriodLabel(r, cycleFb));
        m.put("txnCnt", r.getIncludedTxnCnt());
        m.put("settlementRunId", r.getId());
        String snapCad = resolveRunCalcCycleRaw(r);
        m.put("cadenceGuideKr", SettlementPublishCadence.cadenceGuideKr(!snapCad.isEmpty() ? snapCad : cycleFb));
        m.put("settlementPublishSts", r.getSettlementPublishSts() != null ? r.getSettlementPublishSts() : "");
        String ph = r.getPayoutHoldYn();
        m.put("payoutHoldYn", (ph != null && !ph.isBlank()) ? ph.trim().toUpperCase(Locale.ROOT) : "N");
        m.put("runCreatedAt", r.getCreatedAt() != null
                ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(r.getCreatedAt())
                : "");
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

    private static final String SETTLEMENT_GRID_MISSING_PERIOD_MSG =
            " ~ (미기록) — M/H 격자(H12 등)는 마감시각(period_end_at)이 있어야 12시간·N분 단위 구간으로 표시됩니다.";

    private String formatSettlementTargetRangeDual(LocalDateTime startDt, LocalDateTime endDt) {
        return TrnTimeDualZoneDisplay.formatDualLineDateTimeRange(startDt, endDt, resolveLedgerDisplayZoneId());
    }

    private static String appendSameSuffixToDualPeriodLines(String dualTwoLines, String suffix) {
        if (dualTwoLines == null || dualTwoLines.isBlank()) {
            return "";
        }
        int nl = dualTwoLines.indexOf('\n');
        if (nl < 0) {
            return dualTwoLines + suffix;
        }
        return dualTwoLines.substring(0, nl) + suffix + "\n" + dualTwoLines.substring(nl + 1) + suffix;
    }

    /**
     * 정산대상기간 표시. RT(건당)은 거래번호·승인번호 + 마감 시각(전산설정 표준 + JP), 그 외는 구간을 동일 두 줄로 표시.
     * 격자(H/M) 복원에는 실행 시점 주기({@link #resolveRunCalcCycleRaw})를 우선 사용합니다(스냅샷 없으면 주기 미상으로 격자 복원 생략).
     */
    private String buildSettlementTargetPeriodLabel(SettlementRun r, String settingsCycleFallback) {
        String snap = resolveRunCalcCycleRaw(r);
        String display = resolveRunCalcCycleForExecuteDisplay(r, settingsCycleFallback);
        String labelCycleRaw = !snap.isEmpty() ? snap : display;
        String norm = SettlementPeriodResolver.normalizeCalcCycle(labelCycleRaw != null ? labelCycleRaw : "");
        if (SettlementCycleTiming.isRtPerTransactionCode(norm)
                && r.getPeriodFrom() != null
                && r.getPeriodTo() != null
                && r.getPeriodFrom().equals(r.getPeriodTo())
                && r.getPeriodEndAt() != null) {
            return buildRtSettlementTargetPeriodLine(r);
        }
        return buildSettlementTargetPeriodLabelNonRt(r, norm, labelCycleRaw);
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

    private String formatRtPeriodLine(String trnId, String approvalLabel, LocalDateTime closeAt) {
        String head = "거래번호 " + trnId + " / 승인번호 " + approvalLabel;
        if (closeAt == null) {
            return head + " / 마감 -";
        }
        return head + "\n마감\n" + TrnTimeDualZoneDisplay.formatDualLineDateTime(closeAt, resolveLedgerDisplayZoneId());
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

    /**
     * 비-RT: 집계 구간을 전산설정 표준시간대·JP 두 줄로 표시.
     * M5·H1 등 격자(당일 누적 TM/TH 제외)는 반개구간 {@code [start, end)} 를
     * {@code start ~ end} 로 보이게 하며, 끝 시각은 DB의 {@code period_end_at}(배타 상한)과 동일한 정각(예 M5: 00:05:00)입니다.
     */
    private String buildSettlementTargetPeriodLabelNonRt(SettlementRun r,
                                                                  String calcCycleNorm, String labelCycleRaw) {
        if (r.getPeriodFrom() != null && r.getPeriodTo() != null) {
            LocalDateTime startDt;
            LocalDateTime endDt;
            if (r.getPeriodEndAt() != null) {
                LocalDateTime endExclusive = r.getPeriodEndAt();
                LocalDateTime inferredStart = SettlementCycleTiming.subDailySlotStartInclusiveFromEndExclusive(
                        endExclusive, calcCycleNorm != null ? calcCycleNorm : "");
                if (inferredStart != null) {
                    startDt = inferredStart.truncatedTo(ChronoUnit.SECONDS);
                    /* 격자: 00:00~00:05 구간은 "00:00:00 ~ 00:05:00" (끝은 배타 상한 표기) */
                    endDt = endExclusive.truncatedTo(ChronoUnit.SECONDS);
                } else {
                    startDt = r.getPeriodFrom().atStartOfDay();
                    endDt = endExclusive.truncatedTo(ChronoUnit.SECONDS);
                }
                return formatSettlementTargetRangeDual(startDt, endDt);
            }
            startDt = r.getPeriodFrom().atStartOfDay();
            if (SettlementCycleTiming.isPlainSubDailyGridClosingCode(calcCycleNorm)) {
                return appendSameSuffixToDualPeriodLines(
                        TrnTimeDualZoneDisplay.formatDualLineDateTime(startDt, resolveLedgerDisplayZoneId()),
                        SETTLEMENT_GRID_MISSING_PERIOD_MSG);
            }
            endDt = r.getPeriodTo().atTime(23, 59, 59);
            return formatSettlementTargetRangeDual(startDt, endDt);
        }
        /* period 미기록 시 목록 검색일(from~to)로 채우지 않음 — 2주 검색이면 잘못 14일 구간으로 보였음 */
        if (r.getCalcDt() != null) {
            LocalDate d = r.getCalcDt();
            LocalDateTime startDt = d.atStartOfDay();
            LocalDateTime endDt = d.atTime(23, 59, 59);
            return formatSettlementTargetRangeDual(startDt, endDt);
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
        BigDecimal aggPerTx = BigDecimal.ZERO;
        BigDecimal aggPaySide = BigDecimal.ZERO;
        BigDecimal aggCancel = BigDecimal.ZERO;
        BigDecimal aggRefundVoid = BigDecimal.ZERO;
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

            aggPerTx = aggPerTx.add(FeeListRoundingPolicy.round(BigDecimal.valueOf(br.perTxFee()), feeListRp));
            double paySideD = br.payFee() + br.usdtFee() + br.fxFee() + br.fee3dsFee()
                    + br.extraFee1() + br.extraFee2() + br.extraFee3() + br.extraFee4()
                    + br.usageFee() + br.failFee() + br.chargebackFee();
            aggPaySide = aggPaySide.add(FeeListRoundingPolicy.round(BigDecimal.valueOf(paySideD), feeListRp));
            aggCancel = aggCancel.add(FeeListRoundingPolicy.round(BigDecimal.valueOf(br.cancelFee()), feeListRp));
            double refundVoidD = br.voidFee() + br.manualVoidFee() + br.refundFee();
            aggRefundVoid = aggRefundVoid.add(FeeListRoundingPolicy.round(BigDecimal.valueOf(refundVoidD), feeListRp));
        }
        row.put("feeCnt", feeCnt);

        FeeListRoundingPolicy ledgerRp = resolveSettlementLedgerRoundPolicy();
        BigDecimal storedFeeBd = FeeListRoundingPolicy.round(nz(r.getTotalFee()), ledgerRp);
        BigDecimal perTxLedger = FeeListRoundingPolicy.round(aggPerTx, ledgerRp);
        BigDecimal paySideRawLedger = FeeListRoundingPolicy.round(aggPaySide, ledgerRp);
        BigDecimal cancelLedger = FeeListRoundingPolicy.round(aggCancel, ledgerRp);
        BigDecimal refundVoidLedger = FeeListRoundingPolicy.round(aggRefundVoid, ledgerRp);
        BigDecimal residualFee = storedFeeBd.subtract(perTxLedger.add(paySideRawLedger).add(cancelLedger).add(refundVoidLedger));
        BigDecimal paySideLedger = paySideRawLedger.add(residualFee);
        row.put("feeSumPerTx", settlementMoneyDouble(perTxLedger, ledgerRp));
        row.put("feeSumPaySide", settlementMoneyDouble(paySideLedger, ledgerRp));
        row.put("feeSumCancel", settlementMoneyDouble(cancelLedger, ledgerRp));
        row.put("feeSumRefundVoid", settlementMoneyDouble(refundVoidLedger, ledgerRp));
        BigDecimal perTxTotal = FeeListRoundingPolicy.round(nz(pol.getPerTxFee()).multiply(BigDecimal.valueOf(txs.size())), ledgerRp);
        row.put("perTxFeeAmt", settlementMoneyDouble(perTxTotal, ledgerRp));

        if (r.getSettlementBatchFeeAmt() != null) {
            row.put("settlementPerTxFeeAmt", settlementMoneyDouble(r.getSettlementBatchFeeAmt(), ledgerRp));
        } else {
            BigDecimal settlePerTxTotalLegacy = FeeListRoundingPolicy.round(
                    nz(pol.getFeeSettlementPerTx()).multiply(BigDecimal.valueOf(txs.size())), ledgerRp);
            row.put("settlementPerTxFeeAmt", settlementMoneyDouble(settlePerTxTotalLegacy, ledgerRp));
        }
        row.put("remittanceFee", null);

        row.put("extraFeesAmt", settlementMoneyDouble(sumExtra, ledgerRp));

        BigDecimal vatFeeBase = nz(r.getTotalFee());
        if (r.getSettlementBatchFeeAmt() != null) {
            vatFeeBase = vatFeeBase.add(nz(r.getSettlementBatchFeeAmt()));
        }
        BigDecimal feeVat = MerchantFeeVatUtil.vatOnFeeAmount(vatFeeBase, feeVatSs, ledgerRp.decimalPlaces());
        row.put("feeVat", settlementMoneyDouble(feeVat, ledgerRp));

        BigDecimal holdBd = FeeListRoundingPolicy.round(nz(r.getRollingReserveAmt()), ledgerRp);
        BigDecimal netBd = nz(r.getApproveAmt()).subtract(nz(r.getCancelAmt()));
        if (rollingEff.rollingDays() > 0 && rollingEff.rollingPct().signum() > 0) {
            row.put("holdRate", rollingEff.rollingPct().setScale(4, RoundingMode.HALF_UP));
        } else if (netBd.signum() > 0 && holdBd.signum() > 0) {
            row.put("holdRate", holdBd.multiply(BigDecimal.valueOf(100))
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

    private static String resolveMerchantPgRootNoFromBinds(List<MerchantPgBinding> binds) {
        if (binds == null || binds.isEmpty()) {
            return "-";
        }
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
     * 정산 지급부족으로 자동 생성된 미수금({@code AUTO_DEFICIT:{runId}}) 원금을 실행 ID 기준으로 한 번에 조회합니다.
     */
    private Map<Long, BigDecimal> indexAutoDeficitTotalsByRunId(List<SettlementRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> memos = new ArrayList<>();
        for (SettlementRun run : runs) {
            if (run.getId() != null) {
                memos.add("AUTO_DEFICIT:" + run.getId());
            }
        }
        if (memos.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MerchantReceivable> found = merchantReceivableRepository.findByReasonCodeAndMemoIn(
                SettlementArrearsService.REASON_AUTO_SETTLEMENT_DEFICIT, memos);
        Map<Long, BigDecimal> out = new HashMap<>();
        for (MerchantReceivable mr : found) {
            if (mr == null || mr.getMemo() == null) {
                continue;
            }
            String memo = mr.getMemo().trim();
            if (!memo.startsWith("AUTO_DEFICIT:")) {
                continue;
            }
            try {
                long rid = Long.parseLong(memo.substring("AUTO_DEFICIT:".length()).trim());
                if (mr.getTotalAmount() != null) {
                    out.put(rid, mr.getTotalAmount());
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    /**
     * 가맹점정산 그리드와 동일 키로 {@link SettlementRun} 한 건을 매핑합니다.
     * 금액·수수료·담보금(보류)·지급액은 실행 저장값이며, 건수·부가세·건당·정산건당·기타%·보유율은 동일 집계 구간 거래로
     * 수수료내역·정산실행(calcOne) 규칙에 맞춰 채웁니다.
     *
     * @param queryFrom queryTo 목록 조회 구간(정산대상기간 문구·검색용, null이면 라벨만 생략 가능)
     * @param autoDeficitTotalByRunId {@code AUTO_DEFICIT:{runId}} 미수금의 원금(total). null이면 행마다 DB 조회
     */
    private Map<String, Object> toFranchiseSettlementRunRow(
            SettlementRun r,
            LocalDate queryFrom,
            LocalDate queryTo,
            Map<Long, BigDecimal> autoDeficitTotalByRunId) {
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
        FeeListRoundingPolicy ledgerRp = resolveSettlementLedgerRoundPolicy();
        BigDecimal approve = nz(r.getApproveAmt());
        BigDecimal cancel = nz(r.getCancelAmt());
        BigDecimal netBd = approve.subtract(cancel);
        BigDecimal feeBd = FeeListRoundingPolicy.round(nz(r.getTotalFee()), ledgerRp);
        BigDecimal holdBd = FeeListRoundingPolicy.round(nz(r.getRollingReserveAmt()), ledgerRp);
        BigDecimal settleBd = FeeListRoundingPolicy.round(nz(r.getPayAmt()), ledgerRp);
        row.put("amount", settlementMoneyDouble(netBd, ledgerRp));
        row.put("feeAmt", settlementMoneyDouble(feeBd, ledgerRp));
        row.put("holdAmt", settlementMoneyDouble(holdBd, ledgerRp));
        row.put("settleAmt", settlementMoneyDouble(settleBd, ledgerRp));
        if (netBd.signum() > 0 && feeBd.signum() > 0) {
            row.put("feeRate", feeBd.multiply(BigDecimal.valueOf(100))
                    .divide(netBd, 4, RoundingMode.HALF_UP));
        } else {
            row.put("feeRate", BigDecimal.ZERO);
        }
        applyFranchiseSettlementFeeBreakdown(r, row, compId);
        row.put("settlementBatchFee", r.getSettlementBatchFeeAmt() != null
                ? settlementMoneyDouble(r.getSettlementBatchFeeAmt(), ledgerRp)
                : row.get("settlementPerTxFeeAmt"));
        Optional<SettlementSetting> ssOptFr = ou != null
                ? settlementSettingRepository.findByOrgUnitId(ou.getId())
                : Optional.empty();
        String cycleFbFr = normalizeCalcCycleFromSettlementSetting(ssOptFr);
        String calcCycleRaw = resolveRunCalcCycleForExecuteDisplay(r, cycleFbFr);
        row.put("calcCycle", calcCycleRaw);
        row.put("calcCycleSnapshot", resolveRunCalcCycleRaw(r));
        String receivableRecoveryMode = "AUTO";
        if (ou != null) {
            receivableRecoveryMode = receivableRecoveryModeService.resolveEffectiveModeForMerchantOrgUnitId(ou.getId());
            row.put("calcMethod", ssOptFr.map(ss -> labelCalcProcType(ss.getCalcProcType())).orElse(""));
            row.put("pgRootNo", resolveMerchantPgRootNo(ou.getId()));
        } else {
            row.put("calcMethod", "");
            row.put("pgRootNo", "-");
        }
        row.put("targetPeriodText", buildSettlementTargetPeriodLabel(r, cycleFbFr));
        row.put("txnCnt", r.getIncludedTxnCnt());
        Object snapObj = row.get("calcCycleSnapshot");
        String snapCadFr = snapObj != null ? String.valueOf(snapObj).trim() : "";
        row.put("cadenceGuideKr", SettlementPublishCadence.cadenceGuideKr(!snapCadFr.isEmpty() ? snapCadFr : cycleFbFr));
        row.put("settlementPublishSts", r.getSettlementPublishSts() != null ? r.getSettlementPublishSts().trim() : "");
        String phYn = r.getPayoutHoldYn();
        row.put("payoutHoldYn", (phYn != null && !phYn.isBlank()) ? phYn.trim().toUpperCase(Locale.ROOT) : "N");
        Long runId = r.getId();
        BigDecimal receivableBd = BigDecimal.ZERO;
        if (runId != null) {
            if (autoDeficitTotalByRunId != null) {
                receivableBd = nz(autoDeficitTotalByRunId.get(runId));
            } else if (!compId.isBlank()) {
                String memoKey = "AUTO_DEFICIT:" + runId;
                Optional<MerchantReceivable> recOpt = merchantReceivableRepository.findByMerchantIdAndReasonCodeAndMemo(
                        compId, SettlementArrearsService.REASON_AUTO_SETTLEMENT_DEFICIT, memoKey);
                if (recOpt.isPresent() && recOpt.get().getTotalAmount() != null) {
                    receivableBd = recOpt.get().getTotalAmount();
                }
            }
        }
        row.put("receivableAmt", settlementMoneyDouble(receivableBd, ledgerRp));
        BigDecimal recvAppliedBd = nz(r.getReceivableAppliedAmt());
        row.put("receivableDeductAmt", settlementMoneyDouble(recvAppliedBd, ledgerRp));
        row.put("receivableProcessNm", franchiseReceivableProcessNm(
                receivableRecoveryMode, recvAppliedBd, r.getStatus(), receivableBd, r.getPayAmt()));
        row.put("receivableRecoveryMode", receivableRecoveryMode);
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
        String runStatusStr = r.getStatus() != null ? r.getStatus() : "-";
        row.put("payStatus", runStatusStr);
        row.put("status", runStatusStr);
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
        FeeListRoundingPolicy ledgerRp = resolveSettlementLedgerRoundPolicy();
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
        m.put("settleAmt", settlementMoneyDouble(settleAmt, ledgerRp));
        m.put("hqRate", hqRate);
        m.put("regionalRate", regionalRate);
        m.put("masterRate", masterRate);
        m.put("branchRate", branchRate);
        m.put("agencyRate", agencyRate);
        m.put("salesOfficeRate", salesOfficeRate);
        if (r.getDistHqFeeAmt() != null) {
            m.put("hqFee", settlementMoneyDouble(r.getDistHqFeeAmt(), ledgerRp));
            m.put("regionalFee", settlementMoneyDouble(r.getDistRegionalFeeAmt(), ledgerRp));
            m.put("masterFee", settlementMoneyDouble(r.getDistMasterFeeAmt(), ledgerRp));
            m.put("branchFee", settlementMoneyDouble(r.getDistBranchFeeAmt(), ledgerRp));
            m.put("agencyFee", settlementMoneyDouble(r.getDistAgencyFeeAmt(), ledgerRp));
            m.put("salesOfficeFee", settlementMoneyDouble(r.getDistSalesOfficeFeeAmt(), ledgerRp));
        } else {
            m.put("hqFee", settlementPctFeeDouble(settleAmt, hqRate, ledgerRp));
            m.put("regionalFee", settlementPctFeeDouble(settleAmt, regionalRate, ledgerRp));
            m.put("masterFee", settlementPctFeeDouble(settleAmt, masterRate, ledgerRp));
            m.put("branchFee", settlementPctFeeDouble(settleAmt, branchRate, ledgerRp));
            m.put("agencyFee", settlementPctFeeDouble(settleAmt, agencyRate, ledgerRp));
            m.put("salesOfficeFee", settlementPctFeeDouble(settleAmt, salesOfficeRate, ledgerRp));
        }
        String hq = "", regional = "", master = "", branch = "", agency = "", salesOffice = "";
        OrgUnit cur = orgUnitRepository.findByCode(compId).orElse(null);
        for (int i = 0; i < 8 && cur != null; i++) {
            if (cur.getOrgLevel() != null) {
                switch (cur.getOrgLevel()) {
                    case HEADQUARTERS -> hq = cur.getName();
                    case REGIONAL -> regional = cur.getName();
                    case MASTER_DIST -> master = cur.getName();
                    case BRANCH -> branch = cur.getName();
                    case AGENCY -> agency = cur.getName();
                    case SALES_OFFICE -> salesOffice = cur.getName();
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
        m.put("salesOfficeNm", salesOffice);
        m.put("curType", resolveMerchantStatementCurrency(compId));
        return m;
    }

    private static final class DistributionAgg {
        final LocalDate calcDt;
        final String rowOrgCode;
        BigDecimal settleAmt = BigDecimal.ZERO;
        BigDecimal hqFee = BigDecimal.ZERO;
        BigDecimal regionalFee = BigDecimal.ZERO;
        BigDecimal masterFee = BigDecimal.ZERO;
        BigDecimal branchFee = BigDecimal.ZERO;
        BigDecimal agencyFee = BigDecimal.ZERO;
        BigDecimal salesOfficeFee = BigDecimal.ZERO;
        /** 병합된 가맹 정산 실행 행 수 */
        long runCnt;
        /** {@link SettlementRun#getIncludedTxnCnt()} 합(구간 내 결제 건수). null 실행은 {@link #runsMissingTxnCnt}에서 보정 */
        long includedTxnCntSum;
        /** included_txn_cnt 가 null 인 실행 수(건수는 실행 1건당 1로 가산) */
        int runsMissingTxnCnt;
        BigDecimal approveAmtSum = BigDecimal.ZERO;
        BigDecimal cancelAmtSum = BigDecimal.ZERO;
        /** 취소 금액이 있는 가맹 건수 */
        long cancelRunCnt;
        BigDecimal aprvFeeSum = BigDecimal.ZERO;
        BigDecimal canFeeSum = BigDecimal.ZERO;
        /** 집계 행에 합쳐인 가맹 실행들의 정책·거래 통화(알파) */
        private final java.util.TreeSet<String> curTypes = new java.util.TreeSet<>();

        DistributionAgg(LocalDate calcDt, String rowOrgCode) {
            this.calcDt = calcDt;
            this.rowOrgCode = rowOrgCode;
        }

        void merge(SettlementRun r, Map<String, Object> dr, OrgLevel rollupLevel) {
            if (rollupLevel == null) {
                return;
            }
            Object rawCur = dr != null ? dr.get("curType") : null;
            if (rawCur != null) {
                String c = String.valueOf(rawCur).trim().toUpperCase(Locale.ROOT);
                if (!c.isEmpty()) {
                    curTypes.add(c);
                }
            }
            settleAmt = settleAmt.add(BigDecimal.valueOf(asDoubleStatic(dr.get("settleAmt"))));
            BigDecimal slice = feeSliceForRollupBd(dr, rollupLevel);
            switch (rollupLevel) {
                case HEADQUARTERS -> hqFee = hqFee.add(slice);
                case REGIONAL -> regionalFee = regionalFee.add(slice);
                case MASTER_DIST -> masterFee = masterFee.add(slice);
                case BRANCH -> branchFee = branchFee.add(slice);
                case AGENCY -> agencyFee = agencyFee.add(slice);
                case SALES_OFFICE -> salesOfficeFee = salesOfficeFee.add(slice);
                default -> { }
            }
            runCnt++;
            Integer incTxn = r.getIncludedTxnCnt();
            if (incTxn != null) {
                includedTxnCntSum += incTxn.intValue();
            } else {
                runsMissingTxnCnt++;
            }
            BigDecimal ap = r.getApproveAmt() != null ? r.getApproveAmt() : BigDecimal.ZERO;
            BigDecimal ca = r.getCancelAmt() != null ? r.getCancelAmt() : BigDecimal.ZERO;
            approveAmtSum = approveAmtSum.add(ap);
            cancelAmtSum = cancelAmtSum.add(ca);
            if (ca.signum() > 0) {
                cancelRunCnt++;
            }
            BigDecimal feeSum = slice;
            BigDecimal denom = ap.add(ca);
            if (denom.signum() == 0) {
                aprvFeeSum = aprvFeeSum.add(feeSum);
            } else {
                aprvFeeSum = aprvFeeSum.add(feeSum.multiply(ap).divide(denom, 8, RoundingMode.HALF_UP));
                canFeeSum = canFeeSum.add(feeSum.multiply(ca).divide(denom, 8, RoundingMode.HALF_UP));
            }
        }

        /** 유통망 행(rollup) 조직 단계에 해당하는 수수료 구간만 합산 — 타 단계 금액을 같은 행에 섞지 않음 */
        private static BigDecimal feeSliceForRollupBd(Map<String, Object> dr, OrgLevel rollupLevel) {
            return switch (rollupLevel) {
                case HEADQUARTERS -> BigDecimal.valueOf(asDoubleStatic(dr.get("hqFee")));
                case REGIONAL -> BigDecimal.valueOf(asDoubleStatic(dr.get("regionalFee")));
                case MASTER_DIST -> BigDecimal.valueOf(asDoubleStatic(dr.get("masterFee")));
                case BRANCH -> BigDecimal.valueOf(asDoubleStatic(dr.get("branchFee")));
                case AGENCY -> BigDecimal.valueOf(asDoubleStatic(dr.get("agencyFee")));
                case SALES_OFFICE -> BigDecimal.valueOf(asDoubleStatic(dr.get("salesOfficeFee")));
                default -> BigDecimal.ZERO;
            };
        }

        private static double asDoubleStatic(Object v) {
            if (v == null) {
                return 0d;
            }
            if (v instanceof Number n) {
                return n.doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(v));
            } catch (Exception e) {
                return 0d;
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
        FeeListRoundingPolicy rp = resolveSettlementLedgerRoundPolicy();
        m.put("calcDt", agg.calcDt != null ? agg.calcDt.toString() : "");
        m.put("settleMonth", agg.calcDt != null ? agg.calcDt.format(DateTimeFormatter.ofPattern("yyyy-MM")) : "");
        m.put("compId", rowOrg.getCode());
        m.put("compNm", rowOrg.getName());
        m.put("orgDivNm", rowOrg.getOrgLevel() != null ? rowOrg.getOrgLevel().getNameKo() : "");
        if (agg.curTypes.isEmpty()) {
            m.put("curType", "");
        } else if (agg.curTypes.size() == 1) {
            m.put("curType", agg.curTypes.first());
        } else {
            m.put("curType", String.join("/", agg.curTypes));
        }
        m.put("settleAmt", settlementMoneyDouble(agg.settleAmt, rp));
        m.put("hqFee", settlementMoneyDouble(agg.hqFee, rp));
        m.put("regionalFee", settlementMoneyDouble(agg.regionalFee, rp));
        m.put("masterFee", settlementMoneyDouble(agg.masterFee, rp));
        m.put("branchFee", settlementMoneyDouble(agg.branchFee, rp));
        m.put("agencyFee", settlementMoneyDouble(agg.agencyFee, rp));
        m.put("salesOfficeFee", settlementMoneyDouble(agg.salesOfficeFee, rp));
        m.put("hqRate", BigDecimal.ZERO);
        m.put("regionalRate", BigDecimal.ZERO);
        m.put("masterRate", BigDecimal.ZERO);
        m.put("branchRate", BigDecimal.ZERO);
        m.put("agencyRate", BigDecimal.ZERO);
        m.put("salesOfficeRate", BigDecimal.ZERO);
        fillHierarchyColumnsFrom(rowOrg, m, idToOu);

        BigDecimal apAmt = agg.approveAmtSum;
        BigDecimal caAmt = agg.cancelAmtSum;
        long aprvCntDisplay = agg.includedTxnCntSum + agg.runsMissingTxnCnt;
        m.put("aprvCnt", aprvCntDisplay);
        m.put("aprvAmt", settlementMoneyDouble(apAmt, rp));
        m.put("aprvFeeCnt", aprvCntDisplay);
        m.put("settlementRunCnt", agg.runCnt);
        m.put("aprvFeePct", feePctStringBd(agg.aprvFeeSum, apAmt));
        m.put("aprvFeeSum", settlementMoneyDouble(agg.aprvFeeSum, rp));
        m.put("aprvFeeVat", settlementMoneyDouble(vatFromFeeBd(agg.aprvFeeSum, rp), rp));

        m.put("canCnt", agg.cancelRunCnt);
        m.put("canAmt", settlementMoneyDouble(caAmt, rp));
        m.put("canFeeCnt", agg.cancelRunCnt);
        m.put("canFeePct", feePctStringBd(agg.canFeeSum, caAmt));
        m.put("canFeeSum", settlementMoneyDouble(agg.canFeeSum, rp));
        m.put("canFeeVat", settlementMoneyDouble(vatFromFeeBd(agg.canFeeSum, rp), rp));
        return m;
    }

    private static BigDecimal vatFromFeeBd(BigDecimal feeSum, FeeListRoundingPolicy rp) {
        if (feeSum == null || feeSum.signum() <= 0 || rp == null) {
            return BigDecimal.ZERO;
        }
        return FeeListRoundingPolicy.round(feeSum.divide(BigDecimal.TEN, 8, RoundingMode.HALF_UP), rp);
    }

    /** 수수료 ÷ 금액 × 100 (%) */
    private static String feePctStringBd(BigDecimal fee, BigDecimal amt) {
        if (amt == null || amt.signum() <= 0) {
            return "0";
        }
        BigDecimal f = fee != null ? fee : BigDecimal.ZERO;
        return f.multiply(BigDecimal.valueOf(100))
                .divide(amt, 2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 본사~영업점 컬럼: 행 조직(rowOrg)은 해당 단계 칸에, 나머지는 상위 조직만 채움 */
    private void fillHierarchyColumnsFrom(OrgUnit rowOrg, Map<String, Object> m, Map<Long, OrgUnit> idToOu) {
        String hq = "", regional = "", master = "", branch = "", agency = "", salesOffice = "";
        OrgUnit cur = parentOrg(rowOrg.getParentId(), idToOu);
        for (int i = 0; i < 16 && cur != null; i++) {
            if (cur.getOrgLevel() != null) {
                switch (cur.getOrgLevel()) {
                    case HEADQUARTERS -> hq = nzName(hq, cur.getName());
                    case REGIONAL -> regional = nzName(regional, cur.getName());
                    case MASTER_DIST -> master = nzName(master, cur.getName());
                    case BRANCH -> branch = nzName(branch, cur.getName());
                    case AGENCY -> agency = nzName(agency, cur.getName());
                    case SALES_OFFICE -> salesOffice = nzName(salesOffice, cur.getName());
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
                case AGENCY -> agency = rowOrg.getName() != null ? rowOrg.getName() : agency;
                case SALES_OFFICE -> salesOffice = rowOrg.getName() != null ? rowOrg.getName() : salesOffice;
                default -> { }
            }
        }
        m.put("hqNm", hq);
        m.put("regionalNm", regional);
        m.put("masterNm", master);
        m.put("branchNm", branch);
        m.put("agencyNm", agency);
        m.put("salesOfficeNm", salesOffice);
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
        FeeListRoundingPolicy rp = resolveSettlementLedgerRoundPolicy();
        Map<String, BigDecimal> holdByMerchant = new HashMap<>();
        Map<String, BigDecimal> unpaidByMerchant = new HashMap<>();
        for (SettlementRun r : runs) {
            String compId = r.getMerchantId();
            if (compId == null || compId.isBlank()) continue;
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
                continue;
            }
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            if (ou == null) continue;
            SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(ou.getId()).orElse(null);
            boolean payHold = ss != null && "Y".equalsIgnoreCase(ss.getPayHoldYn());
            BigDecimal payAmt = FeeListRoundingPolicy.round(r.getPayAmt() != null ? r.getPayAmt() : BigDecimal.ZERO, rp);
            BigDecimal unpaid = FeeListRoundingPolicy.round(r.getTotalFee() != null ? r.getTotalFee() : BigDecimal.ZERO, rp);
            if (payHold && payAmt.signum() > 0 && !"Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
                holdByMerchant.merge(compId, payAmt, BigDecimal::add);
            }
            if (unpaid.signum() > 0) {
                unpaidByMerchant.merge(compId, unpaid, BigDecimal::add);
            }
        }

        Map<String, BigDecimal> deductedByMerchant = new HashMap<>();
        for (BalanceDeduction d : balanceDeductionRepository.findAll()) {
            String mid = d.getMerchantId();
            long amt = d.getAmount() != null ? d.getAmount() : 0L;
            if (mid == null || mid.isBlank() || amt <= 0) continue;
            deductedByMerchant.merge(mid, BigDecimal.valueOf(amt), BigDecimal::add);
        }

        Set<String> merchants = new LinkedHashSet<>();
        merchants.addAll(holdByMerchant.keySet());
        merchants.addAll(unpaidByMerchant.keySet());
        merchants.addAll(deductedByMerchant.keySet());
        for (String compId : merchants) {
            if (!allowedMerchantsContains(allowedMerchants, compId.trim())) {
                continue;
            }
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            String compNm = ou != null ? ou.getName() : compId;
            if (searchCompId != null && !searchCompId.isBlank() && (compId == null || !compId.contains(searchCompId.trim()))) continue;
            if (searchCompNm != null && !searchCompNm.isBlank() && (compNm == null || !compNm.contains(searchCompNm.trim()))) continue;
            BigDecimal bal = holdByMerchant.getOrDefault(compId, BigDecimal.ZERO);
            BigDecimal unpaid = unpaidByMerchant.getOrDefault(compId, BigDecimal.ZERO);
            BigDecimal deducted = deductedByMerchant.getOrDefault(compId, BigDecimal.ZERO);
            BigDecimal remain = FeeListRoundingPolicy.round(bal.subtract(deducted).max(BigDecimal.ZERO), rp);
            Map<String, Object> m = new HashMap<>();
            m.put("compId", compId);
            m.put("compNm", compNm);
            if (combined) {
                m.put("balcAmount", settlementMoneyDouble(bal, rp));
                m.put("unpaidAmount", settlementMoneyDouble(unpaid, rp));
                m.put("deductedAmount", settlementMoneyDouble(deducted, rp));
                m.put("remainAmount", settlementMoneyDouble(remain, rp));
            } else {
                m.put("condition", "ETC");
                m.put("chargeType", "지급보류");
                m.put("payMethod", "계좌");
                m.put("chargeNm", compNm);
                m.put("chargeAmt", settlementMoneyDouble(remain, rp));
                m.put("sumChargeAmt", settlementMoneyDouble(remain, rp));
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

    /** 가맹점별 수수료 정책 — 본사 따름이면 배포 템플릿 스코프와 동일(정산 집계와 일치) */
    private CommissionPolicy resolveCommissionPolicyForMerchant(String merchantId) {
        return commissionService.resolveCommissionPolicyForSettlement(
                merchantId != null ? merchantId.trim() : "");
    }

    private record FeePolicy(
            BigDecimal perTxFee, BigDecimal usageRate, BigDecimal failFee,
            BigDecimal cancelRate, BigDecimal refundRate, BigDecimal payRate,
            BigDecimal feeSettlementPerTx, BigDecimal feeUsdt, BigDecimal feeFx,
            boolean recallIncludeFeeYn, boolean settlementVatApplyYn
    ) {}

    /** 회수관리 등: VAT·회수 시 수수료 포함(본사 설정 + 건별 수수료가 있으면 무조건 포함). 환수액은 수수료내역과 동일한 건별 합산. */
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
        Map<Long, BigDecimal> autoDeficitByRun = indexAutoDeficitTotalsByRunId(runs);
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
            if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
                continue;
            }
            if (!allowedMerchantsContains(allowedMerchants, mid.trim())) {
                continue;
            }
            if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
                continue;
            }
            Map<String, Object> row = toConfirmedSettlementReportListRow(r, fromDate, toDate, autoDeficitByRun);
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

    private Map<String, Object> toConfirmedSettlementReportListRow(
            SettlementRun r, LocalDate queryFrom, LocalDate queryTo, Map<Long, BigDecimal> autoDeficitTotalByRunId) {
        Map<String, Object> row = toFranchiseSettlementRunRow(r, queryFrom, queryTo, autoDeficitTotalByRunId);
        row.put("reportRowKind", "CONFIRMED_SETTLEMENT");
        FeeListRoundingPolicy rp = resolveSettlementLedgerRoundPolicy();
        BigDecimal ap = nz(r.getApproveAmt());
        BigDecimal ca = nz(r.getCancelAmt());
        row.put("approveAmt", settlementMoneyDouble(ap, rp));
        row.put("cancelAmt", settlementMoneyDouble(ca, rp));
        row.put("netPay", settlementMoneyDouble(ap.subtract(ca), rp));
        row.put("payAmount", settlementMoneyDouble(r.getPayAmt(), rp));
        row.putAll(settlementReportService.remittanceFieldsForRun(r));
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
        FeeListRoundingPolicy rp = resolveSettlementLedgerRoundPolicy();
        BigDecimal ap = nz(r.getApproveAmt());
        BigDecimal ca = nz(r.getCancelAmt());
        BigDecimal netBd = ap.subtract(ca);
        m.put("approveAmt", settlementMoneyDouble(ap, rp));
        m.put("cancelAmt", settlementMoneyDouble(ca, rp));
        m.put("netPay", settlementMoneyDouble(netBd, rp));
        m.put("totalFee", settlementMoneyDouble(r.getTotalFee(), rp));
        m.put("rollingReserveAmt", settlementMoneyDouble(r.getRollingReserveAmt(), rp));
        m.put("settlementBatchFee", r.getSettlementBatchFeeAmt() != null
                ? settlementMoneyDouble(r.getSettlementBatchFeeAmt(), rp) : null);
        m.put("payAmount", settlementMoneyDouble(r.getPayAmt(), rp));
        Map<String, Object> remExtras = settlementReportService.remittanceFieldsForRun(r);
        m.put("remittanceFeeBank", remExtras.get("remittanceFeeBank"));
        m.put("remittanceFeeUsdt", remExtras.get("remittanceFeeUsdt"));
        m.put("finalPayAfterRemittance", remExtras.get("finalPayAfterRemittance"));
        m.put("remittanceFee", remExtras.get("remittanceFee"));
        m.put("status", r.getStatus());
        m.put("settlementRunId", r.getId());
        m.put("txnCnt", r.getIncludedTxnCnt());
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
