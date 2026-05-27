package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.api.dto.PayListItemDto;
import com.pg.api.dto.PayListRowContext;
import com.pg.api.dto.PayListSearchRequest;
import com.pg.api.dto.TxnDualLineSpec;
import com.pg.entity.AppUser;
import com.pg.entity.ChargebackFeeTier;
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
import com.pg.service.settlement.FeeListTxnAmountService;
import com.pg.service.settlement.SettlementBusinessHolidayService;
import com.pg.service.settlement.SettlementExpectedDateResolver;
import com.pg.entity.CommissionPolicy;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final MasterDistSettlementCronZoneService masterDistSettlementCronZoneService;
    private final SettlementBusinessHolidayService settlementBusinessHolidayService;
    private final CommissionService commissionService;
    private final FeeListTxnAmountService feeListTxnAmountService;

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
                          OrgAccessService orgAccessService,
                          HqLedgerSysSettingsService hqLedgerSysSettingsService,
                          MasterDistSettlementCronZoneService masterDistSettlementCronZoneService,
                          SettlementBusinessHolidayService settlementBusinessHolidayService,
                          CommissionService commissionService,
                          FeeListTxnAmountService feeListTxnAmountService) {
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
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.masterDistSettlementCronZoneService = masterDistSettlementCronZoneService;
        this.settlementBusinessHolidayService = settlementBusinessHolidayService;
        this.commissionService = commissionService;
        this.feeListTxnAmountService = feeListTxnAmountService;
    }

    /**
     * 통합 리포트 상세 — 수수료내역과 동일 건별 총수수료·부가세·정산액을 payList 행에 덮어씁니다.
     */
    private void applyFeeListAmountsToPayRow(Map<String, Object> row,
                                             PgTrnsctn t,
                                             PayListRowContext ctx,
                                             FeeCurrencyRoundResolver feeResolver,
                                             Map<String, CommissionPolicy> polCache,
                                             Map<String, Long> monthCbCountCache,
                                             Map<Long, List<ChargebackFeeTier>> tiersByPolicyId) {
        if (row == null || t == null || t.getMerchantId() == null || t.getMerchantId().isBlank()) {
            return;
        }
        String compId = t.getMerchantId().trim();
        CommissionPolicy pol = polCache.computeIfAbsent(compId,
                id -> commissionService.resolveCommissionPolicyForSettlement(id));
        String payCurKey = PayListItemDto.payCurKeyForFeeCompute(t, ctx);
        FeeListTxnAmountService.FeeListTxnAmounts amts = feeListTxnAmountService.compute(
                t, ctx, pol, payCurKey, feeResolver, monthCbCountCache, tiersByPolicyId);
        row.put("feeAmt", amts.totalFee());
        row.put("feeVat", amts.feeVat());
        row.put("settleAmt", amts.settlementAmt());
        row.put("settlementAmt", amts.settlementAmt());
        row.put("totalFee", amts.totalFee());
        row.put("expectedPayout", amts.expectedPayout());
        row.put("holdAmt", amts.rollingHoldEst());
        row.put("rollingHoldEst", amts.rollingHoldEst());
    }

    private void enrichPayListSettlementExpectedDate(Map<String, Object> row,
                                                     PgTrnsctn t,
                                                     PayListRowContext ctx,
                                                     Map<Long, Set<LocalDate>> holidayCache) {
        if (row == null || t == null || ctx == null || ctx.getSettlement() == null) {
            if (row != null) {
                row.put("expectedSettleDate", "—");
            }
            return;
        }
        String cycle = ctx.getSettlement().getCalcCycle();
        if (cycle == null || cycle.isBlank()) {
            row.put("expectedSettleDate", "—");
            return;
        }
        LocalDate trnDate = t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : null;
        Object trnDateObj = row.get("trnDate");
        if (trnDateObj != null && !String.valueOf(trnDateObj).isBlank()) {
            try {
                trnDate = LocalDate.parse(String.valueOf(trnDateObj).trim());
            } catch (Exception ignored) {
                /* createdAt fallback */
            }
        }
        if (trnDate == null) {
            row.put("expectedSettleDate", "—");
            return;
        }
        Set<LocalDate> hol = Set.of();
        if (ctx.getProfile() != null && ctx.getProfile().getOrgUnitId() != null && holidayCache != null) {
            long orgUnitId = ctx.getProfile().getOrgUnitId();
            hol = holidayCache.computeIfAbsent(orgUnitId,
                    settlementBusinessHolidayService::resolveNonBusinessDatesForMerchantOrgUnitId);
        }
        String expected = SettlementExpectedDateResolver.formatExpectedSettlementDate(
                trnDate, t.getCreatedAt(), cycle.trim(), hol);
        row.put("expectedSettleDate", expected.isEmpty() ? "—" : expected);
    }

    /** 결제 목록 meta: 전산설정 기준 결제 통화(ISO 숫자·알파) — UI 폴백·표시 연동 */
    public void putHqLedgerPayDisplayCurrencyMeta(Map<String, Object> meta) {
        if (meta == null) {
            return;
        }
        var ls = hqLedgerSysSettingsService.getOrCreate();
        String num = PayDisplayCurrency.normalizeIsoNum(ls.getPayDisplayCurrencyIsoNum());
        meta.put("hqPayDisplayCurrencyIsoNum", num);
        meta.put("hqPayDisplayCurrencyCode", PayDisplayCurrency.alphaFromIsoNum(num));
    }

    private String hqLedgerPayDisplayCurrencyAlpha() {
        return PayDisplayCurrency.alphaFromSettings(hqLedgerSysSettingsService.getOrCreate());
    }

    private static String resolvePayListJpaSortProperty(String raw) {
        if (raw == null || raw.isBlank()) {
            return "createdAt";
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "createdat", "created_at" -> "createdAt";
            case "paidat", "paid_at" -> "paidAt";
            case "trnid", "trn_id" -> "trnId";
            case "amtkrw", "amt_krw" -> "amtKrw";
            case "merchantid", "merchant_id" -> "merchantId";
            case "orderno", "order_no" -> "orderNo";
            case "status" -> "status";
            case "curtype", "cur_type" -> "curType";
            default -> "createdAt";
        };
    }

    private static Pageable payListPageable(int page, int size, PayListSearchRequest req) {
        Sort.Direction dir = req.getSearchOrderDir() != null && "ASC".equalsIgnoreCase(req.getSearchOrderDir().trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String prop = resolvePayListJpaSortProperty(req.getSearchOrderBy());
        return PageRequest.of(page - 1, size, Sort.by(dir, prop).and(Sort.by(dir, "trnId")));
    }

    public PageResult<Map<String, Object>> search(PayListSearchRequest req, Authentication authentication) {
        if (req == null) {
            req = new PayListSearchRequest();
        }
        applyDefaultPayListSearchDates(req);
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        int page = Math.max(1, req.getPage());
        int maxSize = req.isListExport() ? 15_000 : 1_000;
        int size = Math.min(maxSize, Math.max(1, req.getSize()));
        Pageable p = payListPageable(page, size, req);
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to, authentication);
        Page<PgTrnsctn> result = trnsctnRepository.findAll(spec, p);
        List<String> merchantCodes = result.getContent().stream().map(PgTrnsctn::getMerchantId).distinct().collect(Collectors.toList());
        Map<String, PayListRowContext> ctxByCode = buildPayListRowContextMap(merchantCodes);

        HqNotifyMappingService.DisplayTransformCache displayCache = hqNotifyMappingService.loadDisplayTransformCache();
        AppUser payListViewer = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        List<Map<String, Object>> list = new ArrayList<>();
        Map<Long, Set<LocalDate>> holidayCache = new HashMap<>();
        int rowNoStart = (page - 1) * size + 1;
        int rowIdx = 0;
        String payListVariant = req.getPayListVariant() != null && !req.getPayListVariant().isBlank()
                ? req.getPayListVariant().trim().toUpperCase(Locale.ROOT) : "";
        boolean integratedFeeListAligned = "INTEGRATED".equals(payListVariant);
        FeeCurrencyRoundResolver integratedFeeResolver = integratedFeeListAligned
                ? FeeCurrencyRoundResolver.from(hqLedgerSysSettingsService.getOrCreate()) : null;
        Map<String, CommissionPolicy> integratedPolCache = integratedFeeListAligned ? new HashMap<>() : null;
        Map<String, Long> integratedMonthCbCache = integratedFeeListAligned ? new HashMap<>() : null;
        Map<Long, List<ChargebackFeeTier>> integratedTiersByPolicyId = integratedFeeListAligned ? new HashMap<>() : null;
        for (PgTrnsctn t : result.getContent()) {
            PayListRowContext ctx = ctxByCode.get(t.getMerchantId());
            Map<String, Object> row = PayListItemDto.from(t, ctx, ledgerTz);
            if (integratedFeeListAligned && integratedFeeResolver != null) {
                applyFeeListAmountsToPayRow(row, t, ctx, integratedFeeResolver, integratedPolCache,
                        integratedMonthCbCache, integratedTiersByPolicyId);
            }
            enrichPayListSettlementExpectedDate(row, t, ctx, holidayCache);
            String pgCd = resolvePgCdForPayListRow(ctx, t);
            hqNotifyMappingService.applyDisplayTransform(displayCache, pgCd, row);
            row.put("payFollowRow", payFollowPolicyService.payFollowRowEnabled(payListViewer, t));
            row.put("rowNo", rowNoStart + rowIdx);
            rowIdx++;
            list.add(row);
        }
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(result.getNumber() + 1);
        pr.setSize(result.getSize());
        pr.setTotalElements(result.getTotalElements());
        pr.setTotalPages(result.getTotalPages());
        if (!req.isSkipMeta()) {
            try {
                Map<String, Object> bar = computePgTxnStatusBar(req, authentication);
                Map<String, Object> fin = computePayListFinancialSummary(req, authentication);
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("payListStatusBar", bar);
                if (fin != null) {
                    meta.put("payListFinancialSummary", fin);
                    if (Boolean.TRUE.equals(fin.get("capped"))) {
                        meta.put("payListFinancialCapped", true);
                        meta.put("payListFinancialCapNote",
                                "집계 대상 건수가 많아 일부만 반영되었을 수 있습니다. 기간을 줄이거나 목록에서 상세 조회하세요.");
                    }
                }
                meta.put("payFollowAllowed", payFollowPolicyService.allowedActionsForViewer(payListViewer));
                putHqLedgerPayDisplayCurrencyMeta(meta);
                pr.setMeta(meta);
            } catch (RuntimeException ignored) {
                /* 집계 실패 시 목록만 반환 */
            }
        }
        return pr;
    }

    /**
     * 정산 집계와 동일한 기간 창으로 {@link PgTrnsctnRepository#findForSettlement} 거래를 읽어
     * 결제내역 그리드와 동일한 행 맵을 구성한다(노티 매핑 display 변환 포함).
     */
    public static final class SettlementWindowPayRows {
        private final List<Map<String, Object>> rows;
        private final boolean truncated;

        public SettlementWindowPayRows(List<Map<String, Object>> rows, boolean truncated) {
            this.rows = rows;
            this.truncated = truncated;
        }

        public List<Map<String, Object>> getRows() {
            return rows;
        }

        public boolean isTruncated() {
            return truncated;
        }
    }

    public SettlementWindowPayRows listRowsForSettlementWindow(String merchantId,
                                                               LocalDateTime fromDt,
                                                               LocalDateTime toDt,
                                                               int maxRows) {
        if (merchantId == null || merchantId.isBlank()) {
            return new SettlementWindowPayRows(List.of(), false);
        }
        int cap = Math.max(1, Math.min(maxRows, 5000));
        List<PgTrnsctn> full = trnsctnRepository.findForSettlement(merchantId.trim(), fromDt, toDt);
        boolean truncated = full.size() > cap;
        List<PgTrnsctn> txs = truncated ? new ArrayList<>(full.subList(0, cap)) : full;
        List<String> codes = txs.stream().map(PgTrnsctn::getMerchantId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, PayListRowContext> ctxByCode = buildPayListRowContextMap(codes);
        HqNotifyMappingService.DisplayTransformCache displayCache = hqNotifyMappingService.loadDisplayTransformCache();
        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        List<Map<String, Object>> list = new ArrayList<>(txs.size());
        for (PgTrnsctn t : txs) {
            PayListRowContext base = ctxByCode.get(t.getMerchantId());
            PayListRowContext ctx = base != null ? base.withOmitSettlementFeeFromApprovedTxnBreakdown(true) : null;
            Map<String, Object> row = PayListItemDto.from(t, ctx, ledgerTz);
            String pgCd = resolvePgCdForPayListRow(ctx, t);
            hqNotifyMappingService.applyDisplayTransform(displayCache, pgCd, row);
            row.put("payFollowRow", Boolean.FALSE);
            list.add(row);
        }
        return new SettlementWindowPayRows(list, truncated);
    }

    /** 결제관리 payList: 기간 미입력 시 전일~당일(서버 일자)로 조회·집계 */
    private static void applyDefaultPayListSearchDates(PayListSearchRequest req) {
        if (req.getSearchFromDate() != null || req.getSearchToDate() != null) {
            return;
        }
        LocalDate today = LocalDate.now();
        req.setSearchFromDate(today.minusDays(1));
        req.setSearchToDate(today);
    }

    /** 결제·수수료 목록 등에서 가맹점 코드 집합에 대해 컨텍스트를 한 번에 구성한다. */
    public Map<String, PayListRowContext> buildPayListRowContextMap(Collection<String> merchantCodes) {
        Map<String, PayListRowContext> ctxByCode = new HashMap<>();
        if (merchantCodes == null || merchantCodes.isEmpty()) {
            return ctxByCode;
        }
        Set<String> codes = merchantCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return ctxByCode;
        }

        List<OrgUnit> merchants = orgUnitRepository.findByCodeIn(codes);
        Map<String, OrgUnit> ouByCode = merchants.stream()
                .collect(Collectors.toMap(OrgUnit::getCode, Function.identity(), (a, b) -> a));

        Map<Long, OrgUnit> byId = new HashMap<>();
        for (OrgUnit m : merchants) {
            byId.put(m.getId(), m);
        }
        Set<Long> frontier = new HashSet<>();
        for (OrgUnit m : merchants) {
            Long pid = m.getParentId();
            if (pid != null && !byId.containsKey(pid)) {
                frontier.add(pid);
            }
        }
        while (!frontier.isEmpty()) {
            List<OrgUnit> batch = orgUnitRepository.findAllById(frontier);
            if (batch.isEmpty()) {
                break;
            }
            frontier.clear();
            for (OrgUnit p : batch) {
                byId.put(p.getId(), p);
                Long pid = p.getParentId();
                if (pid != null && !byId.containsKey(pid)) {
                    frontier.add(pid);
                }
            }
        }

        Map<Long, MerchantProfile> profileByOrgId = new HashMap<>();
        if (!byId.isEmpty()) {
            for (MerchantProfile mp : merchantProfileRepository.findByOrgUnitIdIn(byId.keySet())) {
                profileByOrgId.putIfAbsent(mp.getOrgUnitId(), mp);
            }
        }

        Set<Long> merchantOrgIds = merchants.stream().map(OrgUnit::getId).collect(Collectors.toSet());
        Map<Long, List<MerchantPgBinding>> bindingsByOrgId = new HashMap<>();
        if (!merchantOrgIds.isEmpty()) {
            for (MerchantPgBinding b : merchantPgBindingRepository.findByOrgUnitIdInOrderByOrgUnitIdAscSortOrderAsc(merchantOrgIds)) {
                bindingsByOrgId.computeIfAbsent(b.getOrgUnitId(), k -> new ArrayList<>()).add(b);
            }
        }

        Map<String, DistributionFeeConfig> distByCompId = distributionFeeConfigRepository.findByCompIdIn(codes).stream()
                .collect(Collectors.toMap(DistributionFeeConfig::getCompId, d -> d, (a, b) -> a));

        Optional<CommissionPolicy> defaultPolicy = commissionPolicyRepository.findByScope("DEFAULT");
        Map<String, CommissionPolicy> policyByScope = new HashMap<>();
        for (CommissionPolicy p : commissionPolicyRepository.findByScopeIn(codes)) {
            policyByScope.put(p.getScope(), p);
        }

        Map<Long, SettlementSetting> settlementByOrgId = new HashMap<>();
        if (!merchantOrgIds.isEmpty()) {
            for (SettlementSetting ss : settlementSettingRepository.findByOrgUnitIdIn(merchantOrgIds)) {
                settlementByOrgId.putIfAbsent(ss.getOrgUnitId(), ss);
            }
        }

        for (String code : codes) {
            OrgUnit merchant = ouByCode.get(code);
            String compNm = merchant != null ? merchant.getName() : code;
            MerchantProfile profile = merchant != null ? profileByOrgId.get(merchant.getId()) : null;
            MerchantPgBinding binding = pickBindingFromList(merchant == null ? null : bindingsByOrgId.get(merchant.getId()));
            DistributionFeeConfig dist = distByCompId.get(code);
            CommissionPolicy pol = Optional.ofNullable(policyByScope.get(code)).or(() -> defaultPolicy).orElse(null);
            SettlementSetting ss = merchant == null ? null : settlementByOrgId.get(merchant.getId());
            String[] hier = hierarchyNames(merchant, byId);
            String[] hbc = hierarchyBaseCurrencies(merchant, byId, profileByOrgId);
            TxnDualLineSpec dual = null;
            if (merchant != null) {
                dual = masterDistSettlementCronZoneService.resolveTxnDualLineSpecForOrgUnitId(merchant.getId())
                        .orElse(null);
            }
            ctxByCode.put(code, new PayListRowContext(compNm, profile, binding, dist, pol, ss,
                    hier[0], hier[1], hier[2],
                    hbc[0], hbc[1], hbc[2], false, dual));
        }
        return ctxByCode;
    }

    /** 상위 체인에서 REGIONAL·MASTER_DIST·MERCHANT 각각의 프로필 기준통화(콤마 목록의 첫 토큰) */
    private String[] hierarchyBaseCurrencies(OrgUnit merchant, Map<Long, OrgUnit> byId,
                                             Map<Long, MerchantProfile> profileByOrgId) {
        String regional = "";
        String master = "";
        String merch = "";
        if (merchant == null || byId == null || profileByOrgId == null) {
            return new String[] { regional, master, merch };
        }
        OrgUnit cur = merchant;
        for (int guard = 0; guard < 24 && cur != null; guard++) {
            MerchantProfile pf = profileByOrgId.get(cur.getId());
            String bc = pf != null && pf.getBaseCurrency() != null ? pf.getBaseCurrency() : "";
            if (bc != null && !bc.isBlank()) {
                String tok = firstCsvCurrencyToken(bc);
                if (!tok.isEmpty() && cur.getOrgLevel() != null) {
                    switch (cur.getOrgLevel()) {
                        case MERCHANT -> merch = tok;
                        case MASTER_DIST -> master = tok;
                        case REGIONAL -> regional = tok;
                        default -> { }
                    }
                }
            }
            Long pid = cur.getParentId();
            cur = pid != null ? byId.get(pid) : null;
        }
        return new String[] { regional, master, merch };
    }

    private static String firstCsvCurrencyToken(String bc) {
        if (bc == null) {
            return "";
        }
        String[] parts = bc.split(",\\s*");
        return parts.length > 0 ? parts[0].trim() : "";
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

    private static final int PAY_LIST_FIN_SUMMARY_PAGE_SIZE = 500;
    private static final int PAY_LIST_FIN_SUMMARY_MAX_SCAN_PAGES = 400;

    /** 통합·결제·URL·챗봇·상계취소 — 수수료내역과 동일 건별 산식 상단 요약. 상태별 단일 화면은 null. */
    private static boolean usesFeeListAlignedPayListFinancialSummary(String variant) {
        if (variant == null || variant.isBlank()) {
            return true;
        }
        return switch (variant.trim().toUpperCase(Locale.ROOT)) {
            case "INTEGRATED", "NOTI", "URL_PAY", "CHATBOT_PAY", "OFFSET_CANCEL" -> true;
            default -> false;
        };
    }

    private static Sort.Direction sortDirectionFromSearchOrderDir(String searchOrderDir) {
        return (searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim()))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /**
     * 동일 검색 조건·조직 권한 범위 전체 건 기준 금액 요약(페이지·정렬과 무관).
     * 수수료내역·정산과 동일한 {@link FeeListTxnAmountService} 산식.
     * 추정결산 = 승인 − (취소 + 수수료 + 담보 + 부가세). 총거래 = 검색 범위 전 건 거래금액 합.
     */
    private Map<String, Object> computePayListFinancialSummary(PayListSearchRequest req, Authentication authentication) {
        String variant = req.getPayListVariant() == null || req.getPayListVariant().isBlank()
                ? "INTEGRATED" : req.getPayListVariant().trim().toUpperCase(Locale.ROOT);
        if (!usesFeeListAlignedPayListFinancialSummary(variant)) {
            return null;
        }
        LocalDateTime from = req.getSearchFromDate() != null ? req.getSearchFromDate().atStartOfDay() : null;
        LocalDateTime to = req.getSearchToDate() != null ? req.getSearchToDate().atTime(LocalTime.MAX) : null;
        Specification<PgTrnsctn> spec = buildSpecification(req, from, to, authentication);

        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        OrgLevel level = PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository);
        boolean multi = PayListStatusBarBuckets.isMultiCurrencyViewer(level);
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository,
                hqLedgerPayDisplayCurrencyAlpha());
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

        Map<String, BigDecimal> totalTxn = new HashMap<>();
        Map<String, BigDecimal> approve = new HashMap<>();
        Map<String, BigDecimal> cancel = new HashMap<>();
        Map<String, Long> approveCountByCur = new HashMap<>();
        Map<String, Long> cancelCountByCur = new HashMap<>();
        Map<String, BigDecimal> totalFeeSum = new HashMap<>();
        Map<String, BigDecimal> holdSum = new HashMap<>();
        Map<String, BigDecimal> vatSum = new HashMap<>();
        long successCount = 0;
        boolean capped = false;

        FeeCurrencyRoundResolver feeResolver = FeeCurrencyRoundResolver.from(hqLedgerSysSettingsService.getOrCreate());
        Map<String, Long> monthCbCountCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> tiersByPolicyId = new HashMap<>();
        Map<String, CommissionPolicy> polCache = new HashMap<>();
        Map<String, PayListRowContext> ctxByMerchant = new HashMap<>();
        Sort sort = Sort.by(sortDirectionFromSearchOrderDir(req.getSearchOrderDir()), "createdAt")
                .and(Sort.by(sortDirectionFromSearchOrderDir(req.getSearchOrderDir()), "trnId"));

        for (int pageIdx = 0; pageIdx < PAY_LIST_FIN_SUMMARY_MAX_SCAN_PAGES; pageIdx++) {
            Pageable pageable = PageRequest.of(pageIdx, PAY_LIST_FIN_SUMMARY_PAGE_SIZE, sort);
            Page<PgTrnsctn> slice = trnsctnRepository.findAll(spec, pageable);
            if (slice.isEmpty()) {
                break;
            }
            List<String> mids = slice.getContent().stream()
                    .map(PgTrnsctn::getMerchantId)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(mid -> !ctxByMerchant.containsKey(mid))
                    .distinct()
                    .toList();
            if (!mids.isEmpty()) {
                ctxByMerchant.putAll(buildPayListRowContextMap(mids));
            }
            for (PgTrnsctn t : slice.getContent()) {
                if (t.getMerchantId() == null || t.getMerchantId().isBlank()) {
                    continue;
                }
                String compId = t.getMerchantId().trim();
                PayListRowContext ctx = ctxByMerchant.get(compId);
                CommissionPolicy pol = polCache.computeIfAbsent(compId,
                        id -> commissionService.resolveCommissionPolicyForSettlement(id));
                String payCurKey = PayListItemDto.payCurKeyForFeeCompute(t, ctx);
                String cur = PayListStatusBarBuckets.normalizeCurrency(payCurKey);
                if (allowedCur != null && !allowedCur.contains(cur)) {
                    continue;
                }
                String st = t.getStatus() != null ? t.getStatus().trim() : "";
                BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
                totalTxn.merge(cur, amt, BigDecimal::add);
                if ("10".equals(st)) {
                    successCount++;
                    approveCountByCur.merge(cur, 1L, Long::sum);
                    approve.merge(cur, amt, BigDecimal::add);
                } else if (PayListItemDto.isCancelAmountStatus(st)) {
                    cancelCountByCur.merge(cur, 1L, Long::sum);
                    cancel.merge(cur, amt, BigDecimal::add);
                }
                FeeListTxnAmountService.FeeListTxnAmounts amts = feeListTxnAmountService.compute(
                        t, ctx, pol, payCurKey, feeResolver, monthCbCountCache, tiersByPolicyId);
                totalFeeSum.merge(cur, amts.totalFee(), BigDecimal::add);
                holdSum.merge(cur, amts.rollingHoldEst(), BigDecimal::add);
                vatSum.merge(cur, amts.feeVat(), BigDecimal::add);
            }
            if (!slice.hasNext()) {
                break;
            }
            if (pageIdx + 1 >= PAY_LIST_FIN_SUMMARY_MAX_SCAN_PAGES) {
                capped = true;
                break;
            }
        }

        if (!baseCurrencyConfigured) {
            Set<String> union = new HashSet<>();
            union.addAll(totalTxn.keySet());
            union.addAll(approve.keySet());
            union.addAll(cancel.keySet());
            union.addAll(totalFeeSum.keySet());
            union.addAll(holdSum.keySet());
            union.addAll(vatSum.keySet());
            currencyOrder.clear();
            currencyOrder.addAll(union);
            PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
        }
        if (currencyOrder.isEmpty()) {
            currencyOrder.add(primaryNorm);
        }

        return packPayListFinancialSummaryPayload(totalTxn, approve, cancel, approveCountByCur, cancelCountByCur,
                totalFeeSum, holdSum, vatSum, successCount, capped, primaryNorm, currencyOrder, effectiveMultiCurrency);
    }

    /**
     * ChillPay 통합·정산 API 행 목록에 대해 {@link #computePayListFinancialSummary} 와 동일 키의 금액 요약을 만듭니다.
     * 승인은 Chill {@link PayListStatusBarBuckets#bucketForChillStatus} 의 SUCCESS, 취소 계열은 CANCEL·REFUND·VOID.
     * 매출 금액은 {@code amount} 우선, 없으면 정산 API의 {@code settleAmount}/{@code netAmount}.
     * 총수수료는 API {@code fee} 합, 보류는 0, 지급액은 {@code netAmount}·{@code settleAmount} 우선,
     * 그다음 숫자형 {@code settled}·아니면 {@code amount − fee}(음수는 0).
     */
    public Map<String, Object> buildChillPayFinancialSummary(List<Map<String, Object>> rows,
                                                             Authentication authentication) {
        List<Map<String, Object>> list = rows != null ? rows : List.of();
        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        OrgLevel level = PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository);
        boolean multi = PayListStatusBarBuckets.isMultiCurrencyViewer(level);
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository,
                hqLedgerPayDisplayCurrencyAlpha());
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

        Map<String, BigDecimal> totalTxn = new HashMap<>();
        Map<String, BigDecimal> approve = new HashMap<>();
        Map<String, BigDecimal> cancel = new HashMap<>();
        Map<String, Long> approveCountByCur = new HashMap<>();
        Map<String, Long> cancelCountByCur = new HashMap<>();
        Map<String, BigDecimal> totalFeeSum = new HashMap<>();
        Map<String, BigDecimal> holdSum = new HashMap<>();
        Map<String, BigDecimal> vatSum = new HashMap<>();
        long successCount = 0;

        for (Map<String, Object> row : list) {
            String st = chillPayRowFirstString(row, "status", "Status");
            String bucket = PayListStatusBarBuckets.bucketForChillStatus(st);
            String cur = PayListStatusBarBuckets.normalizeCurrency(chillPayRowFirstString(row, "currency", "Currency"));
            if (allowedCur != null && !allowedCur.contains(cur)) {
                continue;
            }
            BigDecimal amt = chillPayRowApproveBasisAmount(row);
            totalTxn.merge(cur, amt, BigDecimal::add);
            BigDecimal fee = chillPayRowFeeBasis(row);
            if (fee.signum() != 0) {
                totalFeeSum.merge(cur, fee, BigDecimal::add);
            }
            BigDecimal vat = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "serviceVAT", "ServiceVAT"));
            if (vat.signum() != 0) {
                vatSum.merge(cur, vat, BigDecimal::add);
            }
            if (PayListStatusBarBuckets.SUCCESS.equals(bucket)) {
                successCount++;
                approveCountByCur.merge(cur, 1L, Long::sum);
                approve.merge(cur, amt, BigDecimal::add);
            } else if (isChillCancelFinancialBucket(bucket)) {
                cancelCountByCur.merge(cur, 1L, Long::sum);
                cancel.merge(cur, amt, BigDecimal::add);
            }
        }

        if (!baseCurrencyConfigured) {
            Set<String> union = new HashSet<>();
            union.addAll(totalTxn.keySet());
            union.addAll(approve.keySet());
            union.addAll(cancel.keySet());
            union.addAll(totalFeeSum.keySet());
            union.addAll(holdSum.keySet());
            union.addAll(vatSum.keySet());
            currencyOrder.clear();
            currencyOrder.addAll(union);
            PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
        }
        if (currencyOrder.isEmpty()) {
            currencyOrder.add(primaryNorm);
        }
        return packPayListFinancialSummaryPayload(totalTxn, approve, cancel, approveCountByCur, cancelCountByCur,
                totalFeeSum, holdSum, vatSum, successCount, false, primaryNorm, currencyOrder, effectiveMultiCurrency);
    }

    private static boolean isChillCancelFinancialBucket(String bucket) {
        return PayListStatusBarBuckets.CANCEL.equals(bucket)
                || PayListStatusBarBuckets.REFUND.equals(bucket)
                || PayListStatusBarBuckets.VOID.equals(bucket)
                || PayListStatusBarBuckets.EMAIL_VOID.equals(bucket)
                || PayListStatusBarBuckets.FORCE_REFUND.equals(bucket);
    }

    /** 결제내역·통합내역·일별 상세 공통 meta.payListFinancialSummary 키 조립 */
    private static Map<String, Object> packPayListFinancialSummaryPayload(
            Map<String, BigDecimal> totalTxn,
            Map<String, BigDecimal> approve,
            Map<String, BigDecimal> cancel,
            Map<String, Long> approveCountByCur,
            Map<String, Long> cancelCountByCur,
            Map<String, BigDecimal> totalFeeSum,
            Map<String, BigDecimal> holdSum,
            Map<String, BigDecimal> vatSum,
            long successCount,
            boolean capped,
            String primaryNorm,
            List<String> currencyOrder,
            boolean effectiveMultiCurrency) {
        Map<String, String> totalTxnPlain = new LinkedHashMap<>();
        Map<String, String> approvePlain = new LinkedHashMap<>();
        Map<String, Long> approveCountPlain = new LinkedHashMap<>();
        Map<String, String> cancelPlain = new LinkedHashMap<>();
        Map<String, Long> cancelCountPlain = new LinkedHashMap<>();
        Map<String, String> paymentPlain = new LinkedHashMap<>();
        Map<String, String> expectedPayoutPlain = new LinkedHashMap<>();
        Map<String, String> estimatedSettlementPlain = new LinkedHashMap<>();
        Map<String, String> feePlain = new LinkedHashMap<>();
        Map<String, String> holdPlain = new LinkedHashMap<>();
        Map<String, String> vatPlain = new LinkedHashMap<>();
        for (String c : currencyOrder) {
            BigDecimal a = approve.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal k = cancel.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal f = totalFeeSum.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal h = holdSum.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal v = vatSum.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal expectedPayout = a.subtract(k).subtract(f).subtract(v);
            BigDecimal estimated = expectedPayout.subtract(h);
            totalTxnPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(totalTxn.getOrDefault(c, BigDecimal.ZERO)));
            approvePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(a));
            approveCountPlain.put(c, approveCountByCur.getOrDefault(c, 0L));
            cancelPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(k));
            cancelCountPlain.put(c, cancelCountByCur.getOrDefault(c, 0L));
            feePlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(f));
            holdPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(h));
            vatPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(v));
            expectedPayoutPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(expectedPayout));
            estimatedSettlementPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(estimated));
            paymentPlain.put(c, PayListStatusBarBuckets.stripTrailingZeros(estimated));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("payListFeeListAligned", true);
        out.put("successCount", successCount);
        out.put("multiCurrency", effectiveMultiCurrency);
        out.put("primaryCurrency", primaryNorm);
        out.put("currencyOrder", new ArrayList<>(currencyOrder));
        out.put("totalTxnByCurrency", totalTxnPlain);
        out.put("approveByCurrency", approvePlain);
        out.put("approveCountByCurrency", approveCountPlain);
        out.put("cancelByCurrency", cancelPlain);
        out.put("cancelCountByCurrency", cancelCountPlain);
        out.put("expectedPayoutByCurrency", expectedPayoutPlain);
        out.put("estimatedSettlementByCurrency", estimatedSettlementPlain);
        out.put("paymentByCurrency", paymentPlain);
        out.put("feeByCurrency", feePlain);
        out.put("vatByCurrency", vatPlain);
        out.put("holdByCurrency", holdPlain);
        if (capped) {
            out.put("capped", true);
        }
        return out;
    }

    /** 정산관리(수수료내역·일별수수료·통합정산) 상단·상세 공통 집계 */
    public Map<String, Object> packFeeListFinancialSummaryPayload(
            Map<String, BigDecimal> totalTxn,
            Map<String, BigDecimal> approve,
            Map<String, BigDecimal> cancel,
            Map<String, Long> approveCountByCur,
            Map<String, Long> cancelCountByCur,
            Map<String, BigDecimal> totalFeeSum,
            Map<String, BigDecimal> holdSum,
            Map<String, BigDecimal> vatSum,
            long totalCount,
            boolean capped,
            String primaryNorm,
            List<String> currencyOrder,
            boolean effectiveMultiCurrency) {
        Map<String, Object> out = packPayListFinancialSummaryPayload(totalTxn, approve, cancel, approveCountByCur,
                cancelCountByCur, totalFeeSum, holdSum, vatSum, 0, capped, primaryNorm, currencyOrder,
                effectiveMultiCurrency);
        out.put("feeListSummary", true);
        out.put("totalCount", totalCount);
        out.remove("payListFeeListAligned");
        out.remove("successCount");
        return out;
    }

    /** 결제 검색: amount. 정산 검색: amount 없을 때 settleAmount → netAmount. */
    private static BigDecimal chillPayRowApproveBasisAmount(Map<String, Object> row) {
        BigDecimal pay = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "amount", "Amount"));
        if (pay.compareTo(BigDecimal.ZERO) > 0) {
            return pay;
        }
        BigDecimal settle = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "settleAmount", "SettleAmount"));
        if (settle.compareTo(BigDecimal.ZERO) > 0) {
            return settle;
        }
        return PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "netAmount", "NetAmount"));
    }

    private static BigDecimal chillPayRowFeeBasis(Map<String, Object> row) {
        BigDecimal fee = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "fee", "Fee"));
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            return fee;
        }
        BigDecimal svc = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "serviceAmount", "ServiceAmount"));
        BigDecimal vat = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "serviceVAT", "ServiceVAT"));
        BigDecimal wht = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "serviceWHT", "ServiceWHT"));
        return svc.add(vat).add(wht);
    }

    private static BigDecimal chillPayRowPreferredPayout(Map<String, Object> row, BigDecimal amt, BigDecimal fee) {
        BigDecimal netAmt = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "netAmount", "NetAmount"));
        if (netAmt.compareTo(BigDecimal.ZERO) > 0) {
            return netAmt;
        }
        BigDecimal settleAmt = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "settleAmount", "SettleAmount"));
        if (settleAmt.compareTo(BigDecimal.ZERO) > 0) {
            return settleAmt;
        }
        BigDecimal settledMoney = PayListStatusBarBuckets.parseMoney(chillPayRowFirstObject(row, "settled", "Settled"));
        if (settledMoney.compareTo(BigDecimal.ZERO) > 0) {
            return settledMoney;
        }
        BigDecimal net = amt.subtract(fee);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
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
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository,
                hqLedgerPayDisplayCurrencyAlpha());
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
                .when(cb.or(cb.equal(st, "21"), cb.equal(st, "40")), cb.literal(PayListStatusBarBuckets.VOID))
                .when(cb.or(cb.equal(st, "22"), cb.equal(st, "41")), cb.literal(PayListStatusBarBuckets.EMAIL_VOID))
                .when(cb.or(cb.equal(st, "30"), cb.equal(st, "42")), cb.literal(PayListStatusBarBuckets.REFUND))
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
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository,
                hqLedgerPayDisplayCurrencyAlpha());
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
        List<String> visibleBuckets = visiblePayListStatusBarBucketsForVariant(variant);
        return roll.toPayload(effectiveMultiCurrency, primary, false, true, displayOrder, visibleBuckets);
    }

    /** 화면별로 상태바에 노출할 버킷(0건도 슬롯 표시). 통합·결제내역은 통합내역과 동일 8종. */
    private static List<String> visiblePayListStatusBarBucketsForVariant(String variant) {
        if (variant == null || variant.isBlank()) {
            return PayListStatusBarBuckets.DEFAULT_STATUS_BAR_BUCKET_ORDER;
        }
        return switch (variant.trim().toUpperCase(Locale.ROOT)) {
            case "SUCCESS" -> List.of(PayListStatusBarBuckets.SUCCESS);
            case "FAIL" -> List.of(PayListStatusBarBuckets.FAIL);
            case "VOID" -> List.of(PayListStatusBarBuckets.VOID);
            case "MANUAL_VOID" -> List.of(PayListStatusBarBuckets.EMAIL_VOID);
            case "REFUND" -> List.of(PayListStatusBarBuckets.REFUND);
            case "FORCE_REFUND" -> List.of(PayListStatusBarBuckets.FORCE_REFUND);
            case "CANCEL" -> List.of(PayListStatusBarBuckets.CANCEL);
            case "OFFSET_CANCEL" -> List.of(
                    PayListStatusBarBuckets.VOID,
                    PayListStatusBarBuckets.EMAIL_VOID,
                    PayListStatusBarBuckets.REFUND,
                    PayListStatusBarBuckets.FORCE_REFUND,
                    PayListStatusBarBuckets.CANCEL,
                    PayListStatusBarBuckets.OTHER);
            default -> PayListStatusBarBuckets.DEFAULT_STATUS_BAR_BUCKET_ORDER;
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

    private static MerchantPgBinding pickBindingFromList(List<MerchantPgBinding> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream().filter(b -> "Y".equalsIgnoreCase(String.valueOf(b.getOperationalYn()).trim()))
                .findFirst()
                .orElse(list.get(0));
    }

    /** [0]=총판, [1]=지사, [2]=대리점(영업점) */
    private String[] hierarchyNames(OrgUnit merchant, Map<Long, OrgUnit> byId) {
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
            cur = (pid != null && byId != null) ? byId.get(pid) : null;
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
        String fieldType = req.getSearchFieldType() != null ? req.getSearchFieldType().trim().toUpperCase(Locale.ROOT) : "";
        boolean unified = !fieldType.isEmpty();
        String unifiedKw = req.getSearchKeyword() != null ? req.getSearchKeyword().trim() : "";
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
            if (unified) {
                addUnifiedFieldPredicate(parts, root, cb, fieldType, unifiedKw);
            } else {
                addKeywordPredicate(parts, root, cb, req.getSearchKeyword());
                addLike(parts, root, cb, "chillTransactionId", req.getSearchChillTxnId());
                addLike(parts, root, cb, "approvalNo", req.getSearchCardAprvNo());
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    /**
     * null = 가맹점 제한 없음, 비어 있지 않은 Set = 해당 코드만, empty Set = 조건 불충족(결과 0건).
     */
    private Set<String> resolveMerchantFilterCodes(PayListSearchRequest req, Authentication authentication) {
        Set<String> mcs = null;
        String ft = req.getSearchFieldType() != null ? req.getSearchFieldType().trim().toUpperCase(Locale.ROOT) : "";
        String kw = req.getSearchKeyword() != null ? req.getSearchKeyword().trim() : "";
        if (!ft.isEmpty() && !kw.isEmpty()) {
            switch (ft) {
                case "COMP_NM" -> mcs = intersectCodes(mcs, codesFromCompField("NM", kw));
                case "COMP_ID" -> mcs = intersectCodes(mcs, codesFromCompField("CODE", kw));
                case "MID" -> mcs = intersectCodes(mcs, codesFromMid(kw));
                default -> { }
            }
        }
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

    private void addUnifiedFieldPredicate(List<Predicate> parts, Root<PgTrnsctn> root, CriteriaBuilder cb,
                                         String fieldType, String kw) {
        if (fieldType == null || fieldType.isBlank()) {
            return;
        }
        String ft = fieldType.trim().toUpperCase(Locale.ROOT);
        String v = kw != null ? kw.trim() : "";
        if ("ALL".equals(ft) || v.isEmpty()) {
            if (!v.isEmpty()) {
                addKeywordPredicate(parts, root, cb, v);
            }
            return;
        }
        switch (ft) {
            case "CUSTOMER_ID" -> addLike(parts, root, cb, "customerId", v);
            case "APPROVAL_NO" -> addLike(parts, root, cb, "chillTransactionId", v);
            case "ORDER_NO" -> addLike(parts, root, cb, "orderNo", v);
            case "ROUTE" -> addLike(parts, root, cb, "routeNo", v);
            case "CURRENCY" -> addLike(parts, root, cb, "curType", v);
            case "STATUS" -> addLike(parts, root, cb, "status", v);
            case "AMOUNT" -> {
                try {
                    String digits = v.replace(",", "").trim();
                    BigDecimal amt = new BigDecimal(digits);
                    parts.add(cb.equal(root.get("amtKrw"), amt));
                } catch (Exception ignored) {
                    parts.add(cb.disjunction());
                }
            }
            case "COMP_NM", "COMP_ID", "MID" -> {
                /* 가맹 필터는 resolveMerchantFilterCodes에서 처리 */
            }
            default -> {
            }
        }
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

    private static final int OPS_INTEGRATED_REPORT_MAX_DAYS = 93;
    private static final int DAILY_PAY_SUMMARY_MAX_DAYS = 93;

    /** 스트림(ResultSet) 열린 상태에서 중첩 쿼리를 막기 위해 가맹별 정산용 수수료정책을 선로딩합니다. */
    private Map<String, CommissionPolicy> preloadSettlementCommissionPolicies(Set<String> merchantCodes) {
        Map<String, CommissionPolicy> cache = new HashMap<>();
        if (merchantCodes == null || merchantCodes.isEmpty()) {
            return cache;
        }
        for (String mid : merchantCodes) {
            if (mid == null || mid.isBlank()) {
                continue;
            }
            String key = mid.trim();
            cache.put(key, commissionService.resolveCommissionPolicyForSettlement(key));
        }
        return cache;
    }

    /** 일별 집계·통합 리포트 목록: 기본 최신일 우선(DESC). {@code searchOrderDir=ASC} 이면 일자 오름차순 */
    public static void applyDailySummaryDayListOrder(List<Map<String, Object>> rows, String searchOrderDir) {
        if (rows == null || rows.size() <= 1) {
            return;
        }
        if (searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim())) {
            Collections.reverse(rows);
        }
    }

    /**
     * 운영관리 통합 리포트: 적재일(createdAt) 기준 일자별 전역 집계 + 가맹점 동적 열(해당 일 거래가 있는 가맹만, 업체코드 오름차순).
     * 결제내역 통합(INTEGRATED)과 동일한 {@link #buildSpecification}·조직 가맹 범위를 사용합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> buildOpsIntegratedReport(PayListSearchRequest template,
                                                      Authentication authentication) {
        PayListSearchRequest reqIn = PayListSearchRequest.shallowCopy(template != null ? template : new PayListSearchRequest());
        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        LocalDate today = LocalDate.now(ledgerTz);
        LocalDate rangeFrom = reqIn.getSearchFromDate();
        LocalDate rangeTo = reqIn.getSearchToDate();
        if (rangeTo == null) {
            rangeTo = today;
        }
        if (rangeFrom == null) {
            rangeFrom = today.minusDays(1);
        }
        if (rangeFrom.isAfter(rangeTo)) {
            throw new IllegalArgumentException("거래일자 시작이 종료보다 늦을 수 없습니다.");
        }
        long spanRequested = ChronoUnit.DAYS.between(rangeFrom, rangeTo) + 1;
        if (spanRequested > OPS_INTEGRATED_REPORT_MAX_DAYS) {
            throw new IllegalArgumentException("조회 기간은 " + OPS_INTEGRATED_REPORT_MAX_DAYS + "일 이내로 지정해 주세요.");
        }
        LocalDate effectiveTo = rangeTo.isAfter(today) ? today : rangeTo;
        LocalDate effectiveFrom = rangeFrom;
        boolean rangeStartAdjusted = false;
        if (effectiveFrom.isAfter(effectiveTo)) {
            effectiveFrom = effectiveTo;
            rangeStartAdjusted = true;
        }
        PayListSearchRequest base = PayListSearchRequest.shallowCopy(reqIn);
        base.setPayListVariant("INTEGRATED");
        base.setSearchFromDate(effectiveFrom);
        base.setSearchToDate(effectiveTo);
        LocalDateTime fullFrom = effectiveFrom.atStartOfDay();
        LocalDateTime fullTo = effectiveTo.atTime(LocalTime.MAX);
        Specification<PgTrnsctn> specFull = buildSpecification(base, fullFrom, fullTo, authentication);

        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        OrgLevel level = PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository);
        boolean multi = PayListStatusBarBuckets.isMultiCurrencyViewer(level);
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository,
                hqLedgerPayDisplayCurrencyAlpha());
        String primaryNorm = PayListStatusBarBuckets.normalizeCurrency(primary);
        boolean baseCurrencyConfigured = isViewerBaseCurrencyConfigured(user);
        final List<String> currencyOrderTemplate;
        if (baseCurrencyConfigured) {
            currencyOrderTemplate = resolveViewerDisplayCurrencyOrder(user, multi);
        } else {
            currencyOrderTemplate = new ArrayList<>();
        }
        Set<String> allowedCur = baseCurrencyConfigured ? new HashSet<>(currencyOrderTemplate) : null;

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> cqMid = cb.createQuery(String.class);
        Root<PgTrnsctn> rMid = cqMid.from(PgTrnsctn.class);
        cqMid.select(rMid.get("merchantId")).distinct(true);
        cqMid.where(specFull.toPredicate(rMid, cqMid, cb));
        Set<String> mids = new HashSet<>();
        for (String sMid : entityManager.createQuery(cqMid).getResultList()) {
            if (sMid != null && !sMid.isBlank()) {
                mids.add(sMid.trim());
            }
        }
        Map<String, PayListRowContext> ctxMap = buildPayListRowContextMap(mids);
        FeeCurrencyRoundResolver feeResolver = FeeCurrencyRoundResolver.from(hqLedgerSysSettingsService.getOrCreate());
        Map<String, CommissionPolicy> integratedPolCache = preloadSettlementCommissionPolicies(mids);

        Map<LocalDate, OpsIntegratedDayAgg> byDay = new LinkedHashMap<>();
        for (LocalDate d = effectiveFrom; !d.isAfter(effectiveTo); d = d.plusDays(1)) {
            byDay.put(d, new OpsIntegratedDayAgg());
        }

        CriteriaQuery<Tuple> cqRows = cb.createTupleQuery();
        Root<PgTrnsctn> root = cqRows.from(PgTrnsctn.class);
        cqRows.multiselect(root.get("createdAt"), root.get("merchantId"), root.get("status"), root.get("curType"), root.get("amtKrw"));
        cqRows.where(specFull.toPredicate(root, cqRows, cb));
        List<Tuple> rowTuples = entityManager.createQuery(cqRows).getResultList();
        for (Tuple tup : rowTuples) {
                LocalDateTime cat = tup.get(0, LocalDateTime.class);
                if (cat == null) {
                    continue;
                }
                LocalDate day = cat.toLocalDate();
                OpsIntegratedDayAgg agg = byDay.get(day);
                if (agg == null) {
                    continue;
                }
                String midRaw = tup.get(1, String.class);
                String st = tup.get(2, String.class);
                String curRaw = tup.get(3, String.class);
                BigDecimal amt = tup.get(4, BigDecimal.class);
                if (amt == null) {
                    amt = BigDecimal.ZERO;
                }
                agg.txnCount++;
                String bucket = PayListStatusBarBuckets.bucketForPgStatus(st);
                agg.bucketCount.merge(bucket, 1L, Long::sum);

                String cur = PayListStatusBarBuckets.normalizeCurrency(curRaw);
                boolean curOk = allowedCur == null || allowedCur.contains(cur);

                String mid = midRaw != null ? midRaw.trim() : "";
                PayListRowContext ctx = !mid.isEmpty() ? ctxMap.get(mid) : null;
                if (!mid.isEmpty()) {
                    CommissionPolicy pol = integratedPolCache.get(mid);
                    if (pol == null) {
                        pol = commissionService.resolveCommissionPolicyForSettlement(mid);
                        integratedPolCache.put(mid, pol);
                    }
                    BigDecimal bucketPolicyUnit = FeeListTxnAmountService.roundPolicyFixedFeeForBucket(
                            bucket, pol, feeResolver);
                    if (bucketPolicyUnit.signum() > 0) {
                        agg.bucketPolicyFee.merge(bucket, bucketPolicyUnit, BigDecimal::add);
                    }
                }

                if (!mid.isEmpty()) {
                    OpsIntegratedMerchantAgg mag = agg.merchants.computeIfAbsent(mid, k -> new OpsIntegratedMerchantAgg(mid, ctx));
                    mag.txnCount++;
                    mag.bucketCount.merge(bucket, 1L, Long::sum);
                    if ("10".equals(st)) {
                        mag.successCount++;
                        if (curOk) {
                            mag.approve.merge(cur, amt, BigDecimal::add);
                            if (ctx != null) {
                                PayListRowContext policyCtx = ctx.withOmitSettlementFeeFromApprovedTxnBreakdown(true);
                                PayListItemDto.ApprovedSettlementParts p = PayListItemDto.approvedSettlementParts(amt, policyCtx);
                                mag.feeExVat.merge(cur, p.feeAmt, BigDecimal::add);
                                mag.feeVat.merge(cur, p.feeVat, BigDecimal::add);
                                mag.hold.merge(cur, p.holdAmt, BigDecimal::add);
                                mag.perTxnParts.merge(cur, p.perTxAmt, BigDecimal::add);
                                mag.extraPct.merge(cur, p.extraPctAmt, BigDecimal::add);
                            }
                        }
                    } else if (PayListItemDto.isCancelAmountStatus(st) && curOk) {
                        mag.cancel.merge(cur, amt, BigDecimal::add);
                    }
                }

                if ("10".equals(st)) {
                    agg.successCount++;
                    if (curOk) {
                        agg.approve.merge(cur, amt, BigDecimal::add);
                        if (ctx != null) {
                            PayListRowContext policyCtx = ctx.withOmitSettlementFeeFromApprovedTxnBreakdown(true);
                            PayListItemDto.ApprovedSettlementParts p = PayListItemDto.approvedSettlementParts(amt, policyCtx);
                            agg.feeExVat.merge(cur, p.feeAmt, BigDecimal::add);
                            agg.feeVat.merge(cur, p.feeVat, BigDecimal::add);
                            agg.hold.merge(cur, p.holdAmt, BigDecimal::add);
                        }
                    }
                } else if (PayListItemDto.isCancelAmountStatus(st) && curOk) {
                    agg.cancel.merge(cur, amt, BigDecimal::add);
                }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (LocalDate d = effectiveTo; !d.isBefore(effectiveFrom); d = d.minusDays(1)) {
            OpsIntegratedDayAgg agg = byDay.getOrDefault(d, new OpsIntegratedDayAgg());
            list.add(agg.toApiRow(d, primaryNorm, baseCurrencyConfigured, currencyOrderTemplate, ctxMap, feeResolver));
        }
        applyDailySummaryDayListOrder(list, reqIn.getSearchOrderDir());

        Map<String, Object> meta = new LinkedHashMap<>();
        if (rangeStartAdjusted) {
            meta.put("dateRangeAdjusted", true);
            meta.put("note", "조회 시작일이 집계 가능한 마지막 일(" + effectiveTo + ")보다 뒤여서 해당 일 기준으로 표시합니다.");
        }
        if (rangeTo.isAfter(today)) {
            meta.put("displayToDate", effectiveTo.toString());
            meta.put("requestedToDate", rangeTo.toString());
        }
        meta.put("primaryCurrency", primaryNorm);
        meta.put("multiCurrency", multi || !baseCurrencyConfigured);
        meta.put("feeCurrencyFormats", feeResolver.toClientByCurrencyMap());
        meta.put("integratedReportNote",
                "집계 기준일은 거래 적재일(created_at)입니다. 일별결제와 동일합니다. "
                        + "성공·취소·실패·무효·이메일무효·환불·강제환불 열의 금액은 결제액이 아니라 「건수 × 기본 수수료 정책의 해당 상태 건당(고정) 수수료」입니다(성공=건당, 실패=실패수수료 등). "
                        + "가맹 열은 해당 일자에 한 건이라도 집계된 가맹만 표시되며 업체코드 오름차순입니다. "
                        + "가맹 수수료(변동·% / 건당)은 정책 결제율(%)·건당 표시입니다. "
                        + "일자 클릭 시 하단 통합 결제내역의 수수료·정산액은 수수료내역과 동일한 건별 산식입니다.");
        try {
            Map<String, Object> bar = computePgTxnStatusBar(base, authentication);
            Map<String, Object> fin = computePayListFinancialSummary(base, authentication);
            meta.put("payListStatusBar", bar);
            meta.put("payListFinancialSummary", fin);
            putHqLedgerPayDisplayCurrencyMeta(meta);
        } catch (RuntimeException ignored) {
            /* 집계 실패 시 일자 목록만 반환 */
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", list);
        out.put("meta", meta);
        return out;
    }

    /** 운영관리 통합 리포트 — 일자 단위 */
    private static final class OpsIntegratedDayAgg {
        long txnCount;
        long successCount;
        final Map<String, Long> bucketCount = new HashMap<>();
        final Map<String, BigDecimal> bucketPolicyFee = new HashMap<>();
        final Map<String, BigDecimal> approve = new HashMap<>();
        final Map<String, BigDecimal> cancel = new HashMap<>();
        final Map<String, BigDecimal> feeExVat = new HashMap<>();
        final Map<String, BigDecimal> feeVat = new HashMap<>();
        final Map<String, BigDecimal> hold = new HashMap<>();
        final Map<String, OpsIntegratedMerchantAgg> merchants = new HashMap<>();

        Map<String, Object> toApiRow(LocalDate day,
                                     String primaryNorm,
                                     boolean baseCurrencyConfigured,
                                     List<String> currencyOrderTemplate,
                                     Map<String, PayListRowContext> ctxMap,
                                     FeeCurrencyRoundResolver feeResolver) {
            List<String> currencyOrder = new ArrayList<>(currencyOrderTemplate);
            if (!baseCurrencyConfigured) {
                currencyOrder.clear();
                Set<String> union = new HashSet<>();
                union.addAll(approve.keySet());
                union.addAll(cancel.keySet());
                union.addAll(feeExVat.keySet());
                union.addAll(feeVat.keySet());
                union.addAll(hold.keySet());
                currencyOrder.addAll(union);
                PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
            }
            if (currencyOrder.isEmpty()) {
                currencyOrder.add(primaryNorm);
            }
            BigDecimal totalPayment = BigDecimal.ZERO;
            BigDecimal totalFeeExVat = BigDecimal.ZERO;
            BigDecimal totalVat = BigDecimal.ZERO;
            BigDecimal totalHold = BigDecimal.ZERO;
            for (String c : currencyOrder) {
                BigDecimal a = approve.getOrDefault(c, BigDecimal.ZERO);
                BigDecimal k = cancel.getOrDefault(c, BigDecimal.ZERO);
                FeeListRoundingPolicy rp = feeResolver.forCurrency(c);
                totalPayment = totalPayment.add(a.subtract(k));
                totalFeeExVat = totalFeeExVat.add(FeeListRoundingPolicy.round(feeExVat.getOrDefault(c, BigDecimal.ZERO), rp));
                totalVat = totalVat.add(FeeListRoundingPolicy.round(feeVat.getOrDefault(c, BigDecimal.ZERO), rp));
                totalHold = totalHold.add(FeeListRoundingPolicy.round(hold.getOrDefault(c, BigDecimal.ZERO), rp));
            }
            List<Map<String, Object>> bucketRows = new ArrayList<>();
            for (String bk : PayListStatusBarBuckets.DEFAULT_STATUS_BAR_BUCKET_ORDER) {
                if (PayListStatusBarBuckets.OTHER.equals(bk)) {
                    continue;
                }
                Map<String, Object> br = new LinkedHashMap<>();
                br.put("bucket", bk);
                br.put("count", bucketCount.getOrDefault(bk, 0L));
                br.put("amount", PayListStatusBarBuckets.stripTrailingZeros(bucketPolicyFee.getOrDefault(bk, BigDecimal.ZERO)));
                bucketRows.add(br);
            }
            List<Map<String, Object>> merchantRows = new ArrayList<>();
            merchants.values().stream()
                    .filter(m -> m.txnCount > 0)
                    .sorted(Comparator.comparing(m -> m.compId, Comparator.nullsLast(String::compareTo)))
                    .forEach(m -> merchantRows.add(m.toApiRow(currencyOrder, ctxMap, feeResolver)));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("day", day.toString());
            row.put("totalPayment", PayListStatusBarBuckets.stripTrailingZeros(totalPayment));
            row.put("totalFeeExVat", PayListStatusBarBuckets.stripTrailingZeros(totalFeeExVat));
            row.put("depositAmount", PayListStatusBarBuckets.stripTrailingZeros(totalHold));
            row.put("vat", PayListStatusBarBuckets.stripTrailingZeros(totalVat));
            row.put("totalTxnCount", txnCount);
            row.put("buckets", bucketRows);
            row.put("merchants", merchantRows);
            row.put("successCount", successCount);
            return row;
        }
    }

    /** 가맹점별 일 집계(통합 리포트 동적 열) */
    private static final class OpsIntegratedMerchantAgg {
        final String compId;
        final PayListRowContext ctx;
        long txnCount;
        long successCount;
        final Map<String, Long> bucketCount = new HashMap<>();
        final Map<String, BigDecimal> approve = new HashMap<>();
        final Map<String, BigDecimal> cancel = new HashMap<>();
        final Map<String, BigDecimal> feeExVat = new HashMap<>();
        final Map<String, BigDecimal> feeVat = new HashMap<>();
        final Map<String, BigDecimal> hold = new HashMap<>();
        final Map<String, BigDecimal> perTxnParts = new HashMap<>();
        final Map<String, BigDecimal> extraPct = new HashMap<>();

        OpsIntegratedMerchantAgg(String compId, PayListRowContext ctx) {
            this.compId = compId;
            this.ctx = ctx;
        }

        Map<String, Object> toApiRow(List<String> currencyOrder,
                                     Map<String, PayListRowContext> ctxMap,
                                     FeeCurrencyRoundResolver feeResolver) {
            BigDecimal totalAmt = BigDecimal.ZERO;
            for (String c : currencyOrder) {
                BigDecimal a = approve.getOrDefault(c, BigDecimal.ZERO);
                BigDecimal k = cancel.getOrDefault(c, BigDecimal.ZERO);
                totalAmt = totalAmt.add(a.subtract(k));
            }
            PayListRowContext cctx = ctx != null ? ctx : ctxMap.get(compId);
            String compNm = "";
            if (cctx != null && cctx.getCompNm() != null) {
                compNm = cctx.getCompNm();
            }
            String feeVar = "";
            String feePer = "";
            CommissionPolicy pol = cctx != null ? cctx.getPolicy() : null;
            if (pol != null && feeResolver != null) {
                String policyCur = pol.getCurrencyCode() != null && !pol.getCurrencyCode().isBlank()
                        ? pol.getCurrencyCode().trim().toUpperCase(Locale.ROOT) : "KRW";
                FeeListRoundingPolicy rp = feeResolver.forCurrency(policyCur);
                BigDecimal payRate = pol.getPayRate() != null ? pol.getPayRate() : BigDecimal.ZERO;
                BigDecimal perTx = pol.getPerTxFee() != null ? pol.getPerTxFee() : BigDecimal.ZERO;
                feeVar = PayListStatusBarBuckets.stripTrailingZeros(FeeListRoundingPolicy.round(payRate, rp));
                feePer = PayListStatusBarBuckets.stripTrailingZeros(FeeListRoundingPolicy.round(perTx, rp));
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("compId", compId);
            m.put("compNm", compNm);
            m.put("feeVariable", feeVar);
            m.put("feePerTxn", feePer);
            m.put("feePolicyCurrency", pol != null && pol.getCurrencyCode() != null ? pol.getCurrencyCode().trim().toUpperCase(Locale.ROOT) : "");
            m.put("txnCount", txnCount);
            m.put("successCount", successCount);
            m.put("totalAmount", PayListStatusBarBuckets.stripTrailingZeros(totalAmt));
            return m;
        }
    }

    /**
     * 결제내역과 동일 필터({@link PayListSearchRequest})로, 적재일(createdAt) 기준 일자별 건수·금액 요약.
     * 상세 목록은 클라이언트가 해당 일자로 {@code /api/calc/payList} 를 호출합니다.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buildDailyPayListSummary(LocalDate rangeFrom,
                                                              LocalDate rangeTo,
                                                              PayListSearchRequest template,
                                                              Authentication authentication) {
        if (rangeFrom == null || rangeTo == null || rangeFrom.isAfter(rangeTo)) {
            return List.of();
        }
        long span = ChronoUnit.DAYS.between(rangeFrom, rangeTo) + 1;
        if (span > DAILY_PAY_SUMMARY_MAX_DAYS) {
            throw new IllegalArgumentException("조회 기간은 " + DAILY_PAY_SUMMARY_MAX_DAYS + "일 이내로 지정해 주세요.");
        }
        ZoneId ledgerTz = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
        LocalDate today = LocalDate.now(ledgerTz);
        LocalDate effectiveTo = rangeTo.isAfter(today) ? today : rangeTo;
        if (rangeFrom.isAfter(effectiveTo)) {
            return List.of();
        }
        PayListSearchRequest base = PayListSearchRequest.shallowCopy(template);
        base.setPayListVariant("INTEGRATED");
        base.setSearchFromDate(rangeFrom);
        base.setSearchToDate(effectiveTo);
        LocalDateTime fullFrom = rangeFrom.atStartOfDay();
        LocalDateTime fullTo = effectiveTo.atTime(LocalTime.MAX);
        Specification<PgTrnsctn> specFull = buildSpecification(base, fullFrom, fullTo, authentication);

        AppUser user = (authentication != null && authentication.getPrincipal() instanceof AppUser u) ? u : null;
        OrgLevel level = PayListStatusBarBuckets.resolveViewerOrgLevel(user, orgUnitRepository);
        boolean multi = PayListStatusBarBuckets.isMultiCurrencyViewer(level);
        String primary = PayListStatusBarBuckets.resolveViewerPrimaryCurrency(user, orgUnitRepository, commissionPolicyRepository,
                hqLedgerPayDisplayCurrencyAlpha());
        String primaryNorm = PayListStatusBarBuckets.normalizeCurrency(primary);
        boolean baseCurrencyConfigured = isViewerBaseCurrencyConfigured(user);
        final List<String> currencyOrderTemplate;
        if (baseCurrencyConfigured) {
            currencyOrderTemplate = resolveViewerDisplayCurrencyOrder(user, multi);
        } else {
            currencyOrderTemplate = new ArrayList<>();
        }
        Set<String> allowedCur = baseCurrencyConfigured ? new HashSet<>(currencyOrderTemplate) : null;
        boolean effectiveMultiCurrency = multi || !baseCurrencyConfigured;

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        Set<String> mcodes = new HashSet<>();
        CriteriaQuery<String> cqMids = cb.createQuery(String.class);
        Root<PgTrnsctn> rootMids = cqMids.from(PgTrnsctn.class);
        cqMids.select(rootMids.get("merchantId"));
        cqMids.where(specFull.toPredicate(rootMids, cqMids, cb));
        cqMids.distinct(true);
        for (String mid : entityManager.createQuery(cqMids).getResultList()) {
            if (mid != null && !mid.isBlank()) {
                mcodes.add(mid.trim());
            }
        }
        Map<String, PayListRowContext> ctxMap = buildPayListRowContextMap(mcodes);

        FeeCurrencyRoundResolver dailyFeeResolver = FeeCurrencyRoundResolver.from(hqLedgerSysSettingsService.getOrCreate());
        Map<String, CommissionPolicy> dailyPolCache = preloadSettlementCommissionPolicies(mcodes);
        Map<String, Long> dailyMonthCbCache = new HashMap<>();
        Map<Long, List<ChargebackFeeTier>> dailyTiersByPolicyId = new HashMap<>();

        Map<LocalDate, PayDayFinancialAgg> byDay = new LinkedHashMap<>();
        for (LocalDate d = rangeFrom; !d.isAfter(effectiveTo); d = d.plusDays(1)) {
            byDay.put(d, new PayDayFinancialAgg());
        }

        CriteriaQuery<Tuple> cqRows = cb.createTupleQuery();
        Root<PgTrnsctn> root = cqRows.from(PgTrnsctn.class);
        cqRows.multiselect(
                root.get("createdAt"),
                root.get("merchantId"),
                root.get("status"),
                root.get("curType"),
                root.get("amtKrw"));
        cqRows.where(specFull.toPredicate(root, cqRows, cb));
        List<Tuple> rowTuples = entityManager.createQuery(cqRows).getResultList();
        for (Tuple tup : rowTuples) {
            LocalDateTime cat = tup.get(0, LocalDateTime.class);
            if (cat == null) {
                continue;
            }
            LocalDate day = cat.toLocalDate();
            PayDayFinancialAgg agg = byDay.get(day);
            if (agg == null) {
                continue;
            }
            String mid = tup.get(1, String.class);
            String st = tup.get(2, String.class);
            String curRaw = tup.get(3, String.class);
            BigDecimal amt = tup.get(4, BigDecimal.class);
            if (amt == null) {
                amt = BigDecimal.ZERO;
            }
            agg.txnCount++;
            String bucket = PayListStatusBarBuckets.bucketForPgStatus(st);
            agg.bucketCount.merge(bucket, 1L, Long::sum);
            String cur = PayListStatusBarBuckets.normalizeCurrency(curRaw);
            if (allowedCur != null && !allowedCur.contains(cur)) {
                continue;
            }
            agg.totalTxn.merge(cur, amt, BigDecimal::add);
            if ("10".equals(st)) {
                agg.successCount++;
                agg.approveCountByCur.merge(cur, 1L, Long::sum);
                agg.approve.merge(cur, amt, BigDecimal::add);
            } else if (PayListItemDto.isCancelAmountStatus(st)) {
                agg.cancelCountByCur.merge(cur, 1L, Long::sum);
                agg.cancel.merge(cur, amt, BigDecimal::add);
            }
            if (mid == null || mid.isBlank()) {
                continue;
            }
            String compId = mid.trim();
            PayListRowContext ctx = ctxMap.get(compId);
            CommissionPolicy pol = dailyPolCache.get(compId);
            if (pol == null) {
                pol = commissionService.resolveCommissionPolicyForSettlement(compId);
                dailyPolCache.put(compId, pol);
            }
            PgTrnsctn t = new PgTrnsctn();
            t.setMerchantId(compId);
            t.setStatus(st);
            t.setCurType(curRaw);
            t.setAmtKrw(amt);
            String payCurKey = PayListItemDto.payCurKeyForFeeCompute(t, ctx);
            FeeListTxnAmountService.FeeListTxnAmounts amts = feeListTxnAmountService.compute(
                    t, ctx, pol, payCurKey, dailyFeeResolver, dailyMonthCbCache, dailyTiersByPolicyId);
            agg.totalFeeSum.merge(cur, amts.totalFee(), BigDecimal::add);
            agg.holdSum.merge(cur, amts.rollingHoldEst(), BigDecimal::add);
            agg.vatSum.merge(cur, amts.feeVat(), BigDecimal::add);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (LocalDate d = effectiveTo; !d.isBefore(rangeFrom); d = d.minusDays(1)) {
            PayDayFinancialAgg agg = byDay.getOrDefault(d, new PayDayFinancialAgg());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("day", d.toString());
            row.put("txnCount", agg.txnCount);
            row.put("statusBucketCounts", new LinkedHashMap<>(agg.bucketCount));
            row.put("payListFinancialSummary", agg.toPayListFinancialSummaryMap(
                    primaryNorm, allowedCur, effectiveMultiCurrency, baseCurrencyConfigured, currencyOrderTemplate));
            out.add(row);
        }
        applyDailySummaryDayListOrder(out, template != null ? template.getSearchOrderDir() : null);
        return out;
    }

    /** 일간결제 집계용 — {@link #computePayListFinancialSummary} 와 동일 규칙으로 일별 누적 */
    private static final class PayDayFinancialAgg {
        long txnCount;
        long successCount;
        final Map<String, Long> bucketCount = new HashMap<>();
        final Map<String, BigDecimal> totalTxn = new HashMap<>();
        final Map<String, BigDecimal> approve = new HashMap<>();
        final Map<String, BigDecimal> cancel = new HashMap<>();
        final Map<String, Long> approveCountByCur = new HashMap<>();
        final Map<String, Long> cancelCountByCur = new HashMap<>();
        final Map<String, BigDecimal> totalFeeSum = new HashMap<>();
        final Map<String, BigDecimal> holdSum = new HashMap<>();
        final Map<String, BigDecimal> vatSum = new HashMap<>();

        Map<String, Object> toPayListFinancialSummaryMap(String primaryNorm,
                                                          Set<String> allowedCur,
                                                          boolean effectiveMultiCurrency,
                                                          boolean baseCurrencyConfigured,
                                                          List<String> currencyOrderTemplate) {
            List<String> currencyOrder = new ArrayList<>(currencyOrderTemplate);
            if (!baseCurrencyConfigured) {
                currencyOrder.clear();
                Set<String> union = new HashSet<>();
                union.addAll(totalTxn.keySet());
                union.addAll(approve.keySet());
                union.addAll(cancel.keySet());
                union.addAll(totalFeeSum.keySet());
                union.addAll(holdSum.keySet());
                union.addAll(vatSum.keySet());
                currencyOrder.addAll(union);
                PayListStatusBarBuckets.sortCurrencyCodes(currencyOrder);
            }
            if (currencyOrder.isEmpty()) {
                currencyOrder.add(primaryNorm);
            }
            return packPayListFinancialSummaryPayload(totalTxn, approve, cancel, approveCountByCur, cancelCountByCur,
                    totalFeeSum, holdSum, vatSum, successCount, false, primaryNorm, currencyOrder, effectiveMultiCurrency);
        }
    }

    private Predicate variantPredicate(jakarta.persistence.criteria.Root<PgTrnsctn> root,
                                       jakarta.persistence.criteria.CriteriaBuilder cb, String variant) {
        return switch (variant) {
            case "INTEGRATED" -> cb.conjunction();
            case "SUCCESS" -> cb.equal(root.get("status"), "10");
            case "FAIL" -> root.get("status").in("F0", "99");
            case "REFUND" -> root.get("status").in("30", "42");
            case "FORCE_REFUND" -> cb.equal(root.get("status"), "31");
            case "CANCEL" -> cb.equal(root.get("status"), "20");
            case "VOID" -> root.get("status").in("21", "40");
            case "MANUAL_VOID" -> root.get("status").in("22", "41");
            case "OFFSET_CANCEL" -> cb.and(
                    cb.equal(cb.upper(cb.trim(cb.coalesce(root.get("settledYn"), cb.literal("N")))), "Y"),
                    root.get("status").in("20", "21", "22", "30", "31", "40", "41", "42"));
            case "URL_PAY" -> cb.equal(root.get("origin"), "URL");
            case "CHATBOT_PAY" -> cb.equal(root.get("origin"), "CHATBOT");
            case "NOTI" -> cb.equal(root.get("origin"), "NOTI");
            default -> cb.conjunction();
        };
    }
}
