package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRun;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.SettlementCalcService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public ApiSettlementController(SettlementCalcService settlementCalcService,
                                   OrgUnitRepository orgUnitRepository,
                                   DistributionFeeConfigRepository distributionFeeConfigRepository,
                                   PgTrnsctnRepository pgTrnsctnRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   MerchantPgBindingRepository merchantPgBindingRepository) {
        this.settlementCalcService = settlementCalcService;
        this.orgUnitRepository = orgUnitRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
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

    @GetMapping("/distributionList")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> distributionList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(required = false) String searchView,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<SettlementRun> list = settlementCalcService.listRuns(searchFromDate, searchToDate);
        List<Map<String, Object>> allRows = new ArrayList<>();
        for (SettlementRun r : list) allRows.add(toDistributionRow(r));
        if (searchView != null && !searchView.isBlank() && !"DETAIL".equalsIgnoreCase(searchView)) {
            allRows = aggregateRows(allRows, searchView);
        }
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
                String compNm = String.valueOf(row.getOrDefault("merchantNm", ""));
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
        for (SettlementRun r : settlementCalcService.listRuns(searchFromDate, searchToDate)) {
            String compId = r.getMerchantId();
            if (searchCompId != null && !searchCompId.isBlank() && (compId == null || !compId.contains(searchCompId.trim()))) continue;
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            long settleAmt = r.getPayAmt() != null ? r.getPayAmt().longValue() : 0L;
            long recallAmt = r.getTotalFee() != null ? r.getTotalFee().longValue() : 0L;
            long deductAmt = Math.min(recallAmt, Math.max(0L, settleAmt / 10));
            Map<String, Object> m = new HashMap<>();
            m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : "");
            m.put("compId", compId);
            m.put("compNm", ou != null ? ou.getName() : compId);
            m.put("settleAmt", settleAmt);
            m.put("recallAmt", recallAmt);
            m.put("deductAmt", deductAmt);
            all.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(pageOf(all, page, size)));
    }

    @GetMapping("/balanceMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> balanceMng(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchToDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(balanceListCore(null, null, page, size, true)));
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
        List<Map<String, Object>> rows = list.subList(from, to).stream().map(ApiSettlementController::toMap).collect(Collectors.toList());
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
        List<Map<String, Object>> list = runs.stream().map(ApiSettlementController::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    private static Map<String, Object> toMap(SettlementRun r) {
        Map<String, Object> m = new HashMap<>();
        m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : null);
        m.put("compId", r.getMerchantId());
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

    private List<Map<String, Object>> aggregateRows(List<Map<String, Object>> rows, String searchView) {
        String nameKey;
        String feeKey;
        switch (searchView.toUpperCase()) {
            case "HQ" -> { nameKey = "hqNm"; feeKey = "hqFee"; }
            case "REGIONAL" -> { nameKey = "regionalNm"; feeKey = "regionalFee"; }
            case "MASTER" -> { nameKey = "masterNm"; feeKey = "masterFee"; }
            case "BRANCH" -> { nameKey = "branchNm"; feeKey = "branchFee"; }
            case "AGENCY" -> { nameKey = "agencyNm"; feeKey = "agencyFee"; }
            default -> { return rows; }
        }
        Map<String, Map<String, Object>> agg = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String name = String.valueOf(row.getOrDefault(nameKey, ""));
            if (name.isBlank()) continue;
            Map<String, Object> one = agg.computeIfAbsent(name, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("calcDt", row.getOrDefault("calcDt", ""));
                m.put("compId", k);
                m.put("compNm", k);
                m.put("settleAmt", 0L);
                m.put("hqFee", 0L); m.put("regionalFee", 0L); m.put("masterFee", 0L); m.put("branchFee", 0L); m.put("agencyFee", 0L);
                return m;
            });
            one.put("settleAmt", ((Number) one.get("settleAmt")).longValue() + asLong(row.get("settleAmt")));
            one.put(feeKey, ((Number) one.get(feeKey)).longValue() + asLong(row.get(feeKey)));
        }
        return new ArrayList<>(agg.values());
    }

    private long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }

    private PageResult<Map<String, Object>> balanceListCore(String searchCompId, String searchCompNm, int page, int size, boolean combined) {
        List<Map<String, Object>> all = new ArrayList<>();
        List<SettlementRun> runs = settlementCalcService.listRuns(LocalDate.now().minusMonths(6), LocalDate.now());
        for (SettlementRun r : runs) {
            String compId = r.getMerchantId();
            OrgUnit ou = orgUnitRepository.findByCode(compId).orElse(null);
            String compNm = ou != null ? ou.getName() : compId;
            if (searchCompId != null && !searchCompId.isBlank() && (compId == null || !compId.contains(searchCompId.trim()))) continue;
            if (searchCompNm != null && !searchCompNm.isBlank() && (compNm == null || !compNm.contains(searchCompNm.trim()))) continue;
            long bal = r.getPayAmt() != null ? r.getPayAmt().longValue() : 0L;
            long unpaid = r.getTotalFee() != null ? r.getTotalFee().longValue() : 0L;
            Map<String, Object> m = new HashMap<>();
            m.put("compId", compId);
            m.put("compNm", compNm);
            if (combined) {
                m.put("balcAmount", bal);
                m.put("unpaidAmount", unpaid);
            } else {
                m.put("condition", "ETC");
                m.put("chargeType", "기타");
                m.put("payMethod", "계좌");
                m.put("chargeNm", compNm);
                m.put("chargeAmt", bal);
                m.put("sumChargeAmt", bal);
            }
            all.add(m);
        }
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
