package com.pg.service.ops;

import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementRun;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import com.pg.service.ExcelStyledExportService;
import com.pg.service.OrgAccessService;
import com.pg.service.SettlementCalcService;
import com.pg.service.SettlementReportService;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 태국 정부 신고용 TAX 리포트(운영관리).
 * 원천: 확정(CALCULATED)·정산배포·가맹 정산내역 노출 규칙을 통과한 {@link SettlementRun}.
 * 접근: ADMIN 또는 조직 등급이 총본사·본사(REGIONAL)·총판(MASTER_DIST) — 하위 가맹 범위만({@link OrgAccessService}).
 */
@Service
public class TaxReportService {

    private final AuthService authService;
    private final OrgUnitRepository orgUnitRepository;
    private final OrgAccessService orgAccessService;
    private final SettlementCalcService settlementCalcService;
    private final SettlementReportService settlementReportService;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final ExcelStyledExportService excelStyledExportService;

    public TaxReportService(AuthService authService,
                           OrgUnitRepository orgUnitRepository,
                           OrgAccessService orgAccessService,
                           SettlementCalcService settlementCalcService,
                           SettlementReportService settlementReportService,
                           HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                           ExcelStyledExportService excelStyledExportService) {
        this.authService = authService;
        this.orgUnitRepository = orgUnitRepository;
        this.orgAccessService = orgAccessService;
        this.settlementCalcService = settlementCalcService;
        this.settlementReportService = settlementReportService;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.excelStyledExportService = excelStyledExportService;
    }

    public FeeListRoundingPolicy ledgerRoundingPolicy() {
        return hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(s -> FeeCurrencyRoundResolver.from(s).forCurrency(PayDisplayCurrency.alphaFromSettings(s)))
                .orElseGet(FeeListRoundingPolicy::defaults);
    }

