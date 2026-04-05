package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListItemDto;
import com.pg.api.dto.PayListRowContext;
import com.pg.api.dto.PayListSearchRequest;
import com.pg.entity.AppUser;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementSetting;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.util.PayListStatusBarBuckets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class PayListService {

    private final PgTrnsctnRepository trnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final HqNotifyMappingService hqNotifyMappingService;

    @PersistenceContext
    private EntityManager entityManager;

    public PayListService(PgTrnsctnRepository trnsctnRepository,
                          OrgUnitRepository orgUnitRepository,
                          MerchantProfileRepository merchantProfileRepository,
                          MerchantPgBindingRepository merchantPgBindingRepository,
                          DistributionFeeConfigRepository distributionFeeConfigRepository,
                          CommissionPolicyRepository commissionPolicyRepository,
                          SettlementSettingRepository settlementSettingRepository,
                          HqNotifyMappingService hqNotifyMappingService) {
        this.trnsctnRepository = trnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqNotifyMappingService = hqNotifyMappingService;
    }

    public PageResult<Map<String, Object>> search(PayListSearchRequest req, Authentication authentication) {
        if (req == null) {
            req = new PayListSearchRequest();
        }
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        int page = Math.max(1, req.getPage());
        int size = Math.min(1000, Math.max(1, req.getSize()));
        Pageable p = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to);
        Page<PgTrnsctn> result = trnsctnRepository.findAll(spec, p);
        List<String> merchantCodes = result.getContent().stream().map(PgTrnsctn::getMerchantId).distinct().collect(Collectors.toList());
        Map<String, PayListRowContext> ctxByCode = buildPayListRowContextMap(merchantCodes);

        HqNotifyMappingService.DisplayTransformCache displayCache = hqNotifyMappingService.loadDisplayTransformCache();
        List<Map<String, Object>> list = new ArrayList<>();
        for (PgTrnsctn t : result.getContent()) {
            PayListRowContext ctx = ctxByCode.get(t.getMerchantId());
            Map<String, Object> row = PayListItemDto.from(t, ctx);
            String pgCd = resolvePgCdForPayListRow(ctx, t);
            hqNotifyMappingService.applyDisplayTransform(displayCache, pgCd, row);
            list.add(row);
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(result.getNumber() + 1);
        pr.setSize(result.getSize());
        pr.setTotalElements(result.getTotalElements());
        pr.setTotalPages(result.getTotalPages());
        try {
            Map<String, Object> bar = computePgTxnStatusBar(req, authentication);
            Map<String, Object> fin = computePayListFinancialSummary(req, authentication);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("payListStatusBar", bar);
            meta.put("payListFinancialSummary", fin);
            pr.setMeta(meta);
        } catch (RuntimeException ignored) {
            /* 집계 실패 시 목록만 반환 */
        }
        return pr;
    }

    private Map<String, PayListRowContext> buildPayListRowContextMap(Collection<String> merchantCodes) {
        Map<String, PayListRowContext> ctxByCode = new HashMap<>();
        if (merchantCodes == null || merchantCodes.isEmpty()) {
            return ctxByCode;
        }
        Set<String> codes = merchantCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        Map<String, OrgUnit> ouByCode = new HashMap<>();
        for (OrgUnit ou : orgUnitRepository.findAll()) {
            if (codes.contains(ou.getCode())) {
                ouByCode.put(ou.getCode(), ou);
            }
        }
        Optional<CommissionPolicy> defaultPolicy = commissionPolicyRepository.findByScope("DEFAULT");
        for (String code : codes) {
            OrgUnit merchant = ouByCode.get(code);
            String compNm = merchant != null ? merchant.getName() : code;
            MerchantProfile profile = merchant != null
                    ? merchantProfileRepository.findByOrgUnitId(merchant.getId()).orElse(null)
                    : null;
            MerchantPgBinding binding = pickBinding(merchant);
            DistributionFeeConfig dist = distributionFeeConfigRepository.findByCompId(code).orElse(null);
            CommissionPolicy pol = commissionPolicyRepository.findByScope(code).or(() -> defaultPolicy).orElse(null);
            SettlementSetting ss = merchant != null
                    ? settlementSettingRepository.findByOrgUnitId(merchant.getId()).orElse(null)
                    : null;
            String[] hier = hierarchyNames(merchant);
            ctxByCode.put(code, new PayListRowContext(compNm, profile, binding, dist, pol, ss,
                    hier[0], hier[1], hier[2]));
        }
        return ctxByCode;
    }

    /**
     * 동일 검색 조건·조직 권한 범위 전체 건 기준 금액 요약(페이지·정렬과 무관). 건수=승인 건수, 금액은 통화별.
     */
    private Map<String, Object> computePayListFinancialSummary(PayListSearchRequest req, Authentication authentication) {
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<PgTrnsctn> root = cq.from(PgTrnsctn.class);
        cq.multiselect(
                root.get("merchantId"),
                root.get("status"),
                root.get("curType"),
                root.get("amtKrw"));
        cq.where(spec.toPredicate(root, cq, cb));
        List<Tuple> rows = entityManager.createQuery(cq).getResultList();

        Set<String> mcodes = new HashSet<>();
        for (Tuple tup : rows) {
            String mid = tup.get(0, String.class);
            if (mid != null && !mid.isBlank()) {
                mcodes.add(mid.trim());
            }
        }
        Map<String, PayListRowContext> ctxMap = buildPayListRowContextMap(mcodes);

        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        OrgLevel level = PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository);
        boolean multi = PayListStatusBarBuckets.isMultiCurrencyViewer(level);
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository);
        String primaryNorm = PayListStatusBarBuckets.normalizeCurrency(primary);

        Map<String, BigDecimal> approve = new HashMap<>();
        Map<String, BigDecimal> cancel = new HashMap<>();
        Map<String, BigDecimal> feeVatSum = new HashMap<>();
        Map<String, BigDecimal> hold = new HashMap<>();
        Map<String, BigDecimal> payout = new HashMap<>();
        long successCount = 0;

        for (Tuple tup : rows) {
            String mid = tup.get(0, String.class);
            String st = tup.get(1, String.class);
            String curRaw = tup.get(2, String.class);
            BigDecimal amt = tup.get(3, BigDecimal.class);
            if (amt == null) {
                amt = BigDecimal.ZERO;
            }
            String cur = PayListStatusBarBuckets.normalizeCurrency(curRaw);
            if (!multi && !primaryNorm.equals(cur)) {
                continue;
            }
            PayListRowContext ctx = mid != null ? ctxMap.get(mid.trim()) : null;
            if ("10".equals(st)) {
                successCount++;
                approve.merge(cur, amt, BigDecimal::add);
                PayListItemDto.ApprovedSettlementParts p = PayListItemDto.approvedSettlementParts(amt, ctx);
                feeVatSum.merge(cur, p.feeAmt.add(p.feeVat), BigDecimal::add);
                hold.merge(cur, p.holdAmt, BigDecimal::add);
                payout.merge(cur, p.settleAmt, BigDecimal::add);
            } else if (PayListItemDto.isCancelAmountStatus(st)) {
                cancel.merge(cur, amt, BigDecimal::add);
            }
        }

        Set<String> union = new TreeSet<>();
        union.addAll(approve.keySet());
        union.addAll(cancel.keySet());
        List<String> sortedUnion = new ArrayList<>(union);
        PayListStatusBarBuckets.sortCurrencyCodes(sortedUnion);

        Map<String, String> approvePlain = new LinkedHashMap<>();
        Map<String, String> cancelPlain = new LinkedHashMap<>();
        Map<String, String> paymentPlain = new LinkedHashMap<>();
        for (String c : sortedUnion) {
            BigDecimal a = approve.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal k = cancel.getOrDefault(c, BigDecimal.ZERO);
            approvePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a));
            cancelPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(k));
            paymentPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a.subtract(k)));
        }

        List<String> succCurrencies = new ArrayList<>(approve.keySet());
        PayListStatusBarBuckets.sortCurrencyCodes(succCurrencies);
        Map<String, String> feePlain = new LinkedHashMap<>();
        Map<String, String> holdPlain = new LinkedHashMap<>();
        Map<String, String> payoutPlain = new LinkedHashMap<>();
        for (String c : succCurrencies) {
            feePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(feeVatSum.getOrDefault(c, BigDecimal.ZERO)));
            holdPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(hold.getOrDefault(c, BigDecimal.ZERO)));
            payoutPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(payout.getOrDefault(c, BigDecimal.ZERO)));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("successCount", successCount);
        out.put("multiCurrency", multi);
        out.put("primaryCurrency", primaryNorm);
        out.put("approveByCurrency", approvePlain);
        out.put("cancelByCurrency", cancelPlain);
        out.put("paymentByCurrency", paymentPlain);
        out.put("feeByCurrency", feePlain);
        out.put("holdByCurrency", holdPlain);
        out.put("payoutByCurrency", payoutPlain);
        return out;
    }

    /**
     * 동일 검색 조건 전체 건 기준 상태·통화별 합계(페이지와 무관).
     */
    private Map<String, Object> computePgTxnStatusBar(PayListSearchRequest req, Authentication authentication) {
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<PgTrnsctn> root = cq.from(PgTrnsctn.class);
        jakarta.persistence.criteria.Path<String> st = root.get("status");
        Expression<String> curExpr = cb.upper(cb.trim(CriteriaBuilder.Trimspec.BOTH,
                cb.coalesce(root.get("curType"), cb.literal("KRW"))));
        Expression<String> bucket = cb.<String>selectCase()
                .when(cb.equal(st, "10"), cb.literal(PayListStatusBarBuckets.SUCCESS))
                .when(cb.or(
                        cb.equal(st, "F0"),
                        cb.equal(cb.upper(st), "F0"),
                        cb.equal(st, "99")
                ), cb.literal(PayListStatusBarBuckets.FAIL))
                .when(cb.or(
                        cb.equal(st, "21"), cb.equal(st, "22"),
                        cb.equal(st, "40"), cb.equal(st, "41"), cb.equal(st, "42")
                ), cb.literal(PayListStatusBarBuckets.VOID))
                .when(cb.or(cb.equal(st, "30"), cb.equal(st, "31")), cb.literal(PayListStatusBarBuckets.REFUND))
                .otherwise(cb.literal(PayListStatusBarBuckets.OTHER));
        cq.multiselect(bucket, curExpr, cb.count(root),
                cb.sum(cb.coalesce(root.get("amtKrw"), cb.literal(BigDecimal.ZERO))));
        cq.where(spec.toPredicate(root, cq, cb));
        cq.groupBy(bucket, curExpr);
        List<Tuple> tuples = entityManager.createQuery(cq).getResultList();
        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        for (Tuple t : tuples) {
            String b = t.get(0, String.class);
            String c = t.get(1, String.class);
            Long cnt = t.get(2, Long.class);
            BigDecimal sum = t.get(3, BigDecimal.class);
            if (cnt == null) {
                cnt = 0L;
            }
            if (sum == null) {
                sum = BigDecimal.ZERO;
            }
            roll.add(b, c, sum, cnt);
        }
        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        OrgLevel level = PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository);
        boolean multi = PayListStatusBarBuckets.isMultiCurrencyViewer(level);
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository);
        String variant = req.getPayListVariant() == null || req.getPayListVariant().isBlank()
                ? "INTEGRATED" : req.getPayListVariant().trim().toUpperCase(Locale.ROOT);
        /* 통합·결제내역(INTEGRATED): 성공·실패·무효·환불·기타 전 구간 + 0건도 표시. 그 외 변형 화면은 건수 있는 버킷만 */
        boolean showAllBuckets = "INTEGRATED".equals(variant);
        return roll.toPayload(multi, primary, false, showAllBuckets);
    }

    private static String resolvePgCdForPayListRow(PayListRowContext ctx, PgTrnsctn t) {
        if (ctx != null && ctx.getBinding() != null) {
            String p = ctx.getBinding().getPgCd();
            if (p != null && !p.isBlank()) {
                return p.trim();
            }
        }
        if (t != null && t.getVan() != null && !t.getVan().isBlank()) {
            return t.getVan().trim();
        }
        return "";
    }

    private MerchantPgBinding pickBinding(OrgUnit merchant) {
        if (merchant == null) return null;
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(merchant.getId());
        if (list.isEmpty()) return null;
        return list.stream().filter(b -> "Y".equalsIgnoreCase(String.valueOf(b.getOperationalYn()).trim()))
                .findFirst()
                .orElse(list.get(0));
    }

    /** [0]=총판, [1]=지사, [2]=대리점(영업점) */
    private String[] hierarchyNames(OrgUnit merchant) {
        String regional = "", master = "", branch = "";
        OrgUnit cur = merchant;
        for (int i = 0; i < 8 && cur != null; i++) {
            if (cur.getOrgLevel() != null) {
                switch (cur.getOrgLevel()) {
                    case MASTER_DIST -> regional = cur.getName();
                    case BRANCH -> master = cur.getName();
                    case AGENCY, SALES_OFFICE -> branch = cur.getName();
                    default -> { }
                }
            }
            Long pid = cur.getParentId();
            cur = pid != null ? orgUnitRepository.findById(pid).orElse(null) : null;
        }
        return new String[] { regional, master, branch };
    }

    private Specification<PgTrnsctn> buildSpecification(PayListSearchRequest req, LocalDateTime fromDt, LocalDateTime toDt) {
        Set<String> merchantCodes = resolveMerchantFilterCodes(req);
        if (merchantCodes != null && merchantCodes.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        String variant = req.getPayListVariant() == null || req.getPayListVariant().isBlank()
                ? "INTEGRATED" : req.getPayListVariant().trim().toUpperCase(Locale.ROOT);
        final Set<String> mcsFinal = merchantCodes;
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (mcsFinal != null) {
                parts.add(root.get("merchantId").in(mcsFinal));
            }
            if (fromDt != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
            }
            if (toDt != null) {
                parts.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDt));
            }
            parts.add(variantPredicate(root, cb, variant));
            addPayDivPredicate(parts, root, cb, req.getSearchPayDivCd());
            addPayProcPredicate(parts, root, cb, req.getSearchPayProcCd());
            addTranFactorColumnPredicates(parts, root, cb, req.getSearchTranFactor(), req.getSearchTranValue());
            addPgAgencyPredicate(parts, root, cb, req.getSearchPgCd());
            addKeywordPredicate(parts, root, cb, req.getSearchKeyword());
            addLike(parts, root, cb, "chillTransactionId", req.getSearchChillTxnId());
            addLike(parts, root, cb, "approvalNo", req.getSearchCardAprvNo());
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    /**
     * null = 가맹점 제한 없음, 비어 있지 않은 Set = 해당 코드만, empty Set = 조건 불충족(결과 0건).
     */
    private Set<String> resolveMerchantFilterCodes(PayListSearchRequest req) {
        Set<String> mcs = null;
        mcs = intersectCodes(mcs, codesFromCompField(req.getSearchCompField(), req.getSearchCompQ()));
        mcs = intersectCodes(mcs, codesFromRegNo(req.getSearchRegNo()));
        mcs = intersectCodes(mcs, codesFromCalcCycle(req.getSearchCycle()));
        mcs = intersectCodes(mcs, codesFromMid(req.getSearchMid()));

        String tf = req.getSearchTranFactor();
        String tv = req.getSearchTranValue();
        if (tf != null && !tf.isBlank() && tv != null && !tv.isBlank()) {
            String f = tf.trim().toUpperCase(Locale.ROOT);
            if ("MERCHANT".equals(f)) {
                mcs = intersectCodes(mcs, codesFromCompField("BOTH", tv));
            } else if ("MID".equals(f)) {
                mcs = intersectCodes(mcs, codesFromMid(tv.trim()));
            }
        }
        return mcs;
    }

    private static Set<String> intersectCodes(Set<String> current, Set<String> next) {
        if (next == null) {
            return current;
        }
        if (next.isEmpty()) {
            return Set.of();
        }
        if (current == null) {
            return new HashSet<>(next);
        }
        HashSet<String> h = new HashSet<>(current);
        h.retainAll(next);
        return h;
    }

    private Set<String> codesFromCompField(String field, String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String trimmed = q.trim();
        Set<String> set = new HashSet<>();
        String f = field == null ? "" : field.trim().toUpperCase(Locale.ROOT);
        if (f.isEmpty() || "BOTH".equals(f)) {
            orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, trimmed)
                    .forEach(o -> set.add(o.getCode()));
            orgUnitRepository.findByOrgLevelAndCodeContainingIgnoreCase(OrgLevel.MERCHANT, trimmed)
                    .forEach(o -> set.add(o.getCode()));
        } else if ("NM".equals(f)) {
            orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, trimmed)
                    .forEach(o -> set.add(o.getCode()));
        } else if ("CODE".equals(f)) {
            orgUnitRepository.findByOrgLevelAndCodeContainingIgnoreCase(OrgLevel.MERCHANT, trimmed)
                    .forEach(o -> set.add(o.getCode()));
        } else {
            return null;
        }
        return set.isEmpty() ? Set.of() : set;
    }

    private Set<String> codesFromRegNo(String regNo) {
        if (regNo == null || regNo.isBlank()) {
            return null;
        }
        List<Long> ids = merchantProfileRepository.findOrgUnitIdsByRegNoContainingIgnoreCase(regNo.trim());
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<String> codes = new HashSet<>();
        for (Long id : ids) {
            orgUnitRepository.findById(id).ifPresent(o -> codes.add(o.getCode()));
        }
        return codes.isEmpty() ? Set.of() : codes;
    }

    private Set<String> codesFromCalcCycle(String cycle) {
        if (cycle == null || cycle.isBlank()) {
            return null;
        }
        List<SettlementSetting> list = settlementSettingRepository.findByCalcCycle(cycle.trim());
        if (list.isEmpty()) {
            return Set.of();
        }
        Set<String> codes = new HashSet<>();
        for (SettlementSetting ss : list) {
            orgUnitRepository.findById(ss.getOrgUnitId()).ifPresent(o -> codes.add(o.getCode()));
        }
        return codes.isEmpty() ? Set.of() : codes;
    }

    private Set<String> codesFromMid(String mid) {
        if (mid == null || mid.isBlank()) {
            return null;
        }
        List<String> list = merchantPgBindingRepository.findMerchantCodesByMidContaining(mid.trim());
        if (list.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(list);
    }

    private void addPayDivPredicate(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                    jakarta.persistence.criteria.CriteriaBuilder cb, String cd) {
        if (cd == null || cd.isBlank()) {
            return;
        }
        String v = cd.trim();
        if ("FAIL".equalsIgnoreCase(v)) {
            parts.add(root.get("status").in("F0", "99"));
            return;
        }
        parts.add(cb.equal(root.get("status"), v));
    }

    private void addPayProcPredicate(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                       jakarta.persistence.criteria.CriteriaBuilder cb, String cd) {
        if (cd == null || cd.isBlank()) {
            return;
        }
        switch (cd.trim()) {
            case "10" -> parts.add(cb.and(
                    unsettled(root, cb),
                    cb.equal(root.get("status"), "10")
            ));
            case "20" -> parts.add(cb.equal(cb.upper(root.get("settledYn")), "Y"));
            case "30" -> parts.add(cb.equal(root.get("status"), "20"));
            case "40" -> parts.add(cb.and(
                    unsettled(root, cb),
                    root.get("status").in("30", "31", "40", "41", "42")
            ));
            default -> { }
        }
    }

    private static Predicate unsettled(jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                       jakarta.persistence.criteria.CriteriaBuilder cb) {
        return cb.or(
                cb.isNull(root.get("settledYn")),
                cb.notEqual(cb.upper(root.get("settledYn")), "Y")
        );
    }

    private void addTranFactorColumnPredicates(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                               jakarta.persistence.criteria.CriteriaBuilder cb,
                                               String factorRaw, String valueRaw) {
        if (valueRaw == null || valueRaw.isBlank() || factorRaw == null || factorRaw.isBlank()) {
            return;
        }
        String factor = factorRaw.trim().toUpperCase(Locale.ROOT);
        if ("MERCHANT".equals(factor) || "MID".equals(factor)) {
            return;
        }
        String v = valueRaw.trim();
        switch (factor) {
            case "ORDER_NO" -> addLike(parts, root, cb, "orderNo", v);
            case "CUSTOMER_ID" -> addLike(parts, root, cb, "customerId", v);
            case "TRN_ID" -> addLike(parts, root, cb, "trnId", v);
            case "ROUTE" -> addLike(parts, root, cb, "routeNo", v);
            case "AMT" -> {
                try {
                    String digits = v.replace(",", "").trim();
                    BigDecimal amt = new BigDecimal(digits);
                    parts.add(cb.equal(root.get("amtKrw"), amt));
                } catch (Exception ignored) {
                    parts.add(cb.disjunction());
                }
            }
            default -> { }
        }
    }

    private void addPgAgencyPredicate(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                      jakarta.persistence.criteria.CriteriaBuilder cb, String pgCdRaw) {
        if (pgCdRaw == null || pgCdRaw.isBlank()) {
            return;
        }
        String pgCd = pgCdRaw.trim();
        List<String> byBinding = merchantPgBindingRepository.findMerchantCodesByPgCd(pgCd);
        List<Predicate> ors = new ArrayList<>();
        ors.add(cb.equal(root.get("van"), pgCd));
        if (!byBinding.isEmpty()) {
            ors.add(root.get("merchantId").in(byBinding));
        }
        parts.add(cb.or(ors.toArray(Predicate[]::new)));
    }

    private void addKeywordPredicate(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                     jakarta.persistence.criteria.CriteriaBuilder cb, String kw) {
        if (kw == null || kw.isBlank()) {
            return;
        }
        String pat = "%" + kw.trim().toLowerCase(Locale.ROOT) + "%";
        List<Predicate> ors = new ArrayList<>();
        orFieldLike(ors, root, cb, "orderNo", pat);
        orFieldLike(ors, root, cb, "trnId", pat);
        orFieldLike(ors, root, cb, "customerId", pat);
        orFieldLike(ors, root, cb, "chillTransactionId", pat);
        orFieldLike(ors, root, cb, "payNo", pat);
        if (!ors.isEmpty()) {
            parts.add(cb.or(ors.toArray(Predicate[]::new)));
        }
    }

    private static void orFieldLike(List<Predicate> ors, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                    jakarta.persistence.criteria.CriteriaBuilder cb, String field, String pattern) {
        jakarta.persistence.criteria.Path<String> path = root.get(field);
        ors.add(cb.and(cb.isNotNull(path), cb.like(cb.lower(path), pattern)));
    }

    private static void addLike(List<Predicate> parts, jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                jakarta.persistence.criteria.CriteriaBuilder cb, String field, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String pat = "%" + raw.trim().toLowerCase(Locale.ROOT) + "%";
        jakarta.persistence.criteria.Path<String> path = root.get(field);
        parts.add(cb.and(cb.isNotNull(path), cb.like(cb.lower(path), pat)));
    }

    private Predicate variantPredicate(jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                       jakarta.persistence.criteria.CriteriaBuilder cb, String variant) {
        return switch (variant) {
            case "INTEGRATED" -> cb.conjunction();
            case "SUCCESS" -> cb.equal(root.get("status"), "10");
            case "FAIL" -> root.get("status").in("F0", "99");
            case "REFUND" -> cb.equal(root.get("status"), "30");
            case "FORCE_REFUND" -> cb.equal(root.get("status"), "31");
            case "CANCEL" -> cb.equal(root.get("status"), "20");
            case "OFFSET_CANCEL" -> cb.or(cb.isNull(root.get("status")), cb.notEqual(root.get("status"), "10"));
            case "URL_PAY" -> cb.equal(root.get("origin"), "URL");
            case "CHATBOT_PAY" -> cb.equal(root.get("origin"), "CHATBOT");
            case "NOTI" -> cb.equal(root.get("origin"), "NOTI");
            default -> cb.conjunction();
        };
    }
}
