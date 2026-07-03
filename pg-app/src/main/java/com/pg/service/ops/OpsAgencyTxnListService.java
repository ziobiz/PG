package com.pg.service.ops;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListItemDto;
import com.pg.api.dto.PayListRowContext;
import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgAgencyCostPolicy;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyCostPolicyRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.PayListService;
import com.pg.service.OrgAccessService;
import com.pg.service.settlement.PgAgencyCostTxnBreakdownCalculator;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.PgAgencyCostSettleScheduleUtil;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 검수관리 — 대행수수료(수수료내역 패턴 + PG 대행수수료설정 + 맨 끝 정산유무).
 */
@Service
public class OpsAgencyTxnListService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final TaxReportService taxReportService;
    private final OrgAccessService orgAccessService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PayListService payListService;
    private final PgAgencyCostPolicyRepository pgAgencyCostPolicyRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final PgAgencyCostTxnBreakdownCalculator pgAgencyCostTxnBreakdownCalculator;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;

    public OpsAgencyTxnListService(TaxReportService taxReportService,
                                   OrgAccessService orgAccessService,
                                   PgTrnsctnRepository pgTrnsctnRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   PayListService payListService,
                                   PgAgencyCostPolicyRepository pgAgencyCostPolicyRepository,
                                   PgAgencyRepository pgAgencyRepository,
                                   PgAgencyCostTxnBreakdownCalculator pgAgencyCostTxnBreakdownCalculator,
                                   HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                                   MerchantPgBindingRepository merchantPgBindingRepository) {
        this.taxReportService = taxReportService;
        this.orgAccessService = orgAccessService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.payListService = payListService;
        this.pgAgencyCostPolicyRepository = pgAgencyCostPolicyRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.pgAgencyCostTxnBreakdownCalculator = pgAgencyCostTxnBreakdownCalculator;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        Map<String, Object> m = taxReportService.accessMeta(authentication);
        m.put("screen", "agencyTxnList");
        return m;
    }

    public PageResult<Map<String, Object>> list(Authentication authentication,
                                                LocalDate searchFromDate,
                                                LocalDate searchToDate,
                                                String searchCompId,
                                                String searchCompNm,
                                                String searchFieldType,
                                                String searchKeyword,
                                                String searchStatusGroup,
                                                String searchOrderDir,
                                                int page,
                                                int size) {
        Optional<String> deny = taxReportService.accessDeniedReason(authentication);
        if (deny.isPresent()) {
            return emptyPage(page, size, deny.get());
        }
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return emptyPage(page, size, null);
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
                return emptyPage(page, size, null);
            }
            merchantNameFilter = nm;
        } else {
            merchantNameFilter = null;
        }

        Specification<PgTrnsctn> spec = buildSpec(fromDt, toDt, allowedMerchants, merchantNameFilter,
                effFtFinal, effKwFinal, statusGroup);

        int pageSize = Math.min(500, Math.max(1, size));
        int pageOneBased = Math.max(1, page);
        Pageable pageable = PageRequest.of(pageOneBased - 1, pageSize,
                Sort.by(sortDirection(searchOrderDir), "createdAt")
                        .and(Sort.by(sortDirection(searchOrderDir), "trnId")));
        Page<PgTrnsctn> slice = pgTrnsctnRepository.findAll(spec, pageable);

        FeeCurrencyRoundResolver feeResolver = hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(FeeCurrencyRoundResolver::from)
                .orElseGet(() -> FeeCurrencyRoundResolver.from(null));
        ZoneId displayZone = HqLedgerSysSettingsService.resolveDisplayZoneIdFromSettings(
                hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc().orElse(null));

        Map<String, String> pgNmByCd = buildPgDisplayNameByCd();
        AgencyCostPolicyResolver policyResolver = AgencyCostPolicyResolver.from(
                pgAgencyCostPolicyRepository.findAllByOrderByPgCdAsc());

        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        List<String> mids = slice.getContent().stream()
                .map(PgTrnsctn::getMerchantId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        Map<String, Long> orgIdByMerchantCode = new HashMap<>();
        if (!mids.isEmpty()) {
            for (OrgUnit ou : orgUnitRepository.findByCodeIn(mids)) {
                if (ou.getCode() != null && !ou.getCode().isBlank() && ou.getId() != null) {
                    orgIdByMerchantCode.put(ou.getCode().trim(), ou.getId());
                }
            }
        }
        Map<Long, List<MerchantPgBinding>> bindingsByOrgId = new HashMap<>();
        if (!orgIdByMerchantCode.isEmpty()) {
            for (MerchantPgBinding b : merchantPgBindingRepository
                    .findByOrgUnitIdInOrderByOrgUnitIdAscSortOrderAsc(orgIdByMerchantCode.values())) {
                bindingsByOrgId.computeIfAbsent(b.getOrgUnitId(), k -> new ArrayList<>()).add(b);
            }
        }
        Map<String, PayListRowContext> ctxByMerchant = mids.isEmpty()
                ? Map.of() : payListService.buildPayListRowContextMap(mids);

        LocalDateTime now = LocalDateTime.now(SEOUL);
        List<Map<String, Object>> rows = new ArrayList<>();
        int rowNoStart = (pageOneBased - 1) * pageSize + 1;
        int rowIdx = 0;
        for (PgTrnsctn t : slice.getContent()) {
            if (t.getMerchantId() == null || t.getMerchantId().isBlank()) {
                continue;
            }
            String compId = t.getMerchantId().trim();
            Long orgId = orgIdByMerchantCode.get(compId);
            List<MerchantPgBinding> bindings = orgId != null
                    ? bindingsByOrgId.getOrDefault(orgId, List.of()) : List.of();
            Map<String, Object> row = buildRow(t, ctxByMerchant, bindings, policyResolver, pgNmByCd, feeResolver,
                    displayZone, monthCbCountCache, tiersByPolicyId, now);
            row.put("rowNo", rowNoStart + rowIdx);
            rowIdx++;
            rows.add(row);
        }

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(rows);
        pr.setPage(slice.getNumber() + 1);
        pr.setSize(slice.getSize());
        pr.setTotalElements(slice.getTotalElements());
        pr.setTotalPages(Math.max(1, slice.getTotalPages()));
        Map<String, Object> meta = new LinkedHashMap<>();
        payListService.putHqLedgerPayDisplayCurrencyMeta(meta);
        try {
            Map<String, Object> fin = payListService.computeFinancialSummaryForSpec(spec, authentication, searchOrderDir);
            if (fin != null) {
                fin.put("agencySummary", true);
                fin.put("totalCount", slice.getTotalElements());
                meta.put("payListFinancialSummary", fin);
            }
        } catch (RuntimeException ignored) {
            /* 집계 실패 시 목록만 반환 */
        }
        pr.setMeta(meta);
        return pr;
    }

    private Map<String, String> buildPgDisplayNameByCd() {
        Map<String, String> pgNmByCd = new HashMap<>();
        for (PgAgency a : pgAgencyRepository.findAllByOrderByPgCdAsc()) {
            if (a.getPgCd() == null || a.getPgCd().isBlank()) {
                continue;
            }
            String cd = a.getPgCd().trim().toUpperCase(Locale.ROOT);
            String nm = a.getPgNm() != null && !a.getPgNm().isBlank() ? a.getPgNm().trim() : cd;
            pgNmByCd.putIfAbsent(cd, nm);
            String norm = PgVendor.normalizePgCdKey(a.getPgCd());
            if (!norm.isEmpty()) {
                pgNmByCd.putIfAbsent(norm, nm);
            }
        }
        return pgNmByCd;
    }

    private Map<String, Object> buildRow(PgTrnsctn t,
                                         Map<String, PayListRowContext> ctxByMerchant,
                                         List<MerchantPgBinding> merchantBindings,
                                         AgencyCostPolicyResolver policyResolver,
                                         Map<String, String> pgNmByCd,
                                         FeeCurrencyRoundResolver feeResolver,
                                         ZoneId displayZone,
                                         Map<String, Long> monthCbCountCache,
                                         Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
                                         LocalDateTime now) {
        String compId = t.getMerchantId().trim();
        PayListRowContext payCtx = ctxByMerchant.get(compId);
        Map<String, Object> payRow = PayListItemDto.from(t, payCtx, displayZone);

        String vanKey = t.getVan() != null && !t.getVan().isBlank()
                ? t.getVan().trim().toUpperCase(Locale.ROOT) : "";
        String payCurKey = PayListItemDto.payCurKeyForFeeCompute(t, payCtx);
        Optional<MerchantPgBinding> bindingOpt = resolveMerchantPgBindingForTxn(
                t, merchantBindings, payCtx, pgNmByCd, vanKey);
        String bindingPgCd = bindingOpt
                .map(MerchantPgBinding::getPgCd)
                .filter(cd -> cd != null && !cd.isBlank())
                .map(String::trim)
                .orElse("");
        PgAgencyCostPolicy pol = policyResolver.resolve(bindingPgCd, vanKey, payCurKey, pgNmByCd);
        String policyPgCd = pol != null && pol.getPgCd() != null ? pol.getPgCd().trim().toUpperCase(Locale.ROOT) : vanKey;
        FeeListRoundingPolicy feeListRp = feeResolver.forCurrency(
                pol != null && pol.getCurrencyCode() != null && !pol.getCurrencyCode().isBlank()
                        ? pol.getCurrencyCode().trim() : payCurKey);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pgCd", resolveMerchantPgAcquirerLabel(t, merchantBindings, payCtx, pgNmByCd, vanKey));
        m.put("pgNm", policyPgCd.isEmpty() ? "—" : pgNmByCd.getOrDefault(policyPgCd, policyPgCd));
        m.put("compNm", payRow.get("compNm"));
        m.put("compId", payRow.get("compId"));
        m.put("trnDate", payRow.get("trnDate"));
        m.put("trnTime", payRow.get("trnTime"));
        m.put("routeNo", payRow.get("routeNo"));
        m.put("chillTransactionId", payRow.get("chillTransactionId"));
        m.put("trnId", payRow.get("trnId"));
        m.put("status", t.getStatus());
        m.put("statusNm", PayListStatusBarBuckets.pgStatusDisplayLabel(t.getStatus()));
        m.put("amount", t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO);
        m.put("curType", payCurKey);
        m.put("policyCur", pol != null && pol.getCurrencyCode() != null && !pol.getCurrencyCode().isBlank()
                ? pol.getCurrencyCode().trim() : "—");

        if (pol != null) {
            PgAgencyCostTxnBreakdownCalculator.AgencyCostTxnBreakdown br =
                    pgAgencyCostTxnBreakdownCalculator.compute(t, compId, pol, monthCbCountCache, tiersByPolicyId, feeListRp);
            String stRow = t.getStatus() != null ? t.getStatus().trim() : "";
            double txnFixed = 0d;
            double pctSum = 0d;
            if ("10".equals(stRow) || "21".equals(stRow) || "22".equals(stRow) || "30".equals(stRow) || "31".equals(stRow)
                    || "40".equals(stRow) || "41".equals(stRow) || "42".equals(stRow)) {
                txnFixed = br.perTxFee();
                pctSum = br.payFee();
            }
            m.put("txnFixedFeesSum", roundMoney(txnFixed, feeListRp));
            m.put("pctFeesSum", roundMoney(pctSum, feeListRp));
            m.put("usdtFee", roundMoney(br.usdtFee(), feeListRp));
            m.put("fxFee", roundMoney(br.fxFee(), feeListRp));
            m.put("fee3dsFee", roundMoney(br.fee3dsFee(), feeListRp));
            m.put("failFee", roundMoney(br.failFee(), feeListRp));
            m.put("cancelFee", roundMoney(br.cancelFee(), feeListRp));
            m.put("voidFee", roundMoney(br.voidFee(), feeListRp));
            m.put("manualVoidFee", roundMoney(br.manualVoidFee(), feeListRp));
            m.put("refundFee", roundMoney(br.refundFee(), feeListRp));
            m.put("chargebackFee", roundMoney(br.chargebackFee(), feeListRp));
            m.put("rollingHoldEst", roundMoney(br.rollingHoldEst(), feeListRp));
            m.put("totalAgencyFee", roundMoney(br.totalAgencyFee(), feeListRp));
            LocalDateTime txnAt = t.getCreatedAt();
            m.put("agencySettleYn", PgAgencyCostSettleScheduleUtil.agencySettleYn(pol, txnAt, now));
        } else {
            putZeroFeeColumns(m);
            m.put("agencySettleYn", "");
        }
        return m;
    }

    /**
     * 업체관리 「결제대행사 설정」(tb_merchant_pg_binding)의 PG를 거래 van·루트·통화로 매칭해
     * API연동설정(tb_pg_agency) 결제대행사명(예: JPAY API JPY)으로 표시.
     */
    private static String resolveMerchantPgAcquirerLabel(PgTrnsctn t,
                                                         List<MerchantPgBinding> merchantBindings,
                                                         PayListRowContext payCtx,
                                                         Map<String, String> pgNmByCd,
                                                         String vanKey) {
        Optional<MerchantPgBinding> binding = resolveMerchantPgBindingForTxn(t, merchantBindings, payCtx, pgNmByCd, vanKey);
        if (binding.isEmpty()) {
            return vanKey == null || vanKey.isBlank() ? "—" : lookupPgDisplayName(pgNmByCd, vanKey);
        }
        String cd = binding.get().getPgCd();
        return lookupPgDisplayName(pgNmByCd, cd != null ? cd.trim() : "");
    }

    /** 가맹점 결제대행사 설정 행 중 이번 거래에 해당하는 바인딩 */
    private static Optional<MerchantPgBinding> resolveMerchantPgBindingForTxn(PgTrnsctn t,
                                                                              List<MerchantPgBinding> merchantBindings,
                                                                              PayListRowContext payCtx,
                                                                              Map<String, String> pgNmByCd,
                                                                              String vanKey) {
        List<MerchantPgBinding> active = filterActiveBindings(merchantBindings);
        if (active.isEmpty()) {
            return bindingFromPayContext(payCtx);
        }
        List<MerchantPgBinding> matched = filterBindingsMatchingVan(active, vanKey);
        if (matched.isEmpty()) {
            matched = vanKey == null || vanKey.isBlank() ? active : List.of();
        }
        Optional<MerchantPgBinding> chosen = resolveBindingForTxn(matched, t);
        if (chosen.isEmpty() && matched.size() > 1) {
            chosen = disambiguateBindingByCurrency(matched, t, pgNmByCd);
        }
        if (chosen.isPresent()) {
            return chosen;
        }
        return bindingFromPayContext(payCtx);
    }

    private static List<MerchantPgBinding> filterActiveBindings(List<MerchantPgBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        List<MerchantPgBinding> out = new ArrayList<>();
        for (MerchantPgBinding b : bindings) {
            if (b == null) {
                continue;
            }
            String act = b.getActivationYn() != null ? b.getActivationYn().trim() : "";
            if (!act.isEmpty() && "N".equalsIgnoreCase(act)) {
                continue;
            }
            out.add(b);
        }
        return out;
    }

    private static Optional<MerchantPgBinding> bindingFromPayContext(PayListRowContext payCtx) {
        if (payCtx == null || payCtx.getBinding() == null) {
            return Optional.empty();
        }
        MerchantPgBinding b = payCtx.getBinding();
        if (b.getPgCd() == null || b.getPgCd().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(b);
    }

    private static List<MerchantPgBinding> filterBindingsMatchingVan(List<MerchantPgBinding> bindings, String vanKey) {
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        if (vanKey == null || vanKey.isBlank()) {
            return bindings;
        }
        String vanNorm = PgVendor.normalizePgCdKey(vanKey);
        List<MerchantPgBinding> matched = new ArrayList<>();
        for (MerchantPgBinding b : bindings) {
            if (bindingPgCdMatchesVan(b, vanKey, vanNorm)) {
                matched.add(b);
            }
        }
        return matched;
    }

    private static boolean bindingPgCdMatchesVan(MerchantPgBinding b, String vanKey, String vanNorm) {
        if (b.getPgCd() == null || b.getPgCd().isBlank()) {
            return false;
        }
        String cd = b.getPgCd().trim();
        if (vanKey.equalsIgnoreCase(cd)) {
            return true;
        }
        String cdNorm = PgVendor.normalizePgCdKey(cd);
        if (vanNorm.equals(cdNorm)) {
            return true;
        }
        if (PgVendor.isJpayFamily(vanKey) && PgVendor.isJpayFamily(cd)) {
            return true;
        }
        if (PgVendor.isChillPayFamily(vanKey) && PgVendor.isChillPayFamily(cd)) {
            return true;
        }
        return cdNorm.startsWith(vanNorm + "_") || cdNorm.startsWith(vanNorm);
    }

    private static Optional<MerchantPgBinding> disambiguateBindingByCurrency(List<MerchantPgBinding> list,
                                                                             PgTrnsctn t,
                                                                             Map<String, String> pgNmByCd) {
        if (list == null || list.size() <= 1) {
            return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        }
        String cur = firstNonBlankCurrency(t);
        if (cur.isEmpty()) {
            return Optional.empty();
        }
        for (MerchantPgBinding b : list) {
            String cd = b.getPgCd() != null ? b.getPgCd().trim() : "";
            String nm = lookupPgDisplayName(pgNmByCd, cd);
            String cdU = cd.toUpperCase(Locale.ROOT);
            String nmU = nm.toUpperCase(Locale.ROOT);
            if (cdU.contains(cur) || nmU.contains(cur)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    private static String firstNonBlankCurrency(PgTrnsctn t) {
        if (t == null) {
            return "";
        }
        if (t.getCurType() != null && !t.getCurType().isBlank()) {
            return t.getCurType().trim().toUpperCase(Locale.ROOT);
        }
        return "";
    }

    private static Optional<MerchantPgBinding> resolveBindingForTxn(List<MerchantPgBinding> list, PgTrnsctn t) {
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        String rootNo = t.getRouteNo() != null ? t.getRouteNo().trim() : "";
        if (rootNo.isEmpty()) {
            return preferOperationalBinding(list).or(() -> Optional.of(list.get(0)));
        }
        Optional<MerchantPgBinding> exact = list.stream()
                .filter(b -> b.getRootNo() != null && rootNo.equals(b.getRootNo().trim()))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return list.stream()
                .filter(b -> b.getRootNo() == null || b.getRootNo().isBlank())
                .findFirst()
                .or(() -> preferOperationalBinding(list))
                .or(() -> Optional.of(list.get(0)));
    }

    private static Optional<MerchantPgBinding> preferOperationalBinding(List<MerchantPgBinding> list) {
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return list.stream()
                .filter(b -> "Y".equalsIgnoreCase(String.valueOf(b.getOperationalYn()).trim()))
                .findFirst();
    }

    private static String lookupPgDisplayName(Map<String, String> pgNmByCd, String cd) {
        if (cd == null || cd.isBlank()) {
            return "—";
        }
        String u = cd.trim().toUpperCase(Locale.ROOT);
        String label = pgNmByCd.get(u);
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        String norm = PgVendor.normalizePgCdKey(cd);
        label = pgNmByCd.get(norm);
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        return cd.trim();
    }

    private static void putZeroFeeColumns(Map<String, Object> m) {
        for (String k : List.of("txnFixedFeesSum", "pctFeesSum", "usdtFee", "fxFee", "fee3dsFee",
                "failFee", "cancelFee", "voidFee", "manualVoidFee", "refundFee", "chargebackFee",
                "rollingHoldEst", "totalAgencyFee")) {
            m.put(k, 0d);
        }
    }

    private static double roundMoney(double x, FeeListRoundingPolicy rp) {
        return FeeListRoundingPolicy.round(BigDecimal.valueOf(x), rp).doubleValue();
    }

    private Specification<PgTrnsctn> buildSpec(LocalDateTime fromDt,
                                               LocalDateTime toDt,
                                               Set<String> allowedMerchants,
                                               Set<String> merchantNameFilter,
                                               String effFtFinal,
                                               String effKwFinal,
                                               String statusGroup) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            parts.add(cb.between(cb.coalesce(root.get("paidAt"), root.get("createdAt")), fromDt, toDt));
            parts.add(cb.isNotNull(root.get("merchantId")));
            parts.add(cb.notEqual(root.get("merchantId"), ""));
            if (allowedMerchants != null) {
                parts.add(root.get("merchantId").in(allowedMerchants));
            }
            if (merchantNameFilter != null) {
                parts.add(root.get("merchantId").in(merchantNameFilter));
            }
            addStatusGroupPredicate(parts, cb, root, statusGroup);
            if (!"COMP_NM".equals(effFtFinal)) {
                Predicate fieldPred = buildFieldSearchPredicate(root, query, cb, effFtFinal, effKwFinal);
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

    private static void addStatusGroupPredicate(List<Predicate> parts,
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

    private static Predicate buildFieldSearchPredicate(Root<PgTrnsctn> root,
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
            ors.add(cb.like(root.get("customerId"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("customerNm"), "%" + esc + "%", '\\'));
            ors.add(cb.like(cb.upper(root.get("van")), "%" + esc.toUpperCase(Locale.ROOT) + "%", '\\'));
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
            case "PG_CD" -> cb.like(cb.upper(root.get("van")), "%" + esc.toUpperCase(Locale.ROOT) + "%", '\\');
            case "ORDER_NO" -> cb.like(root.get("orderNo"), "%" + esc + "%", '\\');
            case "APPROVAL_NO" -> cb.or(
                    cb.like(root.get("chillTransactionId"), "%" + esc + "%", '\\'),
                    cb.like(root.get("approvalNo"), "%" + esc + "%", '\\'));
            case "ROUTE" -> cb.like(root.get("routeNo"), "%" + esc + "%", '\\');
            case "CUSTOMER_ID" -> cb.like(root.get("customerId"), "%" + esc + "%", '\\');
            case "CUSTOMER_NAME" -> cb.like(root.get("customerNm"), "%" + esc + "%", '\\');
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

    private static PageResult<Map<String, Object>> emptyPage(int page, int size, String denyReason) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(Math.max(1, page));
        pr.setSize(Math.max(1, size));
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        if (denyReason != null && !denyReason.isBlank()) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("accessDenied", true);
            meta.put("accessDeniedReason", denyReason);
            pr.setMeta(meta);
        }
        return pr;
    }

    private static Sort.Direction sortDirection(String searchOrderDir) {
        return (searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim()))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private static boolean allowedMerchantsContains(Set<String> allowed, String code) {
        if (allowed == null) {
            return true;
        }
        return allowed.contains(code);
    }

    private static String escapeSqlLike(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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
}
