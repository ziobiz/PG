package com.pg.service.ops;

import com.pg.api.dto.PayListSearchRequest;
import com.pg.entity.PgAgency;
import com.pg.entity.PgAgencyCostPolicy;
import com.pg.repository.PgAgencyCostPolicyRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.service.IntegratedCheckService;
import com.pg.util.PercentDecimalHelper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 검수관리 — 원가검수.
 * 적용건수·원가 = 통합체크와 동일 대사(대행사) 일별 집계만.
 * ICOPAY 건수·금액은 참고 표시이며, 대사가 없어도 원가 산출에 보강·적용하지 않음.
 */
@Service
public class OpsCostAuditService {

    private final IntegratedCheckService integratedCheckService;
    private final PgAgencyCostPolicyRepository costPolicyRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final TaxReportService taxReportService;

    public OpsCostAuditService(IntegratedCheckService integratedCheckService,
                               PgAgencyCostPolicyRepository costPolicyRepository,
                               PgAgencyRepository pgAgencyRepository,
                               TaxReportService taxReportService) {
        this.integratedCheckService = integratedCheckService;
        this.costPolicyRepository = costPolicyRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.taxReportService = taxReportService;
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        Map<String, Object> m = taxReportService.accessMeta(authentication);
        m.put("screen", "costAudit");
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Authentication authentication,
                                    LocalDate searchFromDate,
                                    LocalDate searchToDate,
                                    String searchKeyword,
                                    String searchOrderNo,
                                    String searchPayDivCd,
                                    String searchOrderDir,
                                    String searchAcquirer,
                                    PayListSearchRequest payTemplate) {
        return list(authentication, searchFromDate, searchToDate, searchKeyword, searchOrderNo,
                searchPayDivCd, searchOrderDir, searchAcquirer, payTemplate, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Authentication authentication,
                                    LocalDate searchFromDate,
                                    LocalDate searchToDate,
                                    String searchKeyword,
                                    String searchOrderNo,
                                    String searchPayDivCd,
                                    String searchOrderDir,
                                    String searchAcquirer,
                                    PayListSearchRequest payTemplate,
                                    boolean includeZeroCount) {
        Optional<String> deny = taxReportService.accessDeniedReason(authentication);
        if (deny.isPresent()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("list", List.of());
            empty.put("meta", Map.of("denied", deny.get(), "costAudit", true));
            return empty;
        }
        LocalDate from = searchFromDate != null ? searchFromDate : LocalDate.now().minusDays(7);
        LocalDate to = searchToDate != null ? searchToDate : LocalDate.now();
        Map<String, Object> base = integratedCheckService.buildIntegratedCheckSummary(
                from, to, searchKeyword, searchOrderNo, searchPayDivCd, searchOrderDir,
                searchAcquirer, payTemplate, authentication, includeZeroCount);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> days = base.get("list") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();
        String metaAcquirer = String.valueOf(base.get("meta") instanceof Map<?, ?> mm ? mm.get("acquirer") : searchAcquirer);

        List<Map<String, Object>> out = new ArrayList<>();
        int rowNo = 1;
        for (Map<String, Object> dayRow : days) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rowNo", rowNo++);
            row.put("day", dayRow.get("day"));
            row.put("acquirerCode", dayRow.get("acquirerCode"));
            row.put("acquirerLabel", dayRow.get("acquirerLabel"));
            row.put("match", dayRow.get("match"));

            @SuppressWarnings("unchecked")
            Map<String, Object> acq = dayRow.get("acquirer") instanceof Map<?, ?> a
                    ? (Map<String, Object>) a : Map.of();
            @SuppressWarnings("unchecked")
            Map<String, Object> icopay = dayRow.get("icopay") instanceof Map<?, ?> i
                    ? (Map<String, Object>) i : Map.of();

            boolean queryReady = !Boolean.FALSE.equals(dayRow.get("acquirerQueryReady"))
                    && !Boolean.TRUE.equals(acq.get("queryPending"));
            long acqCnt = longVal(acq.get("totalCount"));
            long icoCnt = longVal(icopay.get("totalCount"));
            BigDecimal acqAmt = sumCurrency(acq.get("paymentByCurrency"));
            BigDecimal icoAmt = sumCurrency(icopay.get("paymentByCurrency"));

            /*
             * 적용건수·적용금액·원가 = 대사(대행사)만. ICOPAY는 참고 열만.
             * 대사가 없다고 ICOPAY 건수·금액을 적용(원가 산출)에 쓰지 않음.
             */
            boolean useRecon = queryReady && (acqCnt > 0 || acqAmt.signum() > 0);
            String dataSource = useRecon ? "RECON" : "RECON_NONE";
            String dataSourceLabel = useRecon ? "대사" : "대사없음(참고만)";
            long baseCnt = useRecon ? acqCnt : 0L;
            BigDecimal baseAmt = useRecon ? acqAmt : BigDecimal.ZERO;

            row.put("dataSource", dataSource);
            row.put("dataSourceLabel", dataSourceLabel);
            row.put("reconCount", acqCnt);
            row.put("icopayCount", icoCnt);
            row.put("baseCount", baseCnt);
            row.put("baseAmount", baseAmt);
            row.put("reconAmount", acqAmt);
            row.put("icopayAmount", icoAmt);

            String rowAcq = dayRow.get("acquirerCode") != null
                    ? String.valueOf(dayRow.get("acquirerCode")) : metaAcquirer;
            PgAgencyCostPolicy pol = resolvePolicyForAcquirer(rowAcq);
            applyCost(row, pol, baseAmt, baseCnt);
            out.add(row);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("list", out);
        Map<String, Object> meta = new LinkedHashMap<>();
        if (base.get("meta") instanceof Map<?, ?> bm) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bmm = (Map<String, Object>) bm;
            meta.putAll(bmm);
        }
        meta.put("costAudit", true);
        meta.put("includeZeroCount", includeZeroCount);
        meta.put("note", "원가검수는 대사(대행사) 일별 집계로만 원가를 산출합니다. ICOPAY건수·금액은 참고용이며 대사가 없어도 적용건수·원가에 넣지 않습니다. 「전체」는 대행사별 행으로 나열합니다. 기본은 건수 0 행을 숨기고, 「무건수노출」사용 시 모두 표시합니다.");
        payload.put("meta", meta);
        payload.put("totalElements", out.size());
        payload.put("page", 1);
        payload.put("size", Math.max(out.size(), 1));
        payload.put("totalPages", 1);
        return payload;
    }

