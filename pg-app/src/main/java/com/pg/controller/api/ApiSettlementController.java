package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.BalanceDeduction;
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
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.BalanceDeductionRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.AuthService;
import com.pg.service.CollateralLedgerService;
import com.pg.service.SettlementCalcService;
import com.pg.service.SettlementReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/settlement", produces = "application/json")
public class ApiSettlementController {

    private final SettlementCalcService settlementCalcService;
    private final OrgUnitRepository orgUnitRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final BalanceDeductionRepository balanceDeductionRepository;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final AuthService authService;
    private final SettlementReportService settlementReportService;
    private final CollateralLedgerService collateralLedgerService;

    public ApiSettlementController(SettlementCalcService settlementCalcService,
                                   OrgUnitRepository orgUnitRepository,
                                   DistributionFeeConfigRepository distributionFeeConfigRepository,
                                   PgTrnsctnRepository pgTrnsctnRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   MerchantPgBindingRepository merchantPgBindingRepository,
                                   CommissionPolicyRepository commissionPolicyRepository,
                                   SettlementSettingRepository settlementSettingRepository,
                                   BalanceDeductionRepository balanceDeductionRepository,
                                   HqApiConfigRepository hqApiConfigRepository,
                                   AuthService authService,
                                   SettlementReportService settlementReportService,
                                   CollateralLedgerService collateralLedgerService) {
        this.settlementCalcService = settlementCalcService;
        this.orgUnitRepository = orgUnitRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.balanceDeductionRepository = balanceDeductionRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.authService = authService;
        this.settlementReportService = settlementReportService;
        this.collateralLedgerService = collateralLedgerService;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);
        List<PgTrnsctn> txList = pgTrnsctnRepository.findForSettlement(null, fromDt, toDt);
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (PgTrnsctn t : txList) {
            Map<String, Object> row = toFranchiseRow(t);
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> all = new ArrayList<>();
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);
        FeePolicy hqPolicy = resolveHqFeePolicy();
        for (PgTrnsctn t : pgTrnsctnRepository.findForSettlement(null, fromDt, toDt)) {
            String s = t.getStatus() != null ? t.getStatus().trim() : "";
            boolean recallTarget = "20".equals(s) || "30".equals(s) || "31".equals(s);
            if (!recallTarget) continue;
            String compId = t.getMerchantId();
            if (searchCompId != null && !searchCompId.isBlank() && (compId == null || !compId.contains(searchCompId.trim()))) {
                continue;
            }
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            if (ou == null) continue;
            long txnAmt = t.getAmtKrw() != null ? t.getAmtKrw().longValue() : 0L;
            long feeAmt = estimateFeeAmount(txnAmt, compId);
            long feeVat = hqPolicy.settlementVatApplyYn ? Math.round(feeAmt * 0.1d) : 0L;
            long recallAmt = hqPolicy.recallIncludeFeeYn ? Math.max(0L, txnAmt + feeAmt + feeVat) : Math.max(0L, txnAmt);
            long deductAmt = -recallAmt;
            Map<String, Object> m = new HashMap<>();
            m.put("calcDt", t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate().toString() : "");
            m.put("compId", compId);
            m.put("compNm", ou != null ? ou.getName() : compId);
            m.put("settleAmt", txnAmt);
            m.put("recallAmt", recallAmt);
            m.put("deductAmt", deductAmt);
            m.put("status", s);
            m.put("statusNm", "20".equals(s) ? "당일무효" : ("30".equals(s) ? "환불" : "강제환불"));
            m.put("feeIncludedYn", hqPolicy.recallIncludeFeeYn ? "Y" : "N");
            m.put("vatAppliedYn", hqPolicy.settlementVatApplyYn ? "Y" : "N");
            all.add(m);
        }
        all.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("calcDt", ""))).reversed());
        return ResponseEntity.ok(ApiResponse.ok(pageOf(all, page, size)));
    }

    /** 수수료내역: 가맹점 거래 1건마다 본사 기본정책의 모든 수수료 항목 계산 표시 */
    @GetMapping("/feeList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> feeList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDate fromDate = searchFromDate != null ? searchFromDate : LocalDate.now().minusMonths(1);
        LocalDate toDate = searchToDate != null ? searchToDate : LocalDate.now();
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);
        FeePolicy hqPolicy = resolveHqFeePolicy();
        List<Map<String, Object>> all = new ArrayList<>();
        for (PgTrnsctn t : pgTrnsctnRepository.findForSettlement(null, fromDt, toDt)) {
            String compId = t.getMerchantId();
            if (searchCompId != null && !searchCompId.isBlank() && (compId == null || !compId.contains(searchCompId.trim()))) {
                continue;
            }
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            if (ou == null) continue;
            String compNm = ou.getName() != null ? ou.getName() : compId;
            if (searchCompNm != null && !searchCompNm.isBlank() && !compNm.contains(searchCompNm.trim())) {
                continue;
            }
            long amount = t.getAmtKrw() != null ? t.getAmtKrw().longValue() : 0L;
            BigDecimal amountBd = BigDecimal.valueOf(amount);
            BigDecimal policyRate = resolvePayRate(compId);
            long perTxFee = nz(hqPolicy.perTxFee).longValue();
            long usageFee = amountBd.multiply(nz(hqPolicy.usageRate)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue();
            long failFee = "F0".equals(t.getStatus()) || "99".equals(t.getStatus()) ? nz(hqPolicy.failFee).longValue() : 0L;
            long cancelFee = "20".equals(t.getStatus()) ? amountBd.multiply(nz(hqPolicy.cancelRate)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue() : 0L;
            long refundFee = ("30".equals(t.getStatus()) || "31".equals(t.getStatus())) ? amountBd.multiply(nz(hqPolicy.refundRate)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue() : 0L;
            long payFee = "10".equals(t.getStatus()) ? amountBd.multiply(policyRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue() : 0L;
            long settlementPerTx = nz(hqPolicy.feeSettlementPerTx).longValue();
            long usdtFee = nz(hqPolicy.feeUsdt).longValue();
            long fxFee = nz(hqPolicy.feeFx).longValue();
            long totalFee = Math.max(0L, perTxFee + usageFee + failFee + cancelFee + refundFee + payFee + settlementPerTx + usdtFee + fxFee);
            long feeVat = hqPolicy.settlementVatApplyYn ? Math.round(totalFee * 0.1d) : 0L;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("compId", compId);
            m.put("compNm", compNm);
            m.put("trnId", t.getTrnId());
            m.put("status", t.getStatus());
            m.put("statusNm", payDivName(t.getStatus()));
            m.put("trnDate", t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate().toString() : "");
            m.put("amount", amount);
            m.put("perTxFee", perTxFee);
            m.put("usageFee", usageFee);
            m.put("failFee", failFee);
            m.put("cancelFee", cancelFee);
            m.put("refundFee", refundFee);
            m.put("payFeeRate", policyRate);
            m.put("payFee", payFee);
            m.put("settlementPerTxFee", settlementPerTx);
            m.put("usdtFee", usdtFee);
            m.put("fxFee", fxFee);
            m.put("totalFee", totalFee);
            m.put("feeVat", feeVat);
            m.put("vatAppliedYn", hqPolicy.settlementVatApplyYn ? "Y" : "N");
            all.add(m);
        }
        all.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("trnDate", ""))).reversed());
        return ResponseEntity.ok(ApiResponse.ok(pageOf(all, page, size)));
    }

    @GetMapping("/balanceMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> balanceMng(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(balanceListCore(searchCompId, searchCompNm, page, size, true)));
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
            @RequestParam(required = false) String searchCompId,
            @RequestParam(required = false) String searchCompNm,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(balanceListCore(searchCompId, searchCompNm, page, size, false)));
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
        if (fromDate == null) fromDate = LocalDate.now().minusDays(1);
        if (toDate == null) toDate = LocalDate.now();
        List<SettlementRun> runs = settlementCalcService.execute(fromDate, toDate, merchantId);
        List<Map<String, Object>> list = runs.stream().map(this::toMap).collect(Collectors.toList());
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

    private PageResult<Map<String, Object>> balanceListCore(String searchCompId, String searchCompNm, int page, int size, boolean combined) {
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

    private Map<String, Object> toFranchiseRow(PgTrnsctn t) {
        Map<String, Object> m = new HashMap<>();
        String compId = t.getMerchantId();
        OrgUnit merchant = orgUnitRepository.findByCode(compId).orElse(null);
        String merchantNm = merchant != null ? merchant.getName() : compId;
        MerchantProfile mp = merchant != null ? merchantProfileRepository.findByOrgUnitId(merchant.getId()).orElse(null) : null;
        MerchantPgBinding binding = merchant != null
                ? merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(merchant.getId()).stream().findFirst().orElse(null)
                : null;

        BigDecimal amount = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        DistributionFeeConfig cfg = distributionFeeConfigRepository.findByCompId(compId).orElse(null);
        BigDecimal allRate = BigDecimal.ZERO;
        if (cfg != null) {
            allRate = nz(cfg.getHqRate()).add(nz(cfg.getRegionalRate())).add(nz(cfg.getMasterRate())).add(nz(cfg.getBranchRate())).add(nz(cfg.getAgencyRate()));
        }
        BigDecimal feeAmt = amount.multiply(allRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal vatAmt = feeAmt.multiply(BigDecimal.valueOf(0.1)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal holdRate = BigDecimal.ZERO;
        BigDecimal holdAmt = amount.multiply(holdRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal settleAmt = amount.subtract(feeAmt).subtract(vatAmt).subtract(holdAmt);

        m.put("merchantNm", merchantNm);
        m.put("compNm", merchantNm);
        m.put("compId", compId);
        m.put("bizType", regType(mp != null ? mp.getRegNo() : null));
        m.put("bizNo", regNo(mp != null ? mp.getRegNo() : null));
        m.put("payDivNm", payDivLabel(t.getStatus()));
        m.put("payCard", "-");
        m.put("cardAprvNo", blank(t.getApprovalNo()));
        m.put("payCardNo", "-");
        m.put("instalMonth", "0");
        m.put("payMethod", binding != null && binding.getPayMethod() != null ? binding.getPayMethod() : "CARD");
        m.put("corpNm", merchantNm);
        m.put("pgNm", binding != null && binding.getPgCd() != null ? binding.getPgCd() : blank(t.getVan()));
        m.put("terminalId", binding != null ? blank(binding.getMid()) : "-");
        m.put("amount", amount.longValue());
        m.put("payNo", blank(t.getPayNo()));
        m.put("feeCnt", 1);
        m.put("feeRate", allRate);
        m.put("feeAmt", feeAmt.longValue());
        m.put("feeVat", vatAmt.longValue());
        m.put("holdRate", holdRate);
        m.put("holdAmt", holdAmt.longValue());
        m.put("calcCycle", "-");
        m.put("settleAmt", settleAmt.longValue());
        m.put("calcDt", t.getCreatedAt() != null ? t.getCreatedAt().toString().replace("T", " ") : "");
        m.put("approveDt", t.getCreatedAt() != null ? t.getCreatedAt().toString().replace("T", " ") : "");
        m.put("cancelDt", "20".equals(t.getStatus()) ? (t.getCreatedAt() != null ? t.getCreatedAt().toString().replace("T", " ") : "") : "");
        m.put("payStatus", "20".equals(t.getStatus()) ? "취소" : "정산대기");
        m.put("productNm", mp != null ? blank(mp.getProduct()) : "-");
        m.put("customerNm", payerCustomerName(t));
        m.put("customerTel", "-");

        String regional = "", master = "", branch = "";
        OrgUnit cur = merchant;
        for (int i = 0; i < 8 && cur != null; i++) {
            if (cur.getOrgLevel() != null) {
                switch (cur.getOrgLevel()) {
                    case MASTER_DIST -> regional = cur.getName();
                    case BRANCH -> master = cur.getName();
                    case AGENCY, SALES_OFFICE -> branch = cur.getName();
                    default -> {}
                }
            }
            cur = cur.getParentId() != null ? orgUnitRepository.findById(cur.getParentId()).orElse(null) : null;
        }
        m.put("regionalNm", regional);
        m.put("masterNm", master);
        m.put("branchNm", branch);
        return m;
    }

    private BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private String blank(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private record FeePolicy(
            BigDecimal perTxFee, BigDecimal usageRate, BigDecimal failFee,
            BigDecimal cancelRate, BigDecimal refundRate, BigDecimal payRate,
            BigDecimal feeSettlementPerTx, BigDecimal feeUsdt, BigDecimal feeFx,
            boolean recallIncludeFeeYn, boolean settlementVatApplyYn
    ) {}

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

    private BigDecimal resolvePayRate(String merchantId) {
        CommissionPolicy merchant = merchantId != null ? commissionPolicyRepository.findByScope(merchantId).orElse(null) : null;
        BigDecimal base = merchant != null && merchant.getPayRate() != null
                ? merchant.getPayRate()
                : commissionPolicyRepository.findByScope("DEFAULT").map(CommissionPolicy::getPayRate).orElse(BigDecimal.ZERO);
        OrgUnit ou = merchantId != null ? orgUnitRepository.findByCode(merchantId).orElse(null) : null;
        if (ou == null) return nz(base);
        SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(ou.getId()).orElse(null);
        if (ss != null && "N".equalsIgnoreCase(ss.getHoldRateFollowHq() != null ? ss.getHoldRateFollowHq().trim() : "")
                && ss.getHoldRate() != null) {
            return nz(base);
        }
        return nz(base);
    }

    private long estimateFeeAmount(long amount, String merchantId) {
        BigDecimal amt = BigDecimal.valueOf(Math.max(0L, amount));
        BigDecimal rate = resolvePayRate(merchantId);
        return amt.multiply(rate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue();
    }

    private String payDivName(String status) {
        return payDivLabel(status);
    }

    /** 가맹점 정산 행의 고객명 = 거래의 결제 고객(가맹 대표자와 구분) */
    private String payerCustomerName(PgTrnsctn t) {
        if (t.getCustomerNm() != null && !t.getCustomerNm().isBlank()) {
            return t.getCustomerNm().trim();
        }
        if (t.getCustomerId() != null && !t.getCustomerId().isBlank()) {
            return t.getCustomerId().trim();
        }
        return "-";
    }
    private String regType(String regNo) {
        if (regNo == null || !regNo.contains("|")) return "-";
        String t = regNo.split("\\|", 2)[0];
        return "PERSONAL".equalsIgnoreCase(t) ? "개인" : "법인";
    }
    private String regNo(String regNo) {
        if (regNo == null) return "-";
        return regNo.contains("|") ? regNo.split("\\|", 2)[1] : regNo;
    }
    private String payDivLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "10" -> "결제";
            case "20" -> "취소";
            case "30", "31" -> "환불";
            case "F0", "99" -> "실패";
            default -> status;
        };
    }
}
