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
import com.pg.entity.MerchantProfile;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRun;
import com.pg.entity.SettlementSetting;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.BalanceDeductionRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.AuthService;
import com.pg.service.CollateralLedgerService;
import com.pg.service.OrgAccessService;
import com.pg.service.PayListService;
import com.pg.service.SettlementCalcService;
import com.pg.service.SettlementReportService;
import com.pg.service.settlement.SettlementAutoRunService;
import com.pg.util.ChargebackTierResolver;
import com.pg.util.CommissionExtraFeeUtil;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/settlement", produces = "application/json")
public class ApiSettlementController {

    private static final List<String> CHARGEBACK_STATUSES = List.of("30", "31");

    private final SettlementCalcService settlementCalcService;
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

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public ApiSettlementController(SettlementCalcService settlementCalcService,
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
                                   HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository) {
        this.settlementCalcService = settlementCalcService;
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
    }

    private FeeListRoundingPolicy resolveFeeListRoundingPolicy() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(FeeListRoundingPolicy::fromSettings)
                .orElseGet(FeeListRoundingPolicy::defaults);
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
     * 유통망 정산: 로그인 소속 조직부터 하위(영업점)까지만. 가맹점 단위 행은 없음 — 하위 가맹 정산을 조직 행으로 합산.
     * searchCompDiv: REGIONAL/MASTER_DIST/BRANCH/AGENCY/SALES_OFFICE — 비우면 경로상 본사~영업점 각 단계별로 모두 집계(행이 여러 단계로 나뉨).
     */
    @GetMapping("/distributionList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> distributionList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompDiv,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
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

        String levelFilter = searchCompDiv != null ? searchCompDiv.trim() : "";

        List<SettlementRun> list = settlementCalcService.listRuns(searchFromDate, searchToDate);
        Map<String, DistributionAgg> aggMap = new LinkedHashMap<>();
        for (SettlementRun r : list) {
            OrgUnit merchant = orgUnitRepository.findByCode(r.getMerchantId()).orElse(null);
            if (merchant == null || merchant.getOrgLevel() != OrgLevel.MERCHANT) {
                continue;
            }
            if (!userSubtree.contains(merchant.getId())) {
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
                agg.merge(r, dr);
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

        allRows.sort(
                Comparator.<Map<String, Object>, String>comparing(m -> String.valueOf(m.getOrDefault("calcDt", "")))
                        .reversed()
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

    @GetMapping("/franchiseList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> franchiseList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(emptyPage(page, size)));
        }
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);
        List<PgTrnsctn> txList = pgTrnsctnRepository.findForSettlement(null, fromDt, toDt);
        FeePolicy hqPolicy = resolveHqFeePolicy();
        FeeListRoundingPolicy feeListRp = resolveFeeListRoundingPolicy();
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (PgTrnsctn t : txList) {
            String mid = t.getMerchantId();
            if (mid == null || mid.isBlank()) {
                continue;
            }
            if (allowedMerchants != null && !allowedMerchants.contains(mid.trim())) {
                continue;
            }
            Map<String, Object> row = toFranchiseRow(t, hqPolicy, monthCbCountCache, tiersByPolicyId, feeListRp);
            if (searchCompId != null && !searchCompId.isBlank()) {
                String compId = String.valueOf(row.getOrDefault("compId", ""));
                if (!compId.contains(searchCompId.trim())) continue;
            }
            if (searchCompNm != null && !searchCompNm.isBlank()) {
                String compNm = String.valueOf(row.getOrDefault("compNm", row.getOrDefault("merchantNm", "")));
                if (!compNm.contains(searchCompNm.trim())) continue;
            }
            allRows.add(row);
        }
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(allRows.size(), from + Math.max(1, size));
        List<Map<String, Object>> rows = from < allRows.size() ? allRows.subList(from, to) : new ArrayList<>();
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(allRows.size());
        pr.setTotalPages(Math.max(1, (int) Math.ceil((double) allRows.size() / Math.max(1, size))));
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    @GetMapping("/recallMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> recallMng(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
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
        FeeListRoundingPolicy feeListRp = resolveFeeListRoundingPolicy();
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
            FeeListTxnBreakdown br = computeFeeListTxnBreakdown(t, compId, pol, hqPolicy, monthCbCountCache, tiersByPolicyId, feeVatSs, feeListRp);
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
        all.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("calcDt", ""))).reversed());
        return ResponseEntity.ok(ApiResponse.ok(pageOf(all, page, size)));
    }