    private void applyCost(Map<String, Object> row, PgAgencyCostPolicy pol, BigDecimal baseAmt, long baseCnt) {
        if (pol == null) {
            row.put("payRate", "0");
            row.put("perTxFee", "0");
            row.put("pctFeesSum", 0d);
            row.put("txnFixedFeesSum", 0d);
            row.put("totalAgencyFee", 0d);
            row.put("feeVat", 0d);
            row.put("feeWithVat", 0d);
            row.put("policyCur", "—");
            row.put("costNm", "");
            return;
        }
        BigDecimal payRate = pol.getPayRate() != null ? pol.getPayRate() : BigDecimal.ZERO;
        BigDecimal perTx = pol.getPerTxFee() != null ? pol.getPerTxFee() : BigDecimal.ZERO;
        BigDecimal pct = baseAmt.multiply(payRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal fixed = perTx.multiply(BigDecimal.valueOf(Math.max(0L, baseCnt)));
        BigDecimal total = pct.add(fixed);
        BigDecimal vatRate = pol.getVatRatePct() != null ? pol.getVatRatePct() : BigDecimal.ZERO;
        BigDecimal vat = total.multiply(vatRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        row.put("payRate", PercentDecimalHelper.toPlainOneDecimal(payRate));
        row.put("perTxFee", PercentDecimalHelper.toPlainOneDecimal(perTx));
        row.put("pctFeesSum", pct.doubleValue());
        row.put("txnFixedFeesSum", fixed.doubleValue());
        row.put("totalAgencyFee", total.doubleValue());
        row.put("feeVat", vat.doubleValue());
        row.put("feeWithVat", total.add(vat).doubleValue());
        row.put("policyCur", pol.getCurrencyCode() != null ? pol.getCurrencyCode() : "—");
        row.put("costNm", pol.getCostNm() != null ? pol.getCostNm() : "");
        row.put("vatRatePct", PercentDecimalHelper.toPlainOneDecimal(vatRate));
    }

    private PgAgencyCostPolicy resolvePolicyForAcquirer(String acquirerRaw) {
        String acq = acquirerRaw != null ? acquirerRaw.trim().toUpperCase(Locale.ROOT) : "";
        List<PgAgencyCostPolicy> all = costPolicyRepository.findAllByOrderByPgCdAsc();
        for (PgAgencyCostPolicy p : all) {
            if (p.getUseYn() != null && "N".equalsIgnoreCase(p.getUseYn().trim())) {
                continue;
            }
            String pg = p.getPgCd() != null ? p.getPgCd().trim().toUpperCase(Locale.ROOT) : "";
            String vendor = p.getVendorNm() != null ? p.getVendorNm().trim().toUpperCase(Locale.ROOT) : "";
            if (matchAcquirer(acq, pg, vendor)) {
                return p;
            }
        }
        for (PgAgency a : pgAgencyRepository.findAllByOrderByPgCdAsc()) {
            String pg = a.getPgCd() != null ? a.getPgCd().trim().toUpperCase(Locale.ROOT) : "";
            if (!matchAcquirer(acq, pg, a.getPgNm() != null ? a.getPgNm().toUpperCase(Locale.ROOT) : "")) {
                continue;
            }
            if (a.getCostPolicyId() != null) {
                Optional<PgAgencyCostPolicy> pol = costPolicyRepository.findById(a.getCostPolicyId());
                if (pol.isPresent()) {
                    return pol.get();
                }
            }
        }
        return null;
    }

    private static boolean matchAcquirer(String acq, String pg, String vendor) {
        if (acq == null || acq.isBlank()) {
            return false;
        }
        if (pg.equals(acq) || pg.startsWith(acq + "_") || vendor.contains(acq)) {
            return true;
        }
        if ("ELEMENTPAY".equals(acq) && (pg.contains("ELEMENT") || vendor.contains("ELEMENT"))) {
            return true;
        }
        if ("JPAY".equals(acq) && (pg.contains("JPAY") || vendor.contains("JPAY") || pg.contains("CHILL"))) {
            return true;
        }
        if ("PAYSO".equals(acq) && (pg.startsWith("PAYSO") || vendor.contains("PAYSO"))) {
            return true;
        }
        if ("KSNET".equals(acq) && (pg.contains("KSNET") || vendor.contains("KSNET"))) {
            return true;
        }
        if ("OX".equals(acq) && (pg.startsWith("OX") || vendor.contains("OX"))) {
            return true;
        }
        return false;
    }

    private static long longVal(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static BigDecimal sumCurrency(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Object v : m.values()) {
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim().replace(",", "");
            if (s.isEmpty() || "—".equals(s) || "-".equals(s)) {
                continue;
            }
            try {
                sum = sum.add(new BigDecimal(s));
            } catch (Exception ignored) {
                /* skip */
            }
        }
        return sum;
    }
}