    public Optional<String> accessDeniedReason(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return Optional.of("로그인이 필요합니다.");
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return Optional.empty();
        }
        OrgUnit ou = authService.resolveOrgUnitForLoginId(user.getUsername()).orElse(null);
        if (ou == null || ou.getOrgLevel() == null) {
            return Optional.of("조직 정보가 없어 TAX 리포트를 열 수 없습니다.");
        }
        OrgLevel l = ou.getOrgLevel();
        if (l != OrgLevel.HEADQUARTERS && l != OrgLevel.REGIONAL && l != OrgLevel.MASTER_DIST) {
            return Optional.of("총본사·본사·총판 로그인만 이용할 수 있습니다.");
        }
        return Optional.empty();
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        Map<String, Object> m = new LinkedHashMap<>();
        Optional<String> deny = accessDeniedReason(authentication);
        m.put("allowed", deny.isEmpty());
        deny.ifPresent(s -> m.put("reason", s));
        if (!(authentication != null && authentication.getPrincipal() instanceof AppUser u)) {
            return m;
        }
        m.put("isAdmin", "ADMIN".equalsIgnoreCase(u.getRole()));
        authService.resolveOrgUnitForLoginId(u.getUsername()).ifPresent(ou -> {
            m.put("viewerOrgCode", ou.getCode());
            m.put("viewerOrgLevel", ou.getOrgLevel() != null ? ou.getOrgLevel().name() : null);
        });
        return m;
    }

    private boolean merchantAllowed(Set<String> visibleMerchants, String merchantCode) {
        if (merchantCode == null || merchantCode.isBlank()) {
            return false;
        }
        String mid = merchantCode.trim();
        if (visibleMerchants == null) {
            return true;
        }
        return visibleMerchants.contains(mid);
    }

    private boolean eligibleSettlementRow(SettlementRun r, Set<String> visibleMerchants) {
        if (r == null || !"CALCULATED".equalsIgnoreCase(String.valueOf(r.getStatus()))) {
            return false;
        }
        if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
            return false;
        }
        if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
            return false;
        }
        if (!settlementCalcService.isMerchantStatementVisibleSettlementRun(r)) {
            return false;
        }
        return merchantAllowed(visibleMerchants, r.getMerchantId());
    }

    private static BigDecimal nz(BigDecimal x) {
        return x != null ? x : BigDecimal.ZERO;
    }

    private double roundMoney(BigDecimal bd) {
        return FeeListRoundingPolicy.round(bd, ledgerRoundingPolicy()).doubleValue();
    }

    private BigDecimal finalBankAlignedBd(SettlementRun r) {
        Map<String, Object> rm = settlementReportService.remittanceFieldsForRun(r);
        Object v = rm.get("finalPayAfterRemittance");
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return FeeListRoundingPolicy.round(nz(r.getPayAmt()), ledgerRoundingPolicy());
    }

    /** 필터·정렬까지 완료한 전체 행 + 합계 메타 */
    public Built buildAll(Authentication authentication,
                          LocalDate fromInclusive,
                          LocalDate toInclusive,
                          String searchCompId,
                          String searchOrderDir) {
        Built b = new Built();
        b.rows = new ArrayList<>();
        b.meta = new LinkedHashMap<>();

        Optional<String> deny = accessDeniedReason(authentication);
        if (deny.isPresent()) {
            b.meta.put("error", deny.get());
            return b;
        }

        Set<String> vis = orgAccessService.visibleMerchantCompCodes(authentication);
        LocalDate from = fromInclusive != null ? fromInclusive : LocalDate.now().minusMonths(1);
        LocalDate to = toInclusive != null ? toInclusive : LocalDate.now();

        List<SettlementRun> runs = settlementCalcService.listRuns(from, to);
        BigDecimal sumApprove = BigDecimal.ZERO;
        BigDecimal sumCancel = BigDecimal.ZERO;
        BigDecimal sumTotalFee = BigDecimal.ZERO;
        BigDecimal sumPay = BigDecimal.ZERO;
        BigDecimal sumFinalBank = BigDecimal.ZERO;

        for (SettlementRun r : runs) {
            if (!eligibleSettlementRow(r, vis)) {
                continue;
            }
            String mid = r.getMerchantId().trim();
            if (searchCompId != null && !searchCompId.isBlank() && !mid.contains(searchCompId.trim())) {
                continue;
            }
            OrgUnit merchantOu = orgUnitRepository.findByCode(mid).orElse(null);
            if (merchantOu == null) {
                merchantOu = orgUnitRepository.findByCodeIgnoreCase(mid).orElse(null);
            }

            BigDecimal ap = nz(r.getApproveAmt());
            BigDecimal ca = nz(r.getCancelAmt());
            BigDecimal netSales = ap.subtract(ca);
            BigDecimal payBd = nz(r.getPayAmt());
            BigDecimal finBd = finalBankAlignedBd(r);

            sumApprove = sumApprove.add(ap);
            sumCancel = sumCancel.add(ca);
            sumTotalFee = sumTotalFee.add(nz(r.getTotalFee()));
            sumPay = sumPay.add(payBd);
            sumFinalBank = sumFinalBank.add(finBd);

            Map<String, Object> rem = settlementReportService.remittanceFieldsForRun(r);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("settlementRunId", r.getId());
            m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : "");
            m.put("periodFrom", r.getPeriodFrom() != null ? r.getPeriodFrom().toString() : "");
            m.put("periodTo", r.getPeriodTo() != null ? r.getPeriodTo().toString() : "");
            m.put("compId", mid);
            m.put("compNm", merchantOu != null && merchantOu.getName() != null ? merchantOu.getName() : mid);
            m.put("txnCnt", r.getIncludedTxnCnt());
            m.put("approveAmt", roundMoney(ap));
            m.put("cancelAmt", roundMoney(ca));
            m.put("netSales", roundMoney(netSales));
            m.put("totalFee", roundMoney(nz(r.getTotalFee())));
            m.put("rollingReserveAmt", roundMoney(nz(r.getRollingReserveAmt())));
            m.put("settlementBatchFee", r.getSettlementBatchFeeAmt() != null ? roundMoney(r.getSettlementBatchFeeAmt()) : null);
            m.put("payAmount", roundMoney(payBd));
            m.put("remittanceFeeBank", rem.get("remittanceFeeBank"));
            m.put("remittanceFeeUsdt", rem.get("remittanceFeeUsdt"));
            m.put("finalPayAfterRemittance", rem.get("finalPayAfterRemittance"));
            m.put("reportNote", "Final pay column ≈ bank transfer basis for TH tax filing");
            b.rows.add(m);
        }

        boolean asc = searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim());
        Comparator<Map<String, Object>> primary = Comparator.comparing(row -> String.valueOf(row.getOrDefault("calcDt", "")));
        b.rows.sort(asc ? primary : primary.reversed());

        b.meta.put("scopeHint", "CALCULATED settlement runs under your org tree only.");
        b.meta.put("totalApproveAmt", roundMoney(sumApprove));
        b.meta.put("totalCancelAmt", roundMoney(sumCancel));
        b.meta.put("totalNetSales", roundMoney(sumApprove.subtract(sumCancel)));
        b.meta.put("totalFeeSum", roundMoney(sumTotalFee));
        b.meta.put("totalPayAmount", roundMoney(sumPay));
        b.meta.put("totalFinalPayAfterRemittance", roundMoney(sumFinalBank));
        b.meta.put("merchantMonthlyRollup", buildMerchantRollup(b.rows));
        return b;
    }

    public PageResult<Map<String, Object>> listRuns(Authentication authentication,
                                                    LocalDate fromInclusive,
                                                    LocalDate toInclusive,
                                                    String searchCompId,
                                                    String searchOrderDir,
                                                    int page,
                                                    int size) {
        PageResult<Map<String, Object>> out = new PageResult<>();
        Built b = buildAll(authentication, fromInclusive, toInclusive, searchCompId, searchOrderDir);
        if (b.meta.containsKey("error")) {
            out.setList(List.of());
            out.setPage(Math.max(1, page));
            out.setSize(Math.max(1, size));
            out.setTotalElements(0);
            out.setTotalPages(1);
            out.setMeta(b.meta);
            return out;
        }
        List<Map<String, Object>> all = b.rows;
        int total = all.size();
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        int fromIx = Math.min(Math.max(0, (p - 1) * s), total);
        int toIx = Math.min(total, fromIx + s);
        out.setList(fromIx < total ? all.subList(fromIx, toIx) : List.of());
        out.setPage(p);
        out.setSize(s);
        out.setTotalElements(total);
        out.setTotalPages(Math.max(1, (int) Math.ceil(total / (double) s)));
        out.setMeta(b.meta);
        return out;
    }

    private List<Map<String, Object>> buildMerchantRollup(List<Map<String, Object>> detailRows) {
        Map<String, Agg> byMid = new LinkedHashMap<>();
        for (Map<String, Object> row : detailRows) {
            String mid = String.valueOf(row.getOrDefault("compId", "")).trim();
            if (mid.isEmpty()) {
                continue;
            }
            Agg g = byMid.computeIfAbsent(mid, k -> new Agg());
            if (g.compNm.isEmpty()) {
                g.compNm = String.valueOf(row.getOrDefault("compNm", mid));
            }
            g.runCnt++;
            g.finalPaySum += asDouble(row.get("finalPayAfterRemittance"));
            g.payAmtSum += asDouble(row.get("payAmount"));
            g.approveSum += asDouble(row.get("approveAmt"));
            g.cancelSum += asDouble(row.get("cancelAmt"));
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Agg> e : byMid.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("compId", e.getKey());
            m.put("compNm", e.getValue().compNm);
            m.put("runCount", e.getValue().runCnt);
            m.put("sumApproveAmt", round8(e.getValue().approveSum));
            m.put("sumCancelAmt", round8(e.getValue().cancelSum));
            m.put("sumPayAmount", round8(e.getValue().payAmtSum));
            m.put("sumFinalPayAfterRemittance", round8(e.getValue().finalPaySum));
            list.add(m);
        }
        list.sort(Comparator.comparing(x -> String.valueOf(x.getOrDefault("compId", ""))));
        return list;
    }

    private static double asDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(o).replace(",", "").trim());
        } catch (Exception e) {
            return 0d;
        }
    }

    private static double round8(double x) {
        return BigDecimal.valueOf(x).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().doubleValue();
    }

    private static final class Agg {
        String compNm = "";
        int runCnt;
        double approveSum;
        double cancelSum;
        double payAmtSum;
        double finalPaySum;
    }

    public static final class Built {
        List<Map<String, Object>> rows = List.of();
        Map<String, Object> meta = Map.of();
    }

    public Map<String, LocalDate> resolveMonthBounds(String yearMonth) {
        YearMonth ym;
        if (yearMonth == null || yearMonth.isBlank()) {
            ym = YearMonth.now().minusMonths(1);
        } else {
            ym = YearMonth.parse(yearMonth.trim());
        }
        return Map.of("from", ym.atDay(1), "to", ym.atEndOfMonth());
    }

    public List<String> excelHeaders() {
        return List.of(
                "SettlementRunId",
                "SettlementDate",
                "PeriodFrom",
                "PeriodTo",
                "MerchantCode",
                "MerchantName",
                "TxnCount",
                "ApproveAmt",
                "CancelAmt",
                "NetSales",
                "TotalFee",
                "RollingReserve",
                "SettlementBatchFee",
                "PayAmount",
                "RemittanceFeeBank",
                "RemittanceFeeUsdt",
                "FinalPayAfterRemittance",
                "Note"
        );
    }

    public List<List<String>> buildExcelBodyRows(List<Map<String, Object>> detailRows,
                                                 List<Map<String, Object>> merchantRollup,
                                                 Map<String, Object> totalsMeta) {
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> r : detailRows) {
            rows.add(List.of(
                    String.valueOf(r.getOrDefault("settlementRunId", "")),
                    String.valueOf(r.getOrDefault("calcDt", "")),
                    String.valueOf(r.getOrDefault("periodFrom", "")),
                    String.valueOf(r.getOrDefault("periodTo", "")),
                    String.valueOf(r.getOrDefault("compId", "")),
                    String.valueOf(r.getOrDefault("compNm", "")),
                    String.valueOf(r.getOrDefault("txnCnt", "")),
                    numStr(r.get("approveAmt")),
                    numStr(r.get("cancelAmt")),
                    numStr(r.get("netSales")),
                    numStr(r.get("totalFee")),
                    numStr(r.get("rollingReserveAmt")),
                    numStr(r.get("settlementBatchFee")),
                    numStr(r.get("payAmount")),
                    numStr(r.get("remittanceFeeBank")),
                    numStr(r.get("remittanceFeeUsdt")),
                    numStr(r.get("finalPayAfterRemittance")),
                    String.valueOf(r.getOrDefault("reportNote", ""))
            ));
        }
        rows.add(List.of("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""));
        rows.add(List.of(
                "TOTAL",
                "",
                "",
                "",
                "",
                "",
                "",
                numStr(totalsMeta != null ? totalsMeta.get("totalApproveAmt") : null),
                numStr(totalsMeta != null ? totalsMeta.get("totalCancelAmt") : null),
                numStr(totalsMeta != null ? totalsMeta.get("totalNetSales") : null),
                numStr(totalsMeta != null ? totalsMeta.get("totalFeeSum") : null),
                "",
                "",
                numStr(totalsMeta != null ? totalsMeta.get("totalPayAmount") : null),
                "",
                "",
                numStr(totalsMeta != null ? totalsMeta.get("totalFinalPayAfterRemittance") : null),
                "Sum FinalPay ≈ bank remittance"
        ));

        rows.add(List.of("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""));
        rows.add(List.of(
                "MERCHANT_ROLLUP",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        ));
        rows.add(List.of(
                "MerchantCode",
                "MerchantName",
                "RunCount",
                "SumApprove",
                "SumCancel",
                "SumPayAmount",
                "SumFinalPayAfterRemittance",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        ));
        if (merchantRollup != null) {
            for (Map<String, Object> r : merchantRollup) {
                rows.add(List.of(
                        String.valueOf(r.getOrDefault("compId", "")),
                        String.valueOf(r.getOrDefault("compNm", "")),
                        String.valueOf(r.getOrDefault("runCount", "")),
                        numStr(r.get("sumApproveAmt")),
                        numStr(r.get("sumCancelAmt")),
                        numStr(r.get("sumPayAmount")),
                        numStr(r.get("sumFinalPayAfterRemittance")),
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        ""
                ));
            }
        }
        return rows;
    }

    private static String numStr(Object o) {
        if (o == null || "-".equals(String.valueOf(o)) || "—".equals(String.valueOf(o))) {
            return "";
        }
        return String.valueOf(o);
    }

    public Set<Integer> excelTextColumnIndexes() {
        return Set.of(4, 5);
    }

    public byte[] exportStyledXlsx(String sheetName,
                                   Authentication authentication,
                                   LocalDate fromInclusive,
                                   LocalDate toInclusive,
                                   String searchCompId,
                                   String searchOrderDir) throws IOException {
        Built b = buildAll(authentication, fromInclusive, toInclusive, searchCompId, searchOrderDir);
        if (b.meta.containsKey("error")) {
            throw new IllegalStateException(String.valueOf(b.meta.get("error")));
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rollup = (List<Map<String, Object>>) b.meta.getOrDefault("merchantMonthlyRollup", List.of());
        List<List<String>> lines = buildExcelBodyRows(b.rows, rollup, b.meta);
        String safeName = sheetName != null && !sheetName.isBlank() ? sheetName : "TAX_Report";
        return excelStyledExportService.buildStyledTable(safeName, excelHeaders(), lines, new HashSet<>(excelTextColumnIndexes()));
    }
}