    /**
     * 수수료내역(/feeList)과 동일한 건별 수수료 항목 합산 + VAT.
     * 환수금(회수) 시 수수료 포함 여부에 쓰인다.
     */
    private record FeeListTxnBreakdown(
            double remittanceTransferFee,
            double usdtTransferFeeUsd,
            double perTxFee,
            double usageFee,
            double failFee,
            double cancelFee,
            double voidFee,
            double manualVoidFee,
            double refundFee,
            double payFee,
            double usdtFee,
            double fxFee,
            double fee3dsFee,
            double settlementPerTxFee,
            double chargebackFee,
            double extraFee1,
            double extraFee2,
            double extraFee3,
            double extraFee4,
            double rollingHoldEst,
            String rollingPctPlain,
            int rollingDays,
            double successFeesSeparate,
            double totalFee,
            BigDecimal feeVatBd
    ) {}

    private double resolveChargebackFee(
            PgTrnsctn t,
            String compId,
            CommissionPolicy pol,
            String st,
            Map<String, Long> monthCbCountCache,
            Map<Long, List<ChargebackFeeTier>> tiersByPolicyId) {
        if (!"30".equals(st) && !"31".equals(st)) {
            return 0d;
        }
        LocalDate cbDay = t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : LocalDate.now();
        YearMonth ymcb = YearMonth.from(cbDay);
        String ck = compId + "|" + ymcb;
        long monthCbCount = monthCbCountCache.computeIfAbsent(ck, k -> {
            LocalDateTime ms = ymcb.atDay(1).atStartOfDay();
            LocalDateTime me = ymcb.plusMonths(1).atDay(1).atStartOfDay();
            return pgTrnsctnRepository.countByMerchantIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    compId, CHARGEBACK_STATUSES, ms, me);
        });
        int mc = (int) Math.min(monthCbCount, Integer.MAX_VALUE);
        Long cpid = pol.getChargebackPolicyId();
        if (cpid != null) {
            List<ChargebackFeeTier> tiers = tiersByPolicyId.computeIfAbsent(cpid, id ->
                    chargebackFeePolicyRepository.findByIdWithTiers(id)
                            .map(ChargebackFeePolicy::getTiers)
                            .orElse(Collections.emptyList()));
            if (!tiers.isEmpty()) {
                return ChargebackTierResolver.feePerCaseForMonthlyCount(mc, tiers).doubleValue();
            }
            return nz(pol.getChargebackFeePerTx()).doubleValue();
        }
        return nz(pol.getChargebackFeePerTx()).doubleValue();
    }

    /**
     * 수수료내역·환수 등 공통 건별 수수료.
     * <ul>
     *   <li>결제(10): 건당(성공) 고정 + 결제%·USDT%·FX%·기타%·3DS 건당 고정·차지백(0) 합산 + 담보(롤링) 추정. 정산 건당·송금(이체) 수수료는 정산 실행·송금 시점 1회 과금이므로 건별 합·총수수료·지급예상에서 제외(정산리포트에서 정산 수수료·송금 수수료로 표현).</li>
     *   <li>실패(F0·99): 실패수수료만</li>
     *   <li>취소(20): 취소수수료만</li>
     *   <li>무효(21·22): 무효/수무효 수수료 + 승인(성공) 시와 동일한 건당·%·기타%(아래 열, 이중 과금)</li>
     *   <li>환불·강제환불(30·31): 환불·차지백 등 + 위와 동일한 승인(성공) 경로 건당·%·기타%(이중 과금)</li>
     * </ul>
     */
    private FeeListTxnBreakdown computeFeeListTxnBreakdown(
            PgTrnsctn t,
            String compId,
            CommissionPolicy pol,
            FeePolicy hqPolicy,
            Map<String, Long> monthCbCountCache,
            Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
            SettlementSetting merchantFeeVatSetting,
            FeeListRoundingPolicy rp) {
        BigDecimal amountBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        int feeScale = rp.decimalPlaces();
        RoundingMode feeRm = rp.roundMode();
        String st = t.getStatus() != null ? t.getStatus().trim() : "";
        double perTxFee = nz(pol.getPerTxFee()).doubleValue();
        double settlementPerTxFee = nz(pol.getFeeSettlementPerTx()).doubleValue();
        double usdtRemitUsd = nz(pol.getUsdtTransferFeeUsd()).doubleValue();
        double usageFee = 0d;
        double failFee = 0d;
        double cancelFee = 0d;
        double voidFee = 0d;
        double manualVoidFee = 0d;
        double refundFee = 0d;
        double chargebackFee = 0d;
        double payFee = 0d;
        double usdtFee = 0d;
        double fxFee = 0d;
        double fee3dsFee = 0d;
        double extraFee1 = 0d;
        double extraFee2 = 0d;
        double extraFee3 = 0d;
        double extraFee4 = 0d;
        double successFeesSeparate = 0d;
        String rollingPctPlain = PercentDecimalHelper.toPlainOneDecimal(nz(pol.getRollingPct()));
        int rollingDays = pol.getRollingDays() != null ? pol.getRollingDays() : 0;
        double rollingHoldEst = 0d;

        if ("F0".equals(st) || "99".equals(st)) {
            failFee = nz(pol.getFailFee()).doubleValue();
        } else if ("20".equals(st)) {
            cancelFee = nz(pol.getCancelRate()).doubleValue();
        } else if ("21".equals(st) || "22".equals(st) || "30".equals(st) || "31".equals(st)) {
            if ("21".equals(st)) {
                voidFee = nz(pol.getVoidFeePerTx()).doubleValue();
            } else if ("22".equals(st)) {
                manualVoidFee = nz(pol.getManualVoidFeePerTx()).doubleValue();
            }
            if ("30".equals(st) || "31".equals(st)) {
                refundFee = nz(pol.getRefundRate()).doubleValue();
                chargebackFee = resolveChargebackFee(t, compId, pol, st, monthCbCountCache, tiersByPolicyId);
            }
            if (amountBd.signum() > 0) {
                payFee = amountBd.multiply(nz(pol.getPayRate())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                usdtFee = amountBd.multiply(nz(pol.getFeeUsdt())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                fxFee = amountBd.multiply(nz(pol.getFeeFx())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                fee3dsFee = FeeListRoundingPolicy.round(nz(pol.getFee3dsRate()), rp).doubleValue();
                extraFee1 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 1, amountBd, feeScale, feeRm).doubleValue();
                extraFee2 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 2, amountBd, feeScale, feeRm).doubleValue();
                extraFee3 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 3, amountBd, feeScale, feeRm).doubleValue();
                extraFee4 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 4, amountBd, feeScale, feeRm).doubleValue();
            }
            successFeesSeparate = perTxFee + payFee + usdtFee + fxFee + fee3dsFee
                    + extraFee1 + extraFee2 + extraFee3 + extraFee4;
        } else if ("10".equals(st)) {
            rollingHoldEst = amountBd.signum() > 0
                    ? amountBd.multiply(nz(pol.getRollingPct())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue()
                    : 0d;
            if (amountBd.signum() > 0) {
                payFee = amountBd.multiply(nz(pol.getPayRate())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                usdtFee = amountBd.multiply(nz(pol.getFeeUsdt())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                fxFee = amountBd.multiply(nz(pol.getFeeFx())).divide(BigDecimal.valueOf(100), feeScale, feeRm).doubleValue();
                fee3dsFee = FeeListRoundingPolicy.round(nz(pol.getFee3dsRate()), rp).doubleValue();
                extraFee1 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 1, amountBd, feeScale, feeRm).doubleValue();
                extraFee2 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 2, amountBd, feeScale, feeRm).doubleValue();
                extraFee3 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 3, amountBd, feeScale, feeRm).doubleValue();
                extraFee4 = CommissionExtraFeeUtil.pctSlotAmountOnApproved(pol, 4, amountBd, feeScale, feeRm).doubleValue();
            }
        }

        double totalFee;
        if ("10".equals(st)) {
            totalFee = Math.max(0d, perTxFee + usageFee + failFee + cancelFee + voidFee + manualVoidFee + refundFee
                    + payFee + usdtFee + fxFee + fee3dsFee + chargebackFee
                    + extraFee1 + extraFee2 + extraFee3 + extraFee4);
        } else if ("F0".equals(st) || "99".equals(st)) {
            totalFee = Math.max(0d, failFee);
        } else if ("20".equals(st)) {
            totalFee = Math.max(0d, cancelFee);
        } else if ("21".equals(st)) {
            totalFee = Math.max(0d, voidFee + successFeesSeparate);
        } else if ("22".equals(st)) {
            totalFee = Math.max(0d, manualVoidFee + successFeesSeparate);
        } else if ("30".equals(st) || "31".equals(st)) {
            totalFee = Math.max(0d, refundFee + chargebackFee + successFeesSeparate);
        } else {
            totalFee = 0d;
        }

        BigDecimal totalFeeBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(totalFee), rp);
        totalFee = totalFeeBd.doubleValue();
        int vatScale = Math.max(0, feeScale);
        BigDecimal feeVatBd = FeeListRoundingPolicy.round(
                MerchantFeeVatUtil.vatOnFeeAmount(totalFeeBd, merchantFeeVatSetting, vatScale), rp);
        /* 수수료내역: 송금(이체) 수수료는 건별 합계에 넣지 않음 — remittance 필드는 0으로 둠 */
        return new FeeListTxnBreakdown(0d, usdtRemitUsd, perTxFee, usageFee, failFee, cancelFee, voidFee, manualVoidFee, refundFee,
                payFee, usdtFee, fxFee, fee3dsFee, settlementPerTxFee, chargebackFee,
                extraFee1, extraFee2, extraFee3, extraFee4, rollingHoldEst, rollingPctPlain, rollingDays, successFeesSeparate, totalFee, feeVatBd);
    }

    /** 수수료내역: 가맹점 거래 1건마다 본사 기본정책의 모든 수수료 항목 계산 표시 (DB 페이징 — 전량 적재 금지) */
    @GetMapping("/feeList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> feeList(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
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
        Pageable pageable = PageRequest.of(pageOneBased - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PgTrnsctn> slice = pgTrnsctnRepository.findAll(spec, pageable);

        FeePolicy hqPolicy = resolveHqFeePolicy();
        FeeListRoundingPolicy feeListRp = resolveFeeListRoundingPolicy();
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
            rows.add(buildFeeListRowMap(t, hqPolicy, monthCbCountCache, tiersByPolicyId, ctxByMerchant, polCache, feeListRp));
        }

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(slice.getNumber() + 1);
        pr.setSize(slice.getSize());
        pr.setTotalElements(slice.getTotalElements());
        pr.setTotalPages(Math.max(1, slice.getTotalPages()));
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    private static String escapeSqlLike(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Map<String, Object> buildFeeListRowMap(PgTrnsctn t, FeePolicy hqPolicy,
                                                   Map<String, Long> monthCbCountCache,
                                                   Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
                                                   Map<String, PayListRowContext> ctxByMerchant,
                                                   Map<String, CommissionPolicy> polCache,
                                                   FeeListRoundingPolicy feeListRp) {
        String compId = t.getMerchantId().trim();
        PayListRowContext payCtx = ctxByMerchant.get(compId);
        SettlementSetting feeVatSs = payCtx != null ? payCtx.getSettlement() : null;
        CommissionPolicy pol = polCache.computeIfAbsent(compId, this::resolveCommissionPolicyForMerchant);
        BigDecimal amountBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        BigDecimal payRateBd = nz(pol.getPayRate());
        FeeListTxnBreakdown br = computeFeeListTxnBreakdown(t, compId, pol, hqPolicy, monthCbCountCache, tiersByPolicyId, feeVatSs, feeListRp);
        BigDecimal totalFeeBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), feeListRp);
        BigDecimal feeVatOut = FeeListRoundingPolicy.round(br.feeVatBd(), feeListRp);
        BigDecimal expectedPayoutBd = FeeListRoundingPolicy.round(amountBd.subtract(totalFeeBd).subtract(feeVatOut), feeListRp);
        double extraFeesSum = br.extraFee1() + br.extraFee2() + br.extraFee3() + br.extraFee4();
        String stRow = t.getStatus() != null ? t.getStatus().trim() : "";
        double txnFixedFeesSum;
        double pctFeesSum;
        if ("10".equals(stRow) || "21".equals(stRow) || "22".equals(stRow) || "30".equals(stRow) || "31".equals(stRow)) {
            txnFixedFeesSum = br.perTxFee();
            pctFeesSum = br.payFee() + br.usdtFee() + br.fxFee() + extraFeesSum;
        } else {
            txnFixedFeesSum = 0d;
            pctFeesSum = 0d;
        }

        Map<String, Object> payRow = PayListItemDto.from(t, payCtx);
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
        Object payCurDisp = payRow.get("currency");
        m.put("payCur", payCurDisp != null && !String.valueOf(payCurDisp).isBlank()
                ? String.valueOf(payCurDisp).trim()
                : (t.getCurType() != null && !t.getCurType().isBlank() ? t.getCurType().trim() : "KRW"));
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
        m.put("rollingHoldEst", feeListMoney(br.rollingHoldEst(), feeListRp).doubleValue());
        m.put("extraFee1", feeListMoney(br.extraFee1(), feeListRp).doubleValue());
        m.put("extraFee2", feeListMoney(br.extraFee2(), feeListRp).doubleValue());
        m.put("extraFee3", feeListMoney(br.extraFee3(), feeListRp).doubleValue());
        m.put("extraFee4", feeListMoney(br.extraFee4(), feeListRp).doubleValue());
        m.put("extraFees", feeListMoney(extraFeesSum, feeListRp).doubleValue());
        m.put("totalFee", totalFeeBd.doubleValue());
        m.put("feeVat", feeVatOut.doubleValue());
        m.put("expectedPayout", expectedPayoutBd.doubleValue());
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(balanceListCore(authentication, searchCompId, searchCompNm, page, size, true)));
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(balanceListCore(authentication, searchCompId, searchCompNm, page, size, false)));
    }

    @GetMapping("/unpaidMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> unpaidMng(
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> all = new ArrayList<>();
        for (SettlementRun r : settlementCalcService.listRuns(LocalDate.now().minusMonths(6), LocalDate.now())) {
            String compId = r.getMerchantId();
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            String compNm = ou != null ? ou.getName() : compId;
            if (searchCompId != null && !searchCompId.isBlank() && (compId == null || !compId.contains(searchCompId.trim()))) continue;
            if (searchCompNm != null && !searchCompNm.isBlank() && (compNm == null || !compNm.contains(searchCompNm.trim()))) continue;
            long settleAmt = r.getPayAmt() != null ? r.getPayAmt().longValue() : 0L;
            long unpaid = r.getTotalFee() != null ? r.getTotalFee().longValue() : 0L;
            Map<String, Object> m = new HashMap<>();
            m.put("compId", compId);
            m.put("compNm", compNm);
            m.put("settleAmt", settleAmt);
            m.put("deductCnt", unpaid);
            m.put("deductStatus", unpaid > 0 ? "차감" : "정상");
            all.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(pageOf(all, page, size)));
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                collateralLedgerService.search(searchFromDate, searchToDate, searchCompId, searchCompNm, searchStatus, page, size)));
    }

    @GetMapping("/holdList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> holdList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> all = new ArrayList<>();
        for (SettlementRun r : settlementCalcService.listRuns(searchFromDate, searchToDate)) {
            String compId = r.getMerchantId();
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            String compNm = ou != null ? ou.getName() : compId;
            if (searchCompNm != null && !searchCompNm.isBlank() && (compNm == null || !compNm.contains(searchCompNm.trim()))) continue;
            long holdAmount = r.getRollingReserveAmt() != null ? r.getRollingReserveAmt().longValue() : 0L;
            if (holdAmount <= 0) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("holdDt", r.getCalcDt() != null ? r.getCalcDt().toString() + " 00:00:00" : "");
            m.put("compId", compId);
            m.put("compNm", compNm);
            m.put("holdAmount", holdAmount);
            m.put("holdReason", "정산 보류(롤링)");
            all.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(pageOf(all, page, size)));
    }

    @GetMapping("/execute")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> executeList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<SettlementRun> list = settlementCalcService.listRuns(searchFromDate, searchToDate);
        int from = (page - 1) * size;
        int to = Math.min(from + size, list.size());
        List<Map<String, Object>> rows = list.subList(from, to).stream().map(this::toMap).collect(Collectors.toList());
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(list.size());
        pr.setTotalPages(size > 0 ? (int) Math.ceil((double) list.size() / size) : 1);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }

    @PostMapping("/execute/run")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> executeRun(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String merchantId) {
        /* 기간 지정 시: 기존 수동 실행(레거시 유지) */
        if (fromDate != null || toDate != null) {
            if (fromDate == null) fromDate = LocalDate.now().minusDays(1);
            if (toDate == null) toDate = LocalDate.now();
            List<SettlementRun> runs = settlementCalcService.execute(fromDate, toDate, merchantId);
            List<Map<String, Object>> list = runs.stream().map(this::toMap).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.ok(list));
        }

        /* 기간 미지정 시: calcCycle·AUTO·마감시간 등과 동일 규칙으로 자동 실행(스케줄 배치와 공유) */
        LocalDate today = LocalDate.now(SEOUL);
        List<SettlementRun> allRuns = settlementAutoRunService.runDueSettlements(today, merchantId, true);
        List<Map<String, Object>> list = allRuns.stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    private Map<String, Object> toMap(SettlementRun r) {
        Map<String, Object> m = new HashMap<>();
        m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : null);
        String mid = r.getMerchantId();
        m.put("compId", mid);
        OrgUnit ouExec = mid != null ? orgUnitRepository.findByCode(mid).orElse(null) : null;
        m.put("compNm", ouExec != null ? ouExec.getName() : (mid != null ? mid : "-"));
        m.put("targetAmt", r.getApproveAmt() != null && r.getCancelAmt() != null ? r.getApproveAmt().subtract(r.getCancelAmt()).toString() : "0");
        m.put("status", r.getStatus());
        m.put("payAmount", r.getPayAmt() != null ? r.getPayAmt().longValue() : 0);
        m.put("approveAmt", r.getApproveAmt() != null ? r.getApproveAmt().longValue() : 0);
        m.put("cancelAmt", r.getCancelAmt() != null ? r.getCancelAmt().longValue() : 0);
        m.put("totalFee", r.getTotalFee() != null ? r.getTotalFee().longValue() : 0);
        m.put("rollingReserveAmt", r.getRollingReserveAmt() != null ? r.getRollingReserveAmt().longValue() : 0);
        return m;
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

        void merge(SettlementRun r, Map<String, Object> dr) {
            settleAmt += asLongStatic(dr.get("settleAmt"));
            hqFee += asLongStatic(dr.get("hqFee"));
            regionalFee += asLongStatic(dr.get("regionalFee"));
            masterFee += asLongStatic(dr.get("masterFee"));
            branchFee += asLongStatic(dr.get("branchFee"));
            agencyFee += asLongStatic(dr.get("agencyFee"));
            runCnt++;
            BigDecimal ap = r.getApproveAmt() != null ? r.getApproveAmt() : BigDecimal.ZERO;
            BigDecimal ca = r.getCancelAmt() != null ? r.getCancelAmt() : BigDecimal.ZERO;
            approveAmtSum += ap.longValue();
            cancelAmtSum += ca.longValue();
            if (ca.signum() > 0) {
                cancelRunCnt++;
            }
            long feeSum = feeSumFromDr(dr);
            BigDecimal denom = ap.add(ca);
            if (denom.signum() == 0) {
                aprvFeeSum += feeSum;
            } else {
                aprvFeeSum += BigDecimal.valueOf(feeSum).multiply(ap).divide(denom, 0, RoundingMode.HALF_UP).longValue();
                canFeeSum += BigDecimal.valueOf(feeSum).multiply(ca).divide(denom, 0, RoundingMode.HALF_UP).longValue();
            }
        }

        private static long feeSumFromDr(Map<String, Object> dr) {
            return asLongStatic(dr.get("hqFee")) + asLongStatic(dr.get("regionalFee")) + asLongStatic(dr.get("masterFee"))
                    + asLongStatic(dr.get("branchFee")) + asLongStatic(dr.get("agencyFee"));
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
        return l == OrgLevel.REGIONAL || l == OrgLevel.MASTER_DIST || l == OrgLevel.BRANCH
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
            if (payHold && payAmt > 0) {
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
        all.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("compId", ""))));
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

    /**
     * 가맹정산내역: 결제내역과 동일한 수수료·보류·지급액 추정(승인 건은 비율+건당+정산건당+기타% 합산).
     * 승인 외 상태는 수수료내역(/feeList)과 동일한 건별 항목 합산을 오버레이합니다.
     */
    private Map<String, Object> toFranchiseRow(PgTrnsctn t, FeePolicy hqPolicy,
                                                Map<String, Long> monthCbCountCache,
                                                Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
                                                FeeListRoundingPolicy feeListRp) {
        String compId = t.getMerchantId();
        if (compId == null || compId.isBlank()) {
            return new LinkedHashMap<>();
        }
        PayListRowContext ctx = payListService.buildPayListRowContextForMerchant(compId.trim());
        Map<String, Object> row = new LinkedHashMap<>(PayListItemDto.from(t, ctx));
        BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        row.put("amount", amt);
        Object reg = row.get("compRegNo");
        row.put("bizNo", reg != null && !String.valueOf(reg).isBlank() ? String.valueOf(reg) : "-");
        String biz = "-";
        if (ctx != null && ctx.getProfile() != null) {
            MerchantProfile mp = ctx.getProfile();
            if (mp.getBizType() != null && !mp.getBizType().isBlank()) {
                biz = mp.getBizType().trim();
            } else if (mp.getIndustry() != null && !mp.getIndustry().isBlank()) {
                biz = mp.getIndustry().trim();
            }
        }
        row.put("bizType", biz);
        Object pgNo = row.get("pgApproveNo");
        row.put("payNo", pgNo != null ? pgNo : "-");
        if (!"10".equals(t.getStatus() != null ? t.getStatus().trim() : "")) {
            applyNonApproveFranchiseFeeOverlay(row, t, hqPolicy, monthCbCountCache, tiersByPolicyId,
                    ctx != null ? ctx.getSettlement() : null, feeListRp);
        }
        return row;
    }

    /** 수수료내역 API와 동일한 건별 추정치로 가맹정산 그리드 수수료·지급액 열을 채움 */
    private void applyNonApproveFranchiseFeeOverlay(Map<String, Object> row, PgTrnsctn t, FeePolicy hqPolicy,
                                                    Map<String, Long> monthCbCountCache,
                                                    Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
                                                    SettlementSetting merchantFeeVatSetting,
                                                    FeeListRoundingPolicy feeListRp) {
        String compId = t.getMerchantId();
        if (compId == null || compId.isBlank()) {
            return;
        }
        CommissionPolicy pol = resolveCommissionPolicyForMerchant(compId.trim());
        BigDecimal amountBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        FeeListTxnBreakdown br = computeFeeListTxnBreakdown(t, compId.trim(), pol, hqPolicy, monthCbCountCache, tiersByPolicyId, merchantFeeVatSetting, feeListRp);
        BigDecimal feeAmtBd = FeeListRoundingPolicy.round(BigDecimal.valueOf(br.totalFee()), feeListRp);
        BigDecimal feeVatBd = FeeListRoundingPolicy.round(br.feeVatBd(), feeListRp);
        row.put("feeCnt", br.totalFee() > 0 ? 1 : 0);
        row.put("feeAmt", feeAmtBd);
        row.put("feeVat", feeVatBd);
        row.put("holdRate", BigDecimal.ZERO);
        row.put("holdAmt", BigDecimal.ZERO);
        BigDecimal settleAmtBd = FeeListRoundingPolicy.round(amountBd.subtract(feeAmtBd).subtract(feeVatBd), feeListRp);
        row.put("settleAmt", settleAmtBd);
        if (amountBd.signum() > 0 && feeAmtBd.signum() > 0) {
            row.put("feeRate", feeAmtBd.multiply(BigDecimal.valueOf(100)).divide(amountBd, 4, RoundingMode.HALF_UP));
        } else {
            row.put("feeRate", BigDecimal.ZERO);
        }
        row.put("perTxFeeAmt", feeListMoney(br.perTxFee(), feeListRp));
        row.put("settlementPerTxFeeAmt", feeListMoney(br.settlementPerTxFee(), feeListRp));
        double ex = br.extraFee1() + br.extraFee2() + br.extraFee3() + br.extraFee4();
        row.put("extraFeesAmt", feeListMoney(ex, feeListRp));
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
            case "30", "31" -> "환불";
            case "F0", "99" -> "실패";
            default -> status;
        };
    }
}
