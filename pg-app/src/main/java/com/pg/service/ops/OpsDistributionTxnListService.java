package com.pg.service.ops;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListItemDto;
import com.pg.api.dto.PayListRowContext;
import com.pg.entity.ChargebackFeeTier;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.CommissionService;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.service.OrgAccessService;
import com.pg.service.PayListService;
import com.pg.service.settlement.FeeListTxnBreakdownCalculator;
import com.pg.service.settlement.FeeListTxnAmountService;
import com.pg.service.settlement.SplitPayTxnFeeResolver;
import com.pg.util.CommissionTierJsonHelper;
import com.pg.util.DistributionTxnFeeSplitUtil;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayListStatusBarBuckets;
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

/**
 * 운영관리 — 유통망내역(가맹 수수료내역을 조직 시점으로 분해).
 */
@Service
public class OpsDistributionTxnListService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final OpsDistributionAccessService opsDistributionAccessService;
    private final OrgAccessService orgAccessService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PayListService payListService;
    private final CommissionService commissionService;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator;
    private final FeeListTxnAmountService feeListTxnAmountService;
    private final SplitPayTxnFeeResolver splitPayTxnFeeResolver;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;

    public OpsDistributionTxnListService(OpsDistributionAccessService opsDistributionAccessService,
                                         OrgAccessService orgAccessService,
                                         PgTrnsctnRepository pgTrnsctnRepository,
                                         OrgUnitRepository orgUnitRepository,
                                         PayListService payListService,
                                         CommissionService commissionService,
                                         DistributionFeeConfigRepository distributionFeeConfigRepository,
                                         FeeListTxnBreakdownCalculator feeListTxnBreakdownCalculator,
                                         FeeListTxnAmountService feeListTxnAmountService,
                                         SplitPayTxnFeeResolver splitPayTxnFeeResolver,
                                         HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository) {
        this.opsDistributionAccessService = opsDistributionAccessService;
        this.orgAccessService = orgAccessService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.payListService = payListService;
        this.commissionService = commissionService;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.feeListTxnBreakdownCalculator = feeListTxnBreakdownCalculator;
        this.feeListTxnAmountService = feeListTxnAmountService;
        this.splitPayTxnFeeResolver = splitPayTxnFeeResolver;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        return opsDistributionAccessService.accessMeta(authentication, "distributionTxnList");
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
        Optional<String> deny = opsDistributionAccessService.accessDeniedReason(authentication);
        if (deny.isPresent()) {
            return emptyPage(page, size, deny.get());
        }
        Set<String> allowedMerchants = orgAccessService.visibleMerchantCompCodes(authentication);
        if (allowedMerchants != null && allowedMerchants.isEmpty()) {
            return emptyPage(page, size, null);
        }
        OrgLevel viewerLevel = opsDistributionAccessService.resolveViewerOrgLevel(authentication);

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

        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        Map<String, CommissionPolicy> polCache = new HashMap<>();
        Map<String, DistributionFeeConfig> distCfgCache = new HashMap<>();

        List<String> mids = slice.getContent().stream()
                .map(PgTrnsctn::getMerchantId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        Map<String, PayListRowContext> ctxByMerchant = mids.isEmpty()
                ? Map.of() : payListService.buildPayListRowContextMap(mids);
        SplitPayTxnFeeResolver.InstallmentCache splitPayCache =
                splitPayTxnFeeResolver.buildCache(slice.getContent());

        List<Map<String, Object>> rows = new ArrayList<>();
        int rowNoStart = (pageOneBased - 1) * pageSize + 1;
        int rowIdx = 0;
        for (PgTrnsctn t : slice.getContent()) {
            if (t.getMerchantId() == null || t.getMerchantId().isBlank()) {
                continue;
            }
            Map<String, Object> row = buildRow(t, ctxByMerchant, polCache, distCfgCache, feeResolver, displayZone,
                    monthCbCountCache, tiersByPolicyId, splitPayCache, viewerLevel);
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
        if (viewerLevel != null) {
            meta.put("viewerOrgLevel", viewerLevel.name());
            meta.put("viewerOrgLevelNm", viewerLevel.getNameKo());
        }
        try {
            Map<String, Object> fin = payListService.computeFinancialSummaryForSpec(spec, authentication, searchOrderDir);
            if (fin != null) {
                fin.put("distributionTxnSummary", true);
                fin.put("totalCount", slice.getTotalElements());
                meta.put("payListFinancialSummary", fin);
            }
        } catch (RuntimeException ignored) {
            /* 집계 실패 시 목록만 */
        }
        pr.setMeta(meta);
        return pr;
    }

    private Map<String, Object> buildRow(PgTrnsctn t,
                                         Map<String, PayListRowContext> ctxByMerchant,
                                         Map<String, CommissionPolicy> polCache,
                                         Map<String, DistributionFeeConfig> distCfgCache,
                                         FeeCurrencyRoundResolver feeResolver,
                                         ZoneId displayZone,
                                         Map<String, Long> monthCbCountCache,
                                         Map<Long, List<ChargebackFeeTier>> tiersByPolicyId,
                                         SplitPayTxnFeeResolver.InstallmentCache splitPayCache,
                                         OrgLevel viewerLevel) {
        String compId = t.getMerchantId().trim();
        PayListRowContext payCtx = ctxByMerchant.get(compId);
        Map<String, Object> payRow = PayListItemDto.from(t, payCtx, displayZone);
        String payCurKey = PayListItemDto.payCurKeyForFeeCompute(t, payCtx);
        FeeListRoundingPolicy feeListRp = feeResolver.forCurrency(payCurKey);

        CommissionPolicy pol = polCache.computeIfAbsent(compId,
                id -> commissionService.resolveCommissionPolicyForSettlement(id));
        DistributionFeeConfig distCfg = distCfgCache.computeIfAbsent(compId, this::resolveDistributionConfig);

        FeeListTxnBreakdownCalculator.FeeListTxnBreakdown br = feeListTxnBreakdownCalculator.computeFeeListTxnBreakdown(
                t, compId, pol, monthCbCountCache, tiersByPolicyId,
                payCtx != null ? payCtx.getSettlement() : null, feeListRp, splitPayCache);
        FeeListTxnAmountService.FeeListTxnAmounts amts = feeListTxnAmountService.compute(
                t, payCtx, pol, payCurKey, feeResolver, monthCbCountCache, tiersByPolicyId, splitPayCache);

        BigDecimal txnAmt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        DistributionTxnFeeSplitUtil.TierFeeMap tierMap = DistributionTxnFeeSplitUtil.splitTxnFees(
                distCfg, br, txnAmt, feeListRp);
        DistributionTxnFeeSplitUtil.ViewerSlice slice = DistributionTxnFeeSplitUtil.viewerSlice(
                tierMap, viewerLevel, amts.totalFee());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("compNm", payRow.get("compNm"));
        m.put("compId", payRow.get("compId"));
        m.put("trnDate", payRow.get("trnDate"));
        m.put("trnTime", payRow.get("trnTime"));
        m.put("routeNo", payRow.get("routeNo"));
        m.put("chillTransactionId", payRow.get("chillTransactionId"));
        m.put("trnId", payRow.get("trnId"));
        m.put("statusNm", PayListStatusBarBuckets.pgStatusDisplayLabel(t.getStatus()));
        m.put("amount", txnAmt);
        m.put("curType", payCurKey);
        m.put("totalFee", money(amts.totalFee(), feeListRp));
        m.put("feeVat", money(amts.feeVat(), feeListRp));
        m.put("expectedPayout", money(amts.expectedPayout(), feeListRp));

        putTierFee(m, "hqFee", tierMap, OrgLevel.HEADQUARTERS, feeListRp);
        putTierFee(m, "regionalFee", tierMap, OrgLevel.REGIONAL, feeListRp);
        putTierFee(m, "masterFee", tierMap, OrgLevel.MASTER_DIST, feeListRp);
        putTierFee(m, "branchFee", tierMap, OrgLevel.BRANCH, feeListRp);
        putTierFee(m, "agencyFee", tierMap, OrgLevel.AGENCY, feeListRp);
        putTierFee(m, "salesOfficeFee", tierMap, OrgLevel.SALES_OFFICE, feeListRp);

        m.put("upstreamFee", money(slice.upstreamFee(), feeListRp));
        m.put("ownTierFee", money(slice.ownTierFee(), feeListRp));
        m.put("downstreamFee", money(slice.downstreamFee(), feeListRp));
        m.put("passThroughFee", money(slice.passThroughFee(), feeListRp));
        if (viewerLevel != null) {
            m.put("viewerOrgLevelNm", viewerLevel.getNameKo());
        }
        return m;
    }

    private DistributionFeeConfig resolveDistributionConfig(String compId) {
        Optional<DistributionFeeConfig> opt = distributionFeeConfigRepository.findByCompId(compId);
        if (opt.isPresent()) {
            return opt.get();
        }
        CommissionPolicy pol = commissionService.resolveCommissionPolicyForSettlement(compId);
        if (pol == null || pol.getTierCommissionJson() == null || pol.getTierCommissionJson().isBlank()) {
            return null;
        }
        DistributionFeeConfig cfg = new DistributionFeeConfig();
        cfg.setCompId(compId);
        CommissionTierJsonHelper.applyTierJsonToDistribution(pol.getTierCommissionJson(), cfg);
        return cfg;
    }

    private static void putTierFee(Map<String, Object> m,
                                   String key,
                                   DistributionTxnFeeSplitUtil.TierFeeMap tierMap,
                                   OrgLevel level,
                                   FeeListRoundingPolicy rp) {
        m.put(key, money(tierMap.get(level), rp));
    }

    private static double money(BigDecimal v, FeeListRoundingPolicy rp) {
        return FeeListRoundingPolicy.round(v != null ? v : BigDecimal.ZERO, rp).doubleValue();
    }

    private static PageResult<Map<String, Object>> emptyPage(int page, int size, String denyReason) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(List.of());
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

    private static boolean allowedMerchantsContains(Set<String> allowed, String code) {
        if (allowed == null) {
            return true;
        }
        return allowed.contains(code);
    }

    private static Sort.Direction sortDirection(String searchOrderDir) {
        if (searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim())) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
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
            parts.add(cb.between(root.get("createdAt"), fromDt, toDt));
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
        if ("COMP_NM".equals(effFt) || ("ALL".equals(effFt) && effKw.isEmpty())
                || (!"ALL".equals(effFt) && effKw.isEmpty())) {
            if ("ALL".equals(effFt) && effKw.isEmpty()) {
                return null;
            }
            if (!"ALL".equals(effFt) && effKw.isEmpty()) {
                return null;
            }
        }
        if ("COMP_ID".equals(effFt)) {
            return cb.like(root.get("merchantId"), "%" + escapeSqlLike(effKw) + "%", '\\');
        }
        if ("ALL".equals(effFt)) {
            String esc = escapeSqlLike(effKw);
            List<Predicate> ors = new ArrayList<>();
            ors.add(cb.like(root.get("merchantId"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("orderNo"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("chillTransactionId"), "%" + esc + "%", '\\'));
            ors.add(cb.like(root.get("trnId"), "%" + esc + "%", '\\'));
            return cb.or(ors.toArray(new Predicate[0]));
        }
        return null;
    }

    private static String escapeSqlLike(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
