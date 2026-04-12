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
import jakarta.persistence.criteria.Path;
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
import java.time.LocalDate;
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
    private final PayFollowPolicyService payFollowPolicyService;
    private final OrgAccessService orgAccessService;

    @PersistenceContext
    private EntityManager entityManager;

    public PayListService(PgTrnsctnRepository trnsctnRepository,
                          OrgUnitRepository orgUnitRepository,
                          MerchantProfileRepository merchantProfileRepository,
                          MerchantPgBindingRepository merchantPgBindingRepository,
                          DistributionFeeConfigRepository distributionFeeConfigRepository,
                          CommissionPolicyRepository commissionPolicyRepository,
                          SettlementSettingRepository settlementSettingRepository,
                          HqNotifyMappingService hqNotifyMappingService,
                          PayFollowPolicyService payFollowPolicyService,
                          OrgAccessService orgAccessService) {
        this.trnsctnRepository = trnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.hqNotifyMappingService = hqNotifyMappingService;
        this.payFollowPolicyService = payFollowPolicyService;
        this.orgAccessService = orgAccessService;
    }

    public PageResult<Map<String, Object>> search(PayListSearchRequest req, Authentication authentication) {
        if (req == null) {
            req = new PayListSearchRequest();
        }
        applyDefaultPayListSearchDates(req);
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        int page = Math.max(1, req.getPage());
        int size = Math.min(1000, Math.max(1, req.getSize()));
        Pageable p = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to, authentication);
        Page<PgTrnsctn> result = trnsctnRepository.findAll(spec, p);
        List<String> merchantCodes = result.getContent().stream().map(PgTrnsctn::getMerchantId).distinct().collect(Collectors.toList());
        Map<String, PayListRowContext> ctxByCode = buildPayListRowContextMap(merchantCodes);

        HqNotifyMappingService.DisplayTransformCache displayCache = hqNotifyMappingService.loadDisplayTransformCache();
        AppUser payListViewer = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        List<Map<String, Object>> list = new ArrayList<>();
        for (PgTrnsctn t : result.getContent()) {
            PayListRowContext ctx = ctxByCode.get(t.getMerchantId());
            Map<String, Object> row = PayListItemDto.from(t, ctx);
            String pgCd = resolvePgCdForPayListRow(ctx, t);
            hqNotifyMappingService.applyDisplayTransform(displayCache, pgCd, row);
            row.put("payFollowRow", payFollowPolicyService.payFollowRowEnabled(payListViewer, t));
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
            meta.put("payFollowAllowed", payFollowPolicyService.allowedActionsForViewer(payListViewer));
            pr.setMeta(meta);
        } catch (RuntimeException ignored) {
            /* 집계 실패 시 목록만 반환 */
        }
        return pr;
    }

    /** 결제관리 payList: 기간 미입력 시 당일(서버 일자)로 조회·집계 */
    private static void applyDefaultPayListSearchDates(PayListSearchRequest req) {
        if (req.getSearchFromDate() != null || req.getSearchToDate() != null) {
            return;
        }
        LocalDate today = LocalDate.now();
        req.setSearchFromDate(today);
        req.setSearchToDate(today);
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
     * 가맹정산내역 등 단일 가맹점 코드에 대한 결제내역과 동일한 {@link PayListRowContext}(수수료·보류·상위조직명).
     */
    public PayListRowContext buildPayListRowContextForMerchant(String merchantCode) {
        if (merchantCode == null || merchantCode.isBlank()) {
            return null;
        }
        return buildPayListRowContextMap(List.of(merchantCode.trim())).get(merchantCode.trim());
    }

    /**
     * 동일 검색 조건·조직 권한 범위 전체 건 기준 금액 요약(페이지·정렬과 무관). 건수=승인 건수, 금액은 통화별.
     */
    private Map<String, Object> computePayListFinancialSummary(PayListSearchRequest req, Authentication authentication) {
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to, authentication);
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
        boolean baseCurrencyConfigured = isViewerBaseCurrencyConfigured(user);
        final List<String> currencyOrder;
        if (baseCurrencyConfigured) {
            currencyOrder = resolveViewerDisplayCurrencyOrder(user, multi);
        } else {
            currencyOrder = new ArrayList<>(); /* 집계 후 실제 통화 키로 채움 */
        }
        Set<String> allowedCur = baseCurrencyConfigured ? new HashSet<>(currencyOrder) : null;
        boolean effectiveMultiCurrency = multi || !baseCurrencyConfigured;

        Map<String, BigDecimal> approve = new HashMap<>();
        Map<String, BigDecimal> cancel = new HashMap<>();
        Map<String, Long> approveCountByCur = new HashMap<>();
        Map<String, Long> cancelCountByCur = new HashMap<>();
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
            if (allowedCur != null && !allowedCur.contains(cur)) {
                continue;
            }
            PayListRowContext ctx = mid != null ? ctxMap.get(mid.trim()) : null;
            if ("10".equals(st)) {
                successCount++;
                approveCountByCur.merge(cur, 1L, Long::sum);
                approve.merge(cur, amt, BigDecimal::add);
                PayListItemDto.ApprovedSettlementParts p = PayListItemDto.approvedSettlementParts(amt, ctx);
                feeVatSum.merge(cur, p.feeAmt.add(p.feeVat), BigDecimal::add);
                hold.merge(cur, p.holdAmt, BigDecimal::add);
                payout.merge(cur, p.settleAmt, BigDecimal::add);
            } else if (PayListItemDto.isCancelAmountStatus(st)) {
                cancelCountByCur.merge(cur, 1L, Long::sum);
                cancel.merge(cur, amt, BigDecimal::add);
            }
        }

        Map<String, String> approvePlain = new LinkedHashMap<>();
        Map<String, Long> approveCountPlain = new LinkedHashMap<>();
        Map<String, String> cancelPlain = new LinkedHashMap<>();
        Map<String, Long> cancelCountPlain = new LinkedHashMap<>();
        Map<String, String> paymentPlain = new LinkedHashMap<>();
        Map<String, String> feePlain = new LinkedHashMap<>();
        Map<String, String> holdPlain = new LinkedHashMap<>();
        Map<String, String> payoutPlain = new LinkedHashMap<>();
        if (!baseCurrencyConfigured) {
            Set<String> union = new HashSet<>();
            union.addAll(approve.keySet());
            union.addAll(cancel.keySet());
            union.addAll(feeVatSum.keySet());
            union.addAll(hold.keySet());
            union.addAll(payout.keySet());
            currencyOrder.clear();
            currencyOrder.addAll(union);
            PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
        }
        if (currencyOrder.isEmpty()) {
            currencyOrder.add(primaryNorm);
        }
        for (String c : currencyOrder) {
            BigDecimal a = approve.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal k = cancel.getOrDefault(c, BigDecimal.ZERO);
            approvePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a));
            approveCountPlain.put(c, approveCountByCur.getOrDefault(c, 0L));
            cancelPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(k));
            cancelCountPlain.put(c, cancelCountByCur.getOrDefault(c, 0L));
            paymentPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a.subtract(k)));
            feePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(feeVatSum.getOrDefault(c, BigDecimal.ZERO)));
            holdPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(hold.getOrDefault(c, BigDecimal.ZERO)));
            payoutPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(payout.getOrDefault(c, BigDecimal.ZERO)));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("successCount", successCount);
        out.put("multiCurrency", effectiveMultiCurrency);
        out.put("primaryCurrency", primaryNorm);
        out.put("currencyOrder", new ArrayList<>(currencyOrder));
        out.put("approveByCurrency", approvePlain);
        out.put("approveCountByCurrency", approveCountPlain);
        out.put("cancelByCurrency", cancelPlain);
        out.put("cancelCountByCurrency", cancelCountPlain);
        out.put("paymentByCurrency", paymentPlain);
        out.put("feeByCurrency", feePlain);
        out.put("holdByCurrency", holdPlain);
        out.put("payoutByCurrency", payoutPlain);
        return out;
    }

    /**
     * ChillPay 통합·정산 API 행 목록에 대해 {@link #computePayListFinancialSummary} 와 동일 키의 금액 요약을 만듭니다.
     * 승인은 Chill {@link PayListStatusBarBuckets#bucketForChillStatus} 의 SUCCESS, 취소 계열은 CANCEL·REFUND·VOID.
     * 총수수료는 API {@code fee} 합, 보류는 0, 지급액은 {@code settled} 가 양수면 그 값·아니면 {@code amount − fee}(음수는 0).
     */
    public Map<String, Object> buildChillPayFinancialSummary(List<Map<String, Object>> rows,
                                                             Authentication authentication) {
        List<Map<String, Object>> list = rows != null ? rows : List.of();
        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        OrgLevel level = PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository);
        boolean multi = PayListStatusBarBuckets.isMultiCurrencyViewer(level);
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository);
        String primaryNorm = PayListStatusBarBuckets.normalizeCurrency(primary);
        boolean baseCurrencyConfigured = isViewerBaseCurrencyConfigured(user);
        final List<String> currencyOrder;
        if (baseCurrencyConfigured) {
            currencyOrder = resolveViewerDisplayCurrencyOrder(user, multi);
        } else {
            currencyOrder = new ArrayList<>();
        }
        Set<String> allowedCur = baseCurrencyConfigured ? new HashSet<>(currencyOrder) : null;
        boolean effectiveMultiCurrency = multi || !baseCurrencyConfigured;

        Map<String, BigDecimal> approve = new HashMap<>();
        Map<String, BigDecimal> cancel = new HashMap<>();
        Map<String, Long> approveCountByCur = new HashMap<>();
        Map<String, Long> cancelCountByCur = new HashMap<>();
        Map<String, BigDecimal> feeVatSum = new HashMap<>();
        Map<String, BigDecimal> hold = new HashMap<>();
        Map<String, BigDecimal> payout = new HashMap<>();
        long successCount = 0;

        for (Map<String, Object> row : list) {
            String st = chillPayRowFirstString(row, "status", "Status");
            String bucket = PayListStatusBarBuckets.bucketForChillStatus(st);
            String cur = PayListStatusBarBuckets.normalizeCurrency(chillPayRowFirstString(row, "currency", "Currency"));
            if (allowedCur != null && !allowedCur.contains(cur)) {
                continue;
            }
            BigDecimal amt = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "amount", "Amount"));
            if (PayListStatusBarBuckets.SUCCESS.equals(bucket)) {
                successCount++;
                approveCountByCur.merge(cur, 1L, Long::sum);
                approve.merge(cur, amt, BigDecimal::add);
                BigDecimal fee = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "fee", "Fee"));
                feeVatSum.merge(cur, fee, BigDecimal::add);
                BigDecimal settled = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "settled", "Settled"));
                if (settled.compareTo(BigDecimal.ZERO) > 0) {
                    payout.merge(cur, settled, BigDecimal::add);
                } else {
                    BigDecimal net = amt.subtract(fee);
                    if (net.compareTo(BigDecimal.ZERO) < 0) {
                        net = BigDecimal.ZERO;
                    }
                    payout.merge(cur, net, BigDecimal::add);
                }
            } else if (isChillCancelFinancialBucket(bucket)) {
                cancelCountByCur.merge(cur, 1L, Long::sum);
                cancel.merge(cur, amt, BigDecimal::add);
            }
        }

        Map<String, String> approvePlain = new LinkedHashMap<>();
        Map<String, Long> approveCountPlain = new LinkedHashMap<>();
        Map<String, String> cancelPlain = new LinkedHashMap<>();
        Map<String, Long> cancelCountPlain = new LinkedHashMap<>();
        Map<String, String> paymentPlain = new LinkedHashMap<>();
        Map<String, String> feePlain = new LinkedHashMap<>();
        Map<String, String> holdPlain = new LinkedHashMap<>();
        Map<String, String> payoutPlain = new LinkedHashMap<>();
        if (!baseCurrencyConfigured) {
            Set<String> union = new HashSet<>();
            union.addAll(approve.keySet());
            union.addAll(cancel.keySet());
            union.addAll(feeVatSum.keySet());
            union.addAll(hold.keySet());
            union.addAll(payout.keySet());
            currencyOrder.clear();
            currencyOrder.addAll(union);
            PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
        }
        if (currencyOrder.isEmpty()) {
            currencyOrder.add(primaryNorm);
        }
        for (String c : currencyOrder) {
            BigDecimal a = approve.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal k = cancel.getOrDefault(c, BigDecimal.ZERO);
            approvePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a));
            approveCountPlain.put(c, approveCountByCur.getOrDefault(c, 0L));
            cancelPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(k));
            cancelCountPlain.put(c, cancelCountByCur.getOrDefault(c, 0L));
            paymentPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a.subtract(k)));
            feePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(feeVatSum.getOrDefault(c, BigDecimal.ZERO)));
            holdPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(hold.getOrDefault(c, BigDecimal.ZERO)));
            payoutPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(payout.getOrDefault(c, BigDecimal.ZERO)));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("successCount", successCount);
        out.put("multiCurrency", effectiveMultiCurrency);
        out.put("primaryCurrency", primaryNorm);
        out.put("currencyOrder", new ArrayList<>(currencyOrder));
        out.put("approveByCurrency", approvePlain);
        out.put("approveCountByCurrency", approveCountPlain);
        out.put("cancelByCurrency", cancelPlain);
        out.put("cancelCountByCurrency", cancelCountPlain);
        out.put("paymentByCurrency", paymentPlain);
        out.put("feeByCurrency", feePlain);
        out.put("holdByCurrency", holdPlain);
        out.put("payoutByCurrency", payoutPlain);
        return out;
    }

    private static boolean isChillCancelFinancialBucket(String bucket) {
        return PayListStatusBarBuckets.CANCEL.equals(bucket)
                || PayListStatusBarBuckets.REFUND.equals(bucket)
                || PayListStatusBarBuckets.VOID.equals(bucket);
    }

    private static String chillPayRowFirstString(Map<String, Object> row, String... keys) {
        if (row == null) {
            return "";
        }
        for (String k : keys) {
            Object v = row.get(k);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return "";
    }

    private static Object chillPayRowFirstObject(Map<String, Object> row, String k1, String k2) {
        if (row == null) {
            return null;
        }
        if (row.containsKey(k1) && row.get(k1) != null) {
            return row.get(k1);
        }
        if (row.containsKey(k2) && row.get(k2) != null) {
            return row.get(k2);
        }
        return null;
    }

    /**
     * 로그인 사용자 소속 조직 {@link MerchantProfile#getBaseCurrency()} 순서.
     * CSV가 비어 있으면 호출부에서 집계 데이터의 통화 키로 대체한다.
     */
    private List<String> resolveViewerDisplayCurrencyOrder(AppUser user, boolean multiCurrencyViewer) {
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository);
        if (user == null || user.getOrgUnitCode() == null || user.getOrgUnitCode().isBlank()) {
            return PayListStatusBarBuckets.resolveDisplayCurrencyOrder(multiCurrencyViewer, "", primary);
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(user.getOrgUnitCode().trim());
        if (ou.isEmpty()) {
            return PayListStatusBarBuckets.resolveDisplayCurrencyOrder(multiCurrencyViewer, "", primary);
        }
        String bc = merchantProfileRepository.findByOrgUnitId(ou.get().getId())
                .map(MerchantProfile::getBaseCurrency)
                .orElse("");
        return PayListStatusBarBuckets.resolveDisplayCurrencyOrder(multiCurrencyViewer, bc, primary);
    }

    /** 소속 조직 프로필에 기준통화(baseCurrency)가 한 글자라도 설정된 경우만 true */
    private boolean isViewerBaseCurrencyConfigured(AppUser user) {
        if (user == null || user.getOrgUnitCode() == null || user.getOrgUnitCode().isBlank()) {
            return false;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(user.getOrgUnitCode().trim());
        if (ou.isEmpty()) {
            return false;
        }
        String bc = merchantProfileRepository.findByOrgUnitId(ou.get().getId())
                .map(MerchantProfile::getBaseCurrency)
                .orElse("");
        return bc != null && !bc.isBlank();
    }

    /**
     * 동일 검색 조건 전체 건 기준 상태·통화별 합계(페이지와 무관).
     */
    private Map<String, Object> computePgTxnStatusBar(PayListSearchRequest req, Authentication authentication) {
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to, authentication);
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
                .when(cb.equal(st, "30"), cb.literal(PayListStatusBarBuckets.REFUND))
                .when(cb.equal(st, "31"), cb.literal(PayListStatusBarBuckets.FORCE_REFUND))
                .when(cb.equal(st, "20"), cb.literal(PayListStatusBarBuckets.CANCEL))
                .otherwise(cb.literal(PayListStatusBarBuckets.OTHER));
        cq.multiselect(bucket, curExpr, cb.count(root),
                cb.sum(cb.coalesce(root.get("amtKrw"), cb.literal(BigDecimal.ZERO))));
        cq.where(spec.toPredicate(root, cq, cb));
        cq.groupBy(bucket, curExpr);
        List<Tuple> tuples = entityManager.createQuery(cq).getResultList();
        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        OrgLevel level = PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository);
        boolean multi = PayListStatusBarBuckets.isMultiCurrencyViewer(level);
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository);
        boolean baseCurrencyConfigured = isViewerBaseCurrencyConfigured(user);
        List<String> displayOrder = baseCurrencyConfigured ? resolveViewerDisplayCurrencyOrder(user, multi) : null;
        Set<String> allowedCur = displayOrder != null ? new HashSet<>(displayOrder) : null;
        boolean effectiveMultiCurrency = multi || !baseCurrencyConfigured;
        PayListStatusBarBuckets.MutableRollup roll = new PayListStatusBarBuckets.MutableRollup();
        for (Tuple t : tuples) {
            String b = t.get(0, String.class);
            String cRaw = t.get(1, String.class);
            String c = PayListStatusBarBuckets.normalizeCurrency(cRaw);
            if (allowedCur != null && !allowedCur.contains(c)) {
                continue;
            }
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
        String variant = req.getPayListVariant() == null || req.getPayListVariant().isBlank()
                ? "INTEGRATED" : req.getPayListVariant().trim().toUpperCase(Locale.ROOT);
        if ("INTEGRATED".equals(variant) || "NOTI".equals(variant)
                || "URL_PAY".equals(variant) || "CHATBOT_PAY".equals(variant)
                || "OFFSET_CANCEL".equals(variant)) {
            roll.mergeBucketInto(PayListStatusBarBuckets.FORCE_REFUND, PayListStatusBarBuckets.REFUND);
        }
        List<String> visibleBuckets = visiblePayListStatusBarBucketsForVariant(variant);
        return roll.toPayload(effectiveMultiCurrency, primary, false, true, displayOrder, visibleBuckets);
    }

    /** 화면별로 상태바에 노출할 버킷만(0건도 슬롯 표시). 강제환불은 FORCE_REFUND 화면만. */
    private static List<String> visiblePayListStatusBarBucketsForVariant(String variant) {
        if (variant == null || variant.isBlank()) {
            return List.of(
                    PayListStatusBarBuckets.SUCCESS,
                    PayListStatusBarBuckets.FAIL,
                    PayListStatusBarBuckets.VOID,
                    PayListStatusBarBuckets.REFUND,
                    PayListStatusBarBuckets.CANCEL,
                    PayListStatusBarBuckets.OTHER);
        }
        return switch (variant.trim().toUpperCase(Locale.ROOT)) {
            case "SUCCESS" -> List.of(PayListStatusBarBuckets.SUCCESS);
            case "FAIL" -> List.of(PayListStatusBarBuckets.FAIL);
            case "VOID" -> List.of(PayListStatusBarBuckets.VOID);
            case "REFUND" -> List.of(PayListStatusBarBuckets.REFUND);
            case "FORCE_REFUND" -> List.of(PayListStatusBarBuckets.FORCE_REFUND);
            case "CANCEL" -> List.of(PayListStatusBarBuckets.CANCEL);
            default -> List.of(
                    PayListStatusBarBuckets.SUCCESS,
                    PayListStatusBarBuckets.FAIL,
                    PayListStatusBarBuckets.VOID,
                    PayListStatusBarBuckets.REFUND,
                    PayListStatusBarBuckets.CANCEL,
                    PayListStatusBarBuckets.OTHER);
        };
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

    private Specification<PgTrnsctn> buildSpecification(PayListSearchRequest req, LocalDateTime fromDt, LocalDateTime toDt,
                                                        Authentication authentication) {
        Set<String> merchantCodes = resolveMerchantFilterCodes(req, authentication);
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
            addNotifyChannelPredicate(parts, root, cb, variant, req);
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
    private Set<String> resolveMerchantFilterCodes(PayListSearchRequest req, Authentication authentication) {
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
        mcs = intersectCodes(mcs, ownMerchantOnlyForPayListVariant(req, authentication));
        mcs = intersectCodes(mcs, orgAccessService.visibleMerchantCompCodes(authentication));
        return mcs;
    }

    /**
     * URL·챗봇·상계취소 화면: 가맹점(MERCHANT) 로그인은 본인 업체 코드만 조회.
     */
    private Set<String> ownMerchantOnlyForPayListVariant(PayListSearchRequest req, Authentication authentication) {
        String raw = req.getPayListVariant();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (!"URL_PAY".equals(v) && !"CHATBOT_PAY".equals(v) && !"OFFSET_CANCEL".equals(v)) {
            return null;
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return null;
        }
        String code = user.getOrgUnitCode();
        if (code == null || code.isBlank()) {
            return null;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(code.trim());
        if (ou.isEmpty() || ou.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return null;
        }
        return Set.of(ou.get().getCode());
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

    /**
     * 통합·노티 결제내역: NOTI 행만 수신 채널로 제한.
     * 파라미터 없음·ALL = 필터 없음(전체). CALLBACK = CALL·CALLBACK·RETURN·공백.
     * CHILL·URL 등 비-NOTI 행은 항상 포함.
     */
    private void addNotifyChannelPredicate(List<Predicate> parts, Root<PgTrnsctn> root, CriteriaBuilder cb,
                                           String variant, PayListSearchRequest req) {
        if (!"INTEGRATED".equals(variant) && !"NOTI".equals(variant)) {
            return;
        }
        String raw = req.getSearchNotifyChannel();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String mode = raw.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(mode)) {
            return;
        }
        Path<String> origin = root.get("origin");
        Path<String> chPath = root.get("notifyChannelType");
        Expression<String> chNorm = cb.upper(cb.trim(cb.coalesce(chPath, cb.literal(""))));

        Predicate notNoti = cb.or(cb.isNull(origin), cb.notEqual(origin, "NOTI"));

        Predicate notiMatch;
        switch (mode) {
            case "CALLBACK" -> {
                Predicate blankCh = cb.equal(chNorm, "");
                Predicate callFamily = cb.or(
                        cb.equal(chNorm, "CALLBACK"),
                        cb.equal(chNorm, "CALL"),
                        cb.equal(chNorm, "RETURN"));
                notiMatch = cb.and(cb.equal(origin, "NOTI"), cb.or(blankCh, callFamily));
            }
            case "RESULT" -> notiMatch = cb.and(cb.equal(origin, "NOTI"), cb.equal(chNorm, "RESULT"));
            case "BOTH" -> notiMatch = cb.and(cb.equal(origin, "NOTI"), cb.equal(chNorm, "BOTH"));
            default -> {
                return;
            }
        }
        parts.add(cb.or(notNoti, notiMatch));
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
            case "VOID" -> root.get("status").in("21", "22", "40", "41", "42");
            case "OFFSET_CANCEL" -> cb.or(cb.isNull(root.get("status")), cb.notEqual(root.get("status"), "10"));
            case "URL_PAY" -> cb.equal(root.get("origin"), "URL");
            case "CHATBOT_PAY" -> cb.equal(root.get("origin"), "CHATBOT");
            case "NOTI" -> cb.equal(root.get("origin"), "NOTI");
            default -> cb.conjunction();
        };
    }
}
