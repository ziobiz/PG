package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.CommissionHistory;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.MerchantCommissionExtra;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.CommissionHistoryRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.MerchantCommissionExtraRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.CommissionTierJsonHelper;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.PercentDecimalHelper;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CommissionService {

    /** LocalDate 등 스냅샷 직렬화 + 수수료관리 이력 JSON 안정화 */
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 수수료관리 OTP 유효 시간(분) — 마지막 수수료 관련 활동 기준 슬라이딩 */
    public static final int COMMISSION_OTP_GRACE_MINUTES = 10;
    private static final long COMMISSION_OTP_GRACE_MS = COMMISSION_OTP_GRACE_MINUTES * 60L * 1000L;

    /** 사용자별 마지막 수수료 OTP 통과·활동 시각(ms) */
    private final ConcurrentHashMap<String, Long> commissionOtpPassedAtMs = new ConcurrentHashMap<>();

    private final OrgUnitRepository orgUnitRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final CommissionHistoryRepository commissionHistoryRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final OrgUnitChangeAuditService orgUnitChangeAuditService;
    private final AuthService authService;

    public CommissionService(OrgUnitRepository orgUnitRepository,
                             CommissionPolicyRepository commissionPolicyRepository,
                             MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                             MerchantProfileRepository merchantProfileRepository,
                             CommissionHistoryRepository commissionHistoryRepository,
                             DistributionFeeConfigRepository distributionFeeConfigRepository,
                             OrgUnitChangeAuditService orgUnitChangeAuditService,
                             AuthService authService) {
        this.orgUnitRepository = orgUnitRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.commissionHistoryRepository = commissionHistoryRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.orgUnitChangeAuditService = orgUnitChangeAuditService;
        this.authService = authService;
    }

    /**
     * OrgUnit.code 앞뒤 공백이 있으면 저장 시 {@code DistributionFeeConfig.comp_id}는 trim 되는데
     * 조회는 공백 포함으로 하면 행을 못 찾아 본사/기본 배분으로 보이며 "저장이 안 된 것처럼" 보인다.
     */
    private static String normCompCode(String code) {
        return code != null ? code.trim() : "";
    }

    private static String normCompCode(OrgUnit ou) {
        return ou == null ? "" : normCompCode(ou.getCode());
    }

    private Optional<OrgUnit> resolveOrgByCode(String compId) {
        String c = compId != null ? compId.trim() : "";
        if (c.isEmpty()) {
            return Optional.empty();
        }
        return orgUnitRepository.findByCode(c).or(() -> orgUnitRepository.findByCodeIgnoreCase(c));
    }

    public PageResult<Map<String, Object>> search(String searchCompId, String searchCompNm, String searchCompDiv,
                                                  String searchPolicyCur, String useYn, int page, int size) {
        List<OrgUnit> all = orgUnitRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
        Map<Long, OrgUnit> orgById = all.stream()
                .filter(o -> o.getId() != null)
                .collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        List<OrgUnit> filtered = all.stream()
                .filter(o -> o.getOrgLevel() == OrgLevel.MERCHANT)
                .filter(o -> matchUseYn(o, useYn))
                .filter(o -> matchCommissionSearchTarget(o, searchCompId, searchCompNm, searchCompDiv))
                .filter(o -> matchCommissionPolicyOrBaseCurrency(o, searchPolicyCur))
                .collect(Collectors.toList());
        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<OrgUnit> pageList = start < total ? filtered.subList(start, end) : new ArrayList<>();

        List<Map<String, Object>> list = new ArrayList<>();
        for (OrgUnit ou : pageList) {
            Map<String, Object> m = buildCommissionRow(ou);
            syncNamesFromCurrentOrgTree(m, ou, orgById);
            list.add(m);
        }

        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(total);
        pr.setTotalPages(size <= 0 ? 1 : (int) Math.ceil((double) Math.max(0, total) / size));
        return pr;
    }

    private Map<String, Object> buildCommissionRow(OrgUnit merchant) {
        return buildCommissionRow(merchant, true);
    }

    /**
     * @param preferLatestHistoryDistribution 목록·상세 표시 시 true — 최신 변경 이력 배분을 우선.
     *                                      저장 직후 스냅샷 생성 시 false — 방금 저장한 DB 배분을 사용.
     */
    private Map<String, Object> buildCommissionRow(OrgUnit merchant, boolean preferLatestHistoryDistribution) {
        Map<String, Object> m = new HashMap<>();
        String mc = normCompCode(merchant);
        m.put("id", merchant.getId());
        m.put("compId", mc.isEmpty() ? merchant.getCode() : mc);
        m.put("compNm", merchant.getName());
        m.put("compDiv", merchant.getOrgLevel() != null ? merchant.getOrgLevel().name() : "");
        m.put("useYn", resolveOrgUseYn(merchant));
        String regDt = merchant.getCreatedAt() != null ? merchant.getCreatedAt().toString().substring(0, 10) : null;
        m.put("regDt", regDt);
        fillAncestorNames(m, merchant);
        refreshAncestorNamesByCode(m);

        String effectiveScope = resolveMerchantHqScopeForDisplay(merchant);
        CommissionPolicy policy = resolveCommissionPolicyForSettlement(mc);
        if (policy != null) {
            m.put("cmsnRate", policy.getPayRate());
            m.put("perTxFee", policy.getPerTxFee());
            m.put("cancelRate", policy.getCancelRate());
            m.put("voidFeePerTx", policy.getVoidFeePerTx());
            m.put("manualVoidFeePerTx", policy.getManualVoidFeePerTx());
            m.put("payRate", policy.getPayRate());
            m.put("refundRate", policy.getRefundRate());
            m.put("feeSettlementPerTx", policy.getFeeSettlementPerTx());
            m.put("remittanceTransferFee", policy.getRemittanceTransferFee());
            m.put("usdtTransferFeeUsd", policy.getUsdtTransferFeeUsd());
            m.put("rollingPct", policy.getRollingPct());
            m.put("rollingDays", policy.getRollingDays());
        }
        {
            String polCur = "-";
            if (policy != null) {
                String cc = policy.getCurrencyCode();
                if (cc != null && !cc.isBlank()) {
                    polCur = PayListStatusBarBuckets.normalizeCurrency(cc.trim());
                }
            }
            m.put("policyCur", polCur);
        }
        merchantCommissionExtraRepository.findByOrgUnitId(merchant.getId()).ifPresent(ex -> {
            m.put("feeAccountActivation", ex.getFeeAccountActivation());
            m.put("feeAnnual", ex.getFeeAnnual());
            m.put("feeTechService", ex.getFeeTechService());
            m.put("feeSettlementPerTx", ex.getFeeSettlementPerTx());
            m.put("feeRefund", ex.getFeeRefund());
        });
        Optional<DistributionFeeConfig> odf = resolveDistributionFeeConfig(merchant, mc, effectiveScope);
        if (odf.isPresent() && !isDistributionConfigEffectivelyEmpty(odf.get())) {
            applyDistributionToMap(m, odf.get());
        } else {
            applyDistributionToMap(m, null);
            // 가맹점 직접입력(N)인데 배분 행이 아직 없으면, 정책의 결제율/건당을 기본 표시값으로 사용
            if (effectiveScope == null && policy != null) {
                applyDirectPolicyFallbackToDistribution(m, policy);
            }
        }
        /* 목록·상세: 최신 저장 이력 배분을 우선(본사정책 따름이어도 템플릿으로 조용히 덮지 않음) */
        if (preferLatestHistoryDistribution) {
            applyLatestCommissionHistoryDistributionForDisplay(mc, m);
        }

        m.put("totalNm", resolveMerchantBaseCurrencyDisplay(merchant));
        putTotals(m);
        LocalDate apply = null;
        if (m.get("applyStartDate") instanceof LocalDate d) {
            apply = d;
        } else if (m.get("applyStartDate") != null) {
            try {
                String s = m.get("applyStartDate").toString().trim();
                if (s.length() >= 10) {
                    apply = LocalDate.parse(s.substring(0, 10));
                }
            } catch (Exception ignored) {
                apply = null;
            }
        }
        m.put("applyDt", apply != null ? apply.toString() : regDt);
        return m;
    }

    private void applyDirectPolicyFallbackToDistribution(Map<String, Object> m, CommissionPolicy policy) {
        if (m == null || policy == null) return;
        m.put("hqRate", policy.getPayRate() != null ? policy.getPayRate() : BigDecimal.ZERO);
        m.put("hqPerTxFee", policy.getPerTxFee() != null ? policy.getPerTxFee() : BigDecimal.ZERO);
        m.put("regionalRate", BigDecimal.ZERO);
        m.put("masterRate", BigDecimal.ZERO);
        m.put("branchRate", BigDecimal.ZERO);
        m.put("agencyRate", BigDecimal.ZERO);
        m.put("salesOfficeRate", BigDecimal.ZERO);
        m.put("regionalPerTxFee", BigDecimal.ZERO);
        m.put("masterPerTxFee", BigDecimal.ZERO);
        m.put("branchPerTxFee", BigDecimal.ZERO);
        m.put("agencyPerTxFee", BigDecimal.ZERO);
        m.put("salesOfficePerTxFee", BigDecimal.ZERO);
    }

    /**
     * 본사정책 따름({@code effectiveScope} 지정): 선택한 HQ 템플릿 배분을 우선한다.
     * (가맹에 이전 정책 배분이 남아 있거나 변경이력이 있어도, 정책선택 변경이 수수료관리에 바로 반영되도록)
     * 직접입력: 가맹 배분 행을 쓰고, 비어 있으면 템플릿 폴백.
     */
    /**
     * 가맹에 저장된 배분(DF)을 최우선. 비어 있을 때만 HQ 템플릿(본사정책 따름)을 폴백한다.
     * (과거: Follow HQ 시 템플릿을 먼저 써서 목록만 기본수수료로 보이는 표시 버그가 있었음)
     */
    private Optional<DistributionFeeConfig> resolveDistributionFeeConfig(OrgUnit merchant, String merchantCode,
                                                                         String effectiveScope) {
        String mc = normCompCode(merchantCode);
        String rawCode = merchant != null ? merchant.getCode() : null;
        Optional<DistributionFeeConfig> merchantDf = findDistributionByCompIdCandidates(mc, rawCode);
        if (merchantDf.isPresent() && !isDistributionConfigEffectivelyEmpty(merchantDf.get())) {
            return merchantDf;
        }
        if (effectiveScope != null && !effectiveScope.isBlank() && !effectiveScope.equalsIgnoreCase(mc)) {
            Optional<DistributionFeeConfig> fromHq = distributionFromHqTemplateScope(effectiveScope);
            if (fromHq.isPresent()) {
                return fromHq;
            }
        }
        return merchantDf;
    }

    /** HQ 템플릿 scope → 배분(DF 행 또는 tier_commission_json). 미저장 임시 DF일 수 있음. */
    private Optional<DistributionFeeConfig> distributionFromHqTemplateScope(String templateScope) {
        if (templateScope == null || templateScope.isBlank()) {
            return Optional.empty();
        }
        String scopeKey = normCompCode(templateScope);
        Optional<DistributionFeeConfig> scopeDf = distributionFeeConfigRepository.findByCompId(scopeKey);
        if (scopeDf.isEmpty() && !scopeKey.equals(templateScope.trim())) {
            scopeDf = distributionFeeConfigRepository.findByCompId(templateScope.trim());
        }
        if (scopeDf.isPresent() && !isDistributionConfigEffectivelyEmpty(scopeDf.get())) {
            return scopeDf;
        }
        Optional<CommissionPolicy> pol = commissionPolicyRepository.findByScope(templateScope.trim());
        if (pol.isEmpty() && !scopeKey.equals(templateScope.trim())) {
            pol = commissionPolicyRepository.findByScope(scopeKey);
        }
        if (pol.isEmpty()) {
            return Optional.empty();
        }
        CommissionPolicy p = pol.get();
        String json = p.getTierCommissionJson();
        if (json == null || json.isBlank()) {
            json = CommissionTierJsonHelper.buildTierJsonFromPolicyScalars(p);
        }
        DistributionFeeConfig df = new DistributionFeeConfig();
        df.setCompId(templateScope.trim());
        CommissionTierJsonHelper.applyTierJsonToDistribution(json, df);
        if (isDistributionConfigEffectivelyEmpty(df)) {
            return Optional.empty();
        }
        return Optional.of(df);
    }

    private Optional<DistributionFeeConfig> findDistributionByCompIdCandidates(String mc, String rawCode) {
        if (mc == null || mc.isEmpty()) {
            return Optional.empty();
        }
        Optional<DistributionFeeConfig> odf = distributionFeeConfigRepository.findByCompId(mc);
        if (odf.isEmpty() && rawCode != null) {
            String raw = normCompCode(rawCode);
            if (!raw.isEmpty() && !raw.equals(mc)) {
                odf = distributionFeeConfigRepository.findByCompId(raw);
            }
            if (odf.isEmpty() && !rawCode.equals(raw)) {
                odf = distributionFeeConfigRepository.findByCompId(rawCode);
            }
        }
        return odf;
    }

    private static boolean isDistributionConfigEffectivelyEmpty(DistributionFeeConfig df) {
        if (df == null) {
            return true;
        }
        return isZeroOrNull(df.getHqRate())
                && isZeroOrNull(df.getRegionalRate())
                && isZeroOrNull(df.getMasterRate())
                && isZeroOrNull(df.getBranchRate())
                && isZeroOrNull(df.getAgencyRate())
                && isZeroOrNull(df.getSalesOfficeRate())
                && isZeroOrNull(df.getHqPerTxFee())
                && isZeroOrNull(df.getRegionalPerTxFee())
                && isZeroOrNull(df.getMasterPerTxFee())
                && isZeroOrNull(df.getBranchPerTxFee())
                && isZeroOrNull(df.getAgencyPerTxFee())
                && isZeroOrNull(df.getSalesOfficePerTxFee());
    }

    private static boolean isDistributionMapEffectivelyEmpty(Map<String, Object> m) {
        if (m == null || m.isEmpty()) {
            return true;
        }
        String[] keys = {
                "hqRate", "regionalRate", "masterRate", "branchRate", "agencyRate", "salesOfficeRate",
                "hqPerTxFee", "regionalPerTxFee", "masterPerTxFee", "branchPerTxFee", "agencyPerTxFee", "salesOfficePerTxFee"
        };
        for (String k : keys) {
            if (!isZeroOrNull(m.get(k))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isZeroOrNull(Object v) {
        if (v == null) {
            return true;
        }
        return bd(v).compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 수수료관리 목록·상세 표시: 최신 변경 이력 스냅샷의 단계별 배분(요율·건당)을 우선한다.
     * DB/템플릿만 보면 총본사 %만 있고 본사 %가 빠져 합계가 히스토리와 어긋날 수 있다.
     */
    private void applyLatestCommissionHistoryDistributionForDisplay(String merchantCode, Map<String, Object> m) {
        if (merchantCode == null || merchantCode.isBlank() || m == null) {
            return;
        }
        List<CommissionHistory> desc = commissionHistoryRepository.findByCompIdIgnoreCaseOrderByCreatedAtDesc(merchantCode.trim());
        if (desc == null || desc.isEmpty()) {
            return;
        }
        CommissionHistory latest = desc.get(0);
        putLatestCommissionChangeMeta(latest, m);
        Map<String, Object> snap = parseHistorySnapshotMap(latest);
        if (snap == null || snap.isEmpty() || isDistributionMapEffectivelyEmpty(snap)) {
            return;
        }
        copyDistributionFieldsFromMap(snap, m);
    }

    /** 목록 하이라이트용: 최신 수수료 변경일시·금일 수정 여부(Asia/Seoul 기준) */
    private static void putLatestCommissionChangeMeta(CommissionHistory latest, Map<String, Object> m) {
        if (latest == null || m == null || latest.getCreatedAt() == null) {
            return;
        }
        LocalDateTime at = latest.getCreatedAt();
        m.put("lastChgDt", DT_FMT.format(at));
        LocalDate todaySeoul = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        m.put("changedTodayYn", at.toLocalDate().equals(todaySeoul) ? "Y" : "N");
    }

    private static void copyDistributionFieldsFromMap(Map<String, Object> src, Map<String, Object> dest) {
        if (src == null || dest == null) {
            return;
        }
        String[] keys = {
                "hqRate", "regionalRate", "masterRate", "branchRate", "agencyRate", "salesOfficeRate",
                "hqPerTxFee", "regionalPerTxFee", "masterPerTxFee", "branchPerTxFee", "agencyPerTxFee", "salesOfficePerTxFee",
                "applyStartDate", "applyStartDateStr"
        };
        for (String k : keys) {
            if (src.containsKey(k) && src.get(k) != null) {
                dest.put(k, src.get(k));
            }
        }
    }

    private String resolveMerchantHqScopeForDisplay(OrgUnit merchant) {
        if (merchant == null) return "DEFAULT";
        Long orgUnitId = merchant.getId();
        if (orgUnitId == null) return "DEFAULT";
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isEmpty()) return "DEFAULT";
        String rs = mp.get().getRegionalSettings();
        if (rs == null || rs.isBlank()) return "DEFAULT";
        try {
            Map<String, Object> obj = MAPPER.readValue(rs, new TypeReference<>() {});
            String follow = obj.get("commissionFollowHq") != null ? String.valueOf(obj.get("commissionFollowHq")).trim() : "Y";
            if ("N".equalsIgnoreCase(follow)) return null;
            String scope = obj.get("hqPolicyScope") != null ? String.valueOf(obj.get("hqPolicyScope")).trim() : "";
            return scope.isEmpty() ? "DEFAULT" : scope;
        } catch (Exception e) {
            return "DEFAULT";
        }
    }

    /**
     * 정산 집계·정산료·수수료내역 건별 합산에 쓰는 수수료정책.
     * <p>본사정책 따름(Y)이면 {@code hqPolicyScope}(비면 DEFAULT)에 해당하는 <b>배포 템플릿</b> 행을 사용합니다.
     * 가맹 스코프({@code tb_commission_policy.scope=가맹코드}) 행이 남아 있어도 본사 목록과 동일한 값이 오도록,
     * 직접입력(N)일 때만 가맹 스코프를 봅니다.</p>
     * <p>가맹별 단계별 배분({@link DistributionFeeConfig}·변경 이력) 합계가 있으면
     * {@code payRate}·{@code perTxFee}에 반영합니다(수수료내역 결제%·건당과 수수료관리 합계 일치).</p>
     */
    public CommissionPolicy resolveCommissionPolicyForSettlement(String merchantCode) {
        CommissionPolicy base = resolveBaseCommissionPolicyTemplate(merchantCode);
        String mc = merchantCode != null ? merchantCode.trim() : "";
        if (mc.isEmpty()) {
            return base;
        }
        Optional<OrgUnit> ouOpt = resolveOrgByCode(mc);
        if (ouOpt.isEmpty() || ouOpt.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return base;
        }
        return overlayMerchantDistributionRatesOnPolicy(ouOpt.get(), mc, base);
    }

    /** 본사 템플릿 또는 가맹 직접입력 정책(배분 합계 미반영). */
    private CommissionPolicy resolveBaseCommissionPolicyTemplate(String merchantCode) {
        String mc = merchantCode != null ? merchantCode.trim() : "";
        if (mc.isEmpty()) {
            return commissionPolicyRepository.findByScope("DEFAULT").orElseGet(CommissionPolicy::new);
        }
        Optional<OrgUnit> ouOpt = resolveOrgByCode(mc);
        if (ouOpt.isEmpty() || ouOpt.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return commissionPolicyRepository.findByScope("DEFAULT").orElseGet(CommissionPolicy::new);
        }
        OrgUnit merchant = ouOpt.get();
        boolean followHq = true;
        String hqScope = "";
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(merchant.getId());
        if (mpOpt.isPresent()) {
            String rs = mpOpt.get().getRegionalSettings();
            if (rs != null && !rs.isBlank()) {
                try {
                    Map<String, Object> obj = MAPPER.readValue(rs, new TypeReference<>() {});
                    String follow = obj.get("commissionFollowHq") != null ? String.valueOf(obj.get("commissionFollowHq")).trim() : "Y";
                    followHq = !"N".equalsIgnoreCase(follow);
                    hqScope = obj.get("hqPolicyScope") != null ? String.valueOf(obj.get("hqPolicyScope")).trim() : "";
                } catch (Exception ignored) {
                    followHq = true;
                    hqScope = "";
                }
            }
        }
        if (!followHq) {
            return commissionPolicyRepository.findByScope(normCompCode(merchant))
                    .or(() -> commissionPolicyRepository.findByScope(mc))
                    .or(() -> commissionPolicyRepository.findByScope("DEFAULT"))
                    .orElseGet(CommissionPolicy::new);
        }
        String templateScope = hqScope.isEmpty() ? "DEFAULT" : hqScope;
        return commissionPolicyRepository.findByScope(templateScope)
                .or(() -> commissionPolicyRepository.findByScope("DEFAULT"))
                .orElseGet(CommissionPolicy::new);
    }

    /** 가맹 단계별 배분(최신 변경 이력 우선) 합계 → 결제 수수료율·건당 수수료. */
    private CommissionPolicy overlayMerchantDistributionRatesOnPolicy(OrgUnit merchant, String mc, CommissionPolicy base) {
        Map<String, Object> dist = resolveEffectiveDistributionMap(merchant, mc, true);
        if (isDistributionMapEffectivelyEmpty(dist)) {
            return base;
        }
        putTotals(dist);
        CommissionPolicy effective = cloneCommissionPolicyForSettlement(base);
        effective.setPayRate(bd(dist.get("totalRate")));
        effective.setPerTxFee(bd(dist.get("totalPerTxFee")));
        return effective;
    }

    private Map<String, Object> resolveEffectiveDistributionMap(OrgUnit merchant, String mc, boolean preferLatestHistory) {
        Map<String, Object> m = new HashMap<>();
        String effectiveScope = resolveMerchantHqScopeForDisplay(merchant);
        Optional<DistributionFeeConfig> odf = resolveDistributionFeeConfig(merchant, mc, effectiveScope);
        if (odf.isPresent() && !isDistributionConfigEffectivelyEmpty(odf.get())) {
            applyDistributionToMap(m, odf.get());
        } else {
            applyDistributionToMap(m, null);
            if (effectiveScope == null) {
                CommissionPolicy pol = resolveBaseCommissionPolicyTemplate(mc);
                if (pol != null) {
                    applyDirectPolicyFallbackToDistribution(m, pol);
                }
            }
        }
        if (preferLatestHistory) {
            /* 저장 이력 배분 우선 — 본사정책 따름이어도 템플릿으로 덮어 보이지 않음 */
            applyLatestCommissionHistoryDistributionForDisplay(mc, m);
        }
        return m;
    }

    private static CommissionPolicy cloneCommissionPolicyForSettlement(CommissionPolicy base) {
        if (base == null) {
            return new CommissionPolicy();
        }
        CommissionPolicy c = new CommissionPolicy();
        c.setScope(base.getScope());
        c.setPolicyName(base.getPolicyName());
        c.setDeployYn(base.getDeployYn());
        c.setPerTxFee(base.getPerTxFee());
        c.setUsageRate(base.getUsageRate());
        c.setFailFee(base.getFailFee());
        c.setCancelRate(base.getCancelRate());
        c.setVoidFeePerTx(base.getVoidFeePerTx());
        c.setManualVoidFeePerTx(base.getManualVoidFeePerTx());
        c.setRefundRate(base.getRefundRate());
        c.setPayRate(base.getPayRate());
        c.setFeeSettlementPerTx(base.getFeeSettlementPerTx());
        c.setRemittanceTransferFee(base.getRemittanceTransferFee());
        c.setUsdtTransferFeeUsd(base.getUsdtTransferFeeUsd());
        c.setFeeUsdt(base.getFeeUsdt());
        c.setFeeFx(base.getFeeFx());
        c.setRollingPct(base.getRollingPct());
        c.setRollingDays(base.getRollingDays());
        c.setCurrencyCode(base.getCurrencyCode());
        c.setPolicyRemark(base.getPolicyRemark());
        c.setFee3dsRate(base.getFee3dsRate());
        c.setChargebackFeePerTx(base.getChargebackFeePerTx());
        c.setChargebackPolicyId(base.getChargebackPolicyId());
        c.setVoidSettlementMode(base.getVoidSettlementMode());
        c.setManualVoidSettlementMode(base.getManualVoidSettlementMode());
        c.setRefundSettlementMode(base.getRefundSettlementMode());
        c.setForceRefundSettlementMode(base.getForceRefundSettlementMode());
        c.setExtraFee1Name(base.getExtraFee1Name());
        c.setExtraFee1Mode(base.getExtraFee1Mode());
        c.setExtraFee1Value(base.getExtraFee1Value());
        c.setExtraFee2Name(base.getExtraFee2Name());
        c.setExtraFee2Mode(base.getExtraFee2Mode());
        c.setExtraFee2Value(base.getExtraFee2Value());
        c.setExtraFee3Name(base.getExtraFee3Name());
        c.setExtraFee3Mode(base.getExtraFee3Mode());
        c.setExtraFee3Value(base.getExtraFee3Value());
        c.setExtraFee4Name(base.getExtraFee4Name());
        c.setExtraFee4Mode(base.getExtraFee4Mode());
        c.setExtraFee4Value(base.getExtraFee4Value());
        return c;
    }

    private static void syncPolicyPayFieldsFromDistribution(CommissionPolicy policy, DistributionFeeConfig df) {
        if (policy == null || df == null) {
            return;
        }
        BigDecimal totalRate = bd(df.getHqRate())
                .add(bd(df.getRegionalRate()))
                .add(bd(df.getMasterRate()))
                .add(bd(df.getBranchRate()))
                .add(bd(df.getAgencyRate()))
                .add(bd(df.getSalesOfficeRate()));
        BigDecimal totalPerTx = bd(df.getHqPerTxFee())
                .add(bd(df.getRegionalPerTxFee()))
                .add(bd(df.getMasterPerTxFee()))
                .add(bd(df.getBranchPerTxFee()))
                .add(bd(df.getAgencyPerTxFee()))
                .add(bd(df.getSalesOfficePerTxFee()));
        if (totalRate.signum() > 0 || !isDistributionConfigEffectivelyEmpty(df)) {
            policy.setPayRate(totalRate);
        }
        if (totalPerTx.signum() > 0 || !isDistributionConfigEffectivelyEmpty(df)) {
            policy.setPerTxFee(totalPerTx);
        }
    }

    private void applyDistributionToMap(Map<String, Object> m, DistributionFeeConfig df) {
        if (df == null) {
            putZeroDistribution(m);
            return;
        }
        m.put("hqRate", df.getHqRate());
        m.put("regionalRate", df.getRegionalRate());
        m.put("masterRate", df.getMasterRate());
        m.put("branchRate", df.getBranchRate());
        m.put("agencyRate", df.getAgencyRate());
        m.put("salesOfficeRate", df.getSalesOfficeRate());
        m.put("hqPerTxFee", df.getHqPerTxFee());
        m.put("regionalPerTxFee", df.getRegionalPerTxFee());
        m.put("masterPerTxFee", df.getMasterPerTxFee());
        m.put("branchPerTxFee", df.getBranchPerTxFee());
        m.put("agencyPerTxFee", df.getAgencyPerTxFee());
        m.put("salesOfficePerTxFee", df.getSalesOfficePerTxFee());
        m.put("applyStartDate", df.getApplyStartDate());
    }

    private void putZeroDistribution(Map<String, Object> m) {
        m.put("hqRate", BigDecimal.ZERO);
        m.put("regionalRate", BigDecimal.ZERO);
        m.put("masterRate", BigDecimal.ZERO);
        m.put("branchRate", BigDecimal.ZERO);
        m.put("agencyRate", BigDecimal.ZERO);
        m.put("salesOfficeRate", BigDecimal.ZERO);
        m.put("hqPerTxFee", BigDecimal.ZERO);
        m.put("regionalPerTxFee", BigDecimal.ZERO);
        m.put("masterPerTxFee", BigDecimal.ZERO);
        m.put("branchPerTxFee", BigDecimal.ZERO);
        m.put("agencyPerTxFee", BigDecimal.ZERO);
        m.put("salesOfficePerTxFee", BigDecimal.ZERO);
        m.put("applyStartDate", null);
    }

    private void putTotals(Map<String, Object> m) {
        BigDecimal tr = bd(m.get("hqRate"))
                .add(bd(m.get("regionalRate")))
                .add(bd(m.get("masterRate")))
                .add(bd(m.get("branchRate")))
                .add(bd(m.get("agencyRate")))
                .add(bd(m.get("salesOfficeRate")));
        BigDecimal tt = bd(m.get("hqPerTxFee"))
                .add(bd(m.get("regionalPerTxFee")))
                .add(bd(m.get("masterPerTxFee")))
                .add(bd(m.get("branchPerTxFee")))
                .add(bd(m.get("agencyPerTxFee")))
                .add(bd(m.get("salesOfficePerTxFee")));
        m.put("totalRate", tr);
        m.put("totalPerTxFee", tt);
    }

    private static BigDecimal bd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        try {
            return new BigDecimal(o.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 가맹점에서 상위로 올라가며 각 조직단계별 업체명(가장 가까운 조상 한 건씩)
     */
    private void fillAncestorNames(Map<String, Object> m, OrgUnit merchant) {
        String hqNm = "", regionalNm = "", masterNm = "", branchNm = "", agencyNm = "", salesOfficeNm = "";
        String hqId = "", regionalId = "", masterId = "", branchId = "", agencyId = "", salesOfficeId = "";
        OrgUnit cur = merchant;
        for (int i = 0; i < 20 && cur != null; i++) {
            OrgLevel lv = cur.getOrgLevel();
            if (lv != null) {
                String nm = "";
                if (cur.getName() != null && !cur.getName().isBlank()) {
                    nm = cur.getName().trim();
                } else if (cur.getDomainSettingName() != null && !cur.getDomainSettingName().isBlank()) {
                    nm = cur.getDomainSettingName().trim();
                } else if (cur.getCode() != null) {
                    nm = cur.getCode();
                }
                String code = cur.getCode() != null ? cur.getCode() : "";
                switch (lv) {
                    case HEADQUARTERS -> {
                        if (hqNm.isEmpty()) hqNm = nm;
                        if (hqId.isEmpty()) hqId = code;
                    }
                    case REGIONAL -> {
                        if (regionalNm.isEmpty()) regionalNm = nm;
                        if (regionalId.isEmpty()) regionalId = code;
                    }
                    case MASTER_DIST -> {
                        if (masterNm.isEmpty()) masterNm = nm;
                        if (masterId.isEmpty()) masterId = code;
                    }
                    case BRANCH -> {
                        if (branchNm.isEmpty()) branchNm = nm;
                        if (branchId.isEmpty()) branchId = code;
                    }
                    case AGENCY -> {
                        if (agencyNm.isEmpty()) agencyNm = nm;
                        if (agencyId.isEmpty()) agencyId = code;
                    }
                    case SALES_OFFICE -> {
                        if (salesOfficeNm.isEmpty()) salesOfficeNm = nm;
                        if (salesOfficeId.isEmpty()) salesOfficeId = code;
                    }
                    default -> {
                    }
                }
            }
            Long parentId = cur.getParentId();
            cur = parentId != null ? orgUnitRepository.findById(parentId).orElse(null) : null;
        }
        m.put("hqNm", hqNm);
        m.put("regionalNm", regionalNm);
        m.put("masterNm", masterNm);
        m.put("branchNm", branchNm);
        m.put("agencyNm", agencyNm);
        m.put("salesOfficeNm", salesOfficeNm);
        m.put("hqId", hqId);
        m.put("regionalId", regionalId);
        m.put("masterId", masterId);
        m.put("branchId", branchId);
        m.put("agencyId", agencyId);
        m.put("salesOfficeId", salesOfficeId);
    }

    /**
     * 수수료관리의 상위 조직 업체명은 항상 현재 OrgUnit.name 기준으로 보정한다.
     * (이전 화면 캐시/과거 스냅샷 영향 제거)
     */
    private void refreshAncestorNamesByCode(Map<String, Object> m) {
        if (m == null) return;
        String[][] pairs = new String[][] {
                {"hqId", "hqNm"},
                {"regionalId", "regionalNm"},
                {"masterId", "masterNm"},
                {"branchId", "branchNm"},
                {"agencyId", "agencyNm"},
                {"salesOfficeId", "salesOfficeNm"}
        };
        for (String[] p : pairs) {
            String codeKey = p[0];
            String nameKey = p[1];
            Object codeObj = m.get(codeKey);
            String code = codeObj != null ? String.valueOf(codeObj).trim() : "";
            if (code.isEmpty()) continue;
            orgUnitRepository.findByCode(code).ifPresent(ou -> {
                String nm = ou.getName() != null ? ou.getName().trim() : "";
                if (!nm.isEmpty()) m.put(nameKey, nm);
            });
        }
    }

    /**
     * 검색 응답 직전에 조직 트리를 다시 타며 현재 업체명을 최종 동기화한다.
     * (업체코드/parentId 기반 단일 소스 보장)
     */
    private void syncNamesFromCurrentOrgTree(Map<String, Object> m, OrgUnit merchant, Map<Long, OrgUnit> orgById) {
        if (m == null || merchant == null || orgById == null || orgById.isEmpty()) return;
        if (merchant.getName() != null && !merchant.getName().isBlank()) {
            m.put("compNm", merchant.getName().trim());
        }

        String hqNm = "", regionalNm = "", masterNm = "", branchNm = "", agencyNm = "", salesOfficeNm = "";
        String hqId = "", regionalId = "", masterId = "", branchId = "", agencyId = "", salesOfficeId = "";

        OrgUnit cur = merchant;
        for (int i = 0; i < 20 && cur != null; i++) {
            OrgLevel lv = cur.getOrgLevel();
            String nm = cur.getName() != null ? cur.getName().trim() : "";
            String code = cur.getCode() != null ? cur.getCode().trim() : "";
            if (lv != null) {
                switch (lv) {
                    case HEADQUARTERS -> {
                        if (hqId.isEmpty()) hqId = code;
                        if (hqNm.isEmpty()) hqNm = !nm.isEmpty() ? nm : code;
                    }
                    case REGIONAL -> {
                        if (regionalId.isEmpty()) regionalId = code;
                        if (regionalNm.isEmpty()) regionalNm = !nm.isEmpty() ? nm : code;
                    }
                    case MASTER_DIST -> {
                        if (masterId.isEmpty()) masterId = code;
                        if (masterNm.isEmpty()) masterNm = !nm.isEmpty() ? nm : code;
                    }
                    case BRANCH -> {
                        if (branchId.isEmpty()) branchId = code;
                        if (branchNm.isEmpty()) branchNm = !nm.isEmpty() ? nm : code;
                    }
                    case AGENCY -> {
                        if (agencyId.isEmpty()) agencyId = code;
                        if (agencyNm.isEmpty()) agencyNm = !nm.isEmpty() ? nm : code;
                    }
                    case SALES_OFFICE -> {
                        if (salesOfficeId.isEmpty()) salesOfficeId = code;
                        if (salesOfficeNm.isEmpty()) salesOfficeNm = !nm.isEmpty() ? nm : code;
                    }
                    default -> {
                    }
                }
            }
            Long pid = cur.getParentId();
            cur = pid != null ? orgById.get(pid) : null;
        }

        m.put("hqId", hqId); m.put("hqNm", hqNm);
        m.put("regionalId", regionalId); m.put("regionalNm", regionalNm);
        m.put("masterId", masterId); m.put("masterNm", masterNm);
        m.put("branchId", branchId); m.put("branchNm", branchNm);
        m.put("agencyId", agencyId); m.put("agencyNm", agencyNm);
        m.put("salesOfficeId", salesOfficeId); m.put("salesOfficeNm", salesOfficeNm);
    }

    private String resolveOrgUseYn(OrgUnit orgUnit) {
        if (orgUnit == null) return "Y";
        Long orgUnitId = orgUnit.getId();
        if (orgUnitId == null) return "Y";
        String fromProfile = merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> {
                    String v = mp.getUseYn();
                    return normalizeStoredUseYn(v);
                })
                .orElse("");
        if (!fromProfile.isBlank()) return fromProfile;
        String status = orgUnit.getStatus() != null ? orgUnit.getStatus().trim().toUpperCase(Locale.ROOT) : "";
        if (!status.isBlank()) return "ACTIVE".equals(status) ? "Y" : "N";
        return "Y";
    }

    private static String normalizeStoredUseYn(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String t = raw.trim().toUpperCase(Locale.ROOT);
        if ("Y".equals(t) || "사용".equals(raw.trim())) return "Y";
        if ("N".equals(t) || "미사용".equals(raw.trim())) return "N";
        // 비표준 값은 미사용으로 간주 (운영 데이터 편차 대응)
        return "N";
    }

    private static boolean containsIgnoreCase(String src, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        if (src == null || src.isEmpty()) return false;
        return src.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private static String normalizeUseYnFilter(String useYn) {
        if (useYn == null || useYn.trim().isEmpty()) return "Y";
        String t = useYn.trim().toUpperCase(Locale.ROOT);
        if ("미사용".equals(useYn.trim())) return "N";
        return "N".equals(t) ? "N" : "Y";
    }

    private boolean matchUseYn(OrgUnit orgUnit, String useYn) {
        String f = normalizeUseYnFilter(useYn);
        return f.equals(resolveOrgUseYn(orgUnit));
    }

    /**
     * 적용 정책 통화(tb_commission_policy.currency_code) 또는 가맹 프로필 기준통화(CSV 가능)가 선택값과 같으면 통과.
     */
    private boolean matchCommissionPolicyOrBaseCurrency(OrgUnit merchant, String searchPolicyCurRaw) {
        if (searchPolicyCurRaw == null || searchPolicyCurRaw.isBlank()) {
            return true;
        }
        String want = PayListStatusBarBuckets.normalizeCurrency(searchPolicyCurRaw.trim());
        if (want.isEmpty()) {
            return true;
        }
        String mc = normCompCode(merchant);
        CommissionPolicy policy = resolveCommissionPolicyForSettlement(mc);
        if (policy != null && policy.getCurrencyCode() != null && !policy.getCurrencyCode().isBlank()) {
            String polCur = PayListStatusBarBuckets.normalizeCurrency(policy.getCurrencyCode().trim());
            if (want.equalsIgnoreCase(polCur)) {
                return true;
            }
        }
        for (String b : merchantBaseCurrencyAlphas(merchant)) {
            if (want.equalsIgnoreCase(b)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> merchantBaseCurrencyAlphas(OrgUnit merchant) {
        Set<String> out = new HashSet<>();
        if (merchant == null || merchant.getId() == null) {
            return out;
        }
        merchantProfileRepository.findByOrgUnitId(merchant.getId()).ifPresent(mp -> {
            String raw = mp.getBaseCurrency();
            if (raw == null || raw.isBlank()) {
                return;
            }
            for (String p : raw.split(",")) {
                String t = p != null ? p.trim() : "";
                if (t.isEmpty()) {
                    continue;
                }
                out.add(PayListStatusBarBuckets.normalizeCurrency(t));
            }
        });
        return out;
    }

    private boolean matchCommissionSearchTarget(OrgUnit merchant, String searchCompId, String searchCompNm, String searchCompDiv) {
        String div = searchCompDiv != null ? searchCompDiv.trim().toUpperCase(Locale.ROOT) : "";
        if (div.isEmpty() || "MERCHANT".equals(div)) {
            return containsIgnoreCase(merchant.getCode(), searchCompId)
                    && containsIgnoreCase(merchant.getName(), searchCompNm);
        }
        Map<String, Object> target = new HashMap<>();
        fillAncestorNames(target, merchant);
        String codeKey = switch (div) {
            case "HEADQUARTERS" -> "hqId";
            case "REGIONAL" -> "regionalId";
            case "MASTER_DIST" -> "masterId";
            case "BRANCH" -> "branchId";
            case "AGENCY" -> "agencyId";
            case "SALES_OFFICE" -> "salesOfficeId";
            default -> "";
        };
        String nameKey = switch (div) {
            case "HEADQUARTERS" -> "hqNm";
            case "REGIONAL" -> "regionalNm";
            case "MASTER_DIST" -> "masterNm";
            case "BRANCH" -> "branchNm";
            case "AGENCY" -> "agencyNm";
            case "SALES_OFFICE" -> "salesOfficeNm";
            default -> "";
        };
        String targetCode = codeKey.isEmpty() ? "" : String.valueOf(target.getOrDefault(codeKey, ""));
        String targetName = nameKey.isEmpty() ? "" : String.valueOf(target.getOrDefault(nameKey, ""));
        return containsIgnoreCase(targetCode, searchCompId)
                && containsIgnoreCase(targetName, searchCompNm);
    }

    public Optional<Map<String, Object>> getDetail(String compId) {
        return resolveOrgByCode(compId).map(ou -> {
            Map<String, Object> m = new HashMap<>();
            String mc = normCompCode(ou);
            m.put("compId", mc.isEmpty() ? ou.getCode() : mc);
            m.put("compNm", ou.getName());
            m.put("orgUnitId", ou.getId());
            CommissionPolicy policy = ou.getOrgLevel() == OrgLevel.MERCHANT
                    ? resolveCommissionPolicyForSettlement(mc)
                    : commissionPolicyRepository.findByScope(mc)
                            .or(() -> ou.getCode() != null && !mc.equals(ou.getCode())
                                    ? commissionPolicyRepository.findByScope(ou.getCode()) : Optional.empty())
                            .orElse(null);
            if (policy != null) {
                m.put("perTxFee", policy.getPerTxFee());
                m.put("cancelRate", policy.getCancelRate());
                m.put("voidFeePerTx", policy.getVoidFeePerTx());
                m.put("manualVoidFeePerTx", policy.getManualVoidFeePerTx());
                m.put("usageRate", policy.getUsageRate());
                m.put("failFee", policy.getFailFee());
                m.put("payRate", policy.getPayRate());
                m.put("refundRate", policy.getRefundRate());
                m.put("rollingPct", policy.getRollingPct());
                m.put("rollingDays", policy.getRollingDays());
                m.put("fee3dsRate", policy.getFee3dsRate());
                m.put("chargebackFeePerTx", policy.getChargebackFeePerTx());
                m.put("chargebackPolicyId", policy.getChargebackPolicyId());
                m.put("remittanceTransferFee", policy.getRemittanceTransferFee());
                m.put("usdtTransferFeeUsd", policy.getUsdtTransferFeeUsd());
                m.put("extraFee1Name", policy.getExtraFee1Name());
                m.put("extraFee1Mode", policy.getExtraFee1Mode());
                m.put("extraFee1Value", policy.getExtraFee1Value());
                m.put("extraFee2Name", policy.getExtraFee2Name());
                m.put("extraFee2Mode", policy.getExtraFee2Mode());
                m.put("extraFee2Value", policy.getExtraFee2Value());
                m.put("extraFee3Name", policy.getExtraFee3Name());
                m.put("extraFee3Mode", policy.getExtraFee3Mode());
                m.put("extraFee3Value", policy.getExtraFee3Value());
                m.put("extraFee4Name", policy.getExtraFee4Name());
                m.put("extraFee4Mode", policy.getExtraFee4Mode());
                m.put("extraFee4Value", policy.getExtraFee4Value());
            }
            merchantCommissionExtraRepository.findByOrgUnitId(ou.getId()).ifPresent(ex -> {
                m.put("feeAccountActivation", ex.getFeeAccountActivation());
                m.put("feeAnnual", ex.getFeeAnnual());
                m.put("feeTechService", ex.getFeeTechService());
                m.put("feeCancel", ex.getFeeCancel());
                m.put("feeInvalid", ex.getFeeInvalid());
                m.put("feeFail", ex.getFeeFail());
                m.put("feeUnpaid", ex.getFeeUnpaid());
                m.put("feeSettlementPerTx", ex.getFeeSettlementPerTx());
                m.put("feeUsdt", ex.getFeeUsdt());
                m.put("feeFx", ex.getFeeFx());
                m.put("feeRefund", ex.getFeeRefund());
                m.put("feeChargebackWarn", ex.getFeeChargebackWarn());
            });
            merchantProfileRepository.findByOrgUnitId(ou.getId()).ifPresent(mp -> {
                String rs = mp.getRegionalSettings();
                if (rs == null || rs.isBlank()) return;
                try {
                    Map<String, Object> obj = MAPPER.readValue(rs, new TypeReference<>() {});
                    if (obj.containsKey("hqPolicyScope")) {
                        Object hs = obj.get("hqPolicyScope");
                        m.put("hqPolicyScope", hs == null ? "" : String.valueOf(hs).trim());
                    }
                    if (obj.get("holdRate") != null) m.put("holdRate", obj.get("holdRate"));
                    if (obj.get("holdDays") != null) m.put("holdDays", obj.get("holdDays"));
                    if (obj.get("commissionFollowHq") != null) m.put("commissionFollowHq", String.valueOf(obj.get("commissionFollowHq")));
                } catch (Exception ignored) {
                }
            });
            String effectiveScope = resolveMerchantHqScopeForDisplay(ou);
            Optional<DistributionFeeConfig> odf2 = resolveDistributionFeeConfig(ou, mc, effectiveScope);
            if (odf2.isPresent() && !isDistributionConfigEffectivelyEmpty(odf2.get())) {
                applyDistributionToMap(m, odf2.get());
            } else {
                applyDistributionToMap(m, null);
                if (effectiveScope == null && policy != null) {
                    applyDirectPolicyFallbackToDistribution(m, policy);
                }
            }
            applyLatestCommissionHistoryDistributionForDisplay(mc, m);
            putTotals(m);
            if (m.get("applyStartDate") instanceof LocalDate d) m.put("applyStartDateStr", d.toString());
            else if (m.get("applyStartDate") != null) m.put("applyStartDateStr", m.get("applyStartDate").toString());
            return m;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean save(String compId, Map<String, Object> body) {
        return resolveOrgByCode(compId).map(ou -> {
            String merchantCode = normCompCode(ou);
            CommissionPolicy policy = commissionPolicyRepository.findByScope(merchantCode)
                    .or(() -> ou.getCode() != null && !merchantCode.equals(ou.getCode())
                            ? commissionPolicyRepository.findByScope(ou.getCode()) : Optional.empty())
                    .orElseGet(() -> {
                        CommissionPolicy p = new CommissionPolicy();
                        p.setScope(merchantCode);
                        return p;
                    });
            if (policy.getScope() != null && !normCompCode(policy.getScope()).equals(merchantCode)) {
                policy.setScope(merchantCode);
            }
            setAmtOne(policy::setPerTxFee, body.get("perTxFee"));
            setAmtOne(policy::setCancelRate, body.get("cancelRate"));
            setAmtOne(policy::setVoidFeePerTx, body.get("voidFeePerTx"));
            setAmtOne(policy::setManualVoidFeePerTx, body.get("manualVoidFeePerTx"));
            setAmtOne(policy::setUsageRate, body.get("usageRate"));
            setAmtOne(policy::setFailFee, body.get("failFee"));
            setPct(policy::setPayRate, body.get("payRate"));
            setAmtOne(policy::setRefundRate, body.get("refundRate"));
            setPct(policy::setRollingPct, body.get("rollingPct"));
            if (body.get("rollingDays") != null && !body.get("rollingDays").toString().isEmpty()) {
                policy.setRollingDays(Integer.parseInt(body.get("rollingDays").toString()));
            }
            setAmtOne(policy::setFee3dsRate, body.get("fee3dsRate"));
            setAmtOne(policy::setFeeSettlementPerTx, body.get("feeSettlementPerTx"));
            setAmtOne(policy::setRemittanceTransferFee, body.get("remittanceTransferFee"));
            setAmtOne(policy::setUsdtTransferFeeUsd, body.get("usdtTransferFeeUsd"));
            setPct(policy::setFeeUsdt, body.get("feeUsdt"));
            setPct(policy::setFeeFx, body.get("feeFx"));
            setAmtOne(policy::setChargebackFeePerTx, body.get("chargebackFeePerTx"));
            if (body.get("chargebackPolicyId") != null) {
                String cp = body.get("chargebackPolicyId").toString().trim();
                if (cp.isEmpty()) {
                    policy.setChargebackPolicyId(null);
                } else {
                    try {
                        policy.setChargebackPolicyId(Long.parseLong(cp));
                    } catch (Exception ignored) {
                    }
                }
            }
            applyExtraFeesFromCommissionBody(policy, body);

            DistributionFeeConfig df = distributionFeeConfigRepository.findByCompId(merchantCode)
                    .or(() -> ou.getCode() != null && !merchantCode.equals(ou.getCode())
                            ? distributionFeeConfigRepository.findByCompId(ou.getCode()) : Optional.empty())
                    .orElseGet(() -> {
                        DistributionFeeConfig x = new DistributionFeeConfig();
                        x.setCompId(merchantCode);
                        return x;
                    });
            if (df.getCompId() != null && !normCompCode(df.getCompId()).equals(merchantCode)) {
                df.setCompId(merchantCode);
            }
            setPct(df::setHqRate, body.get("hqRate"));
            setPct(df::setRegionalRate, body.get("regionalRate"));
            setPct(df::setMasterRate, body.get("masterRate"));
            setPct(df::setBranchRate, body.get("branchRate"));
            setPct(df::setAgencyRate, body.get("agencyRate"));
            setPct(df::setSalesOfficeRate, body.get("salesOfficeRate"));
            setBd(df::setHqPerTxFee, body.get("hqPerTxFee"));
            setBd(df::setRegionalPerTxFee, body.get("regionalPerTxFee"));
            setBd(df::setMasterPerTxFee, body.get("masterPerTxFee"));
            setBd(df::setBranchPerTxFee, body.get("branchPerTxFee"));
            setBd(df::setAgencyPerTxFee, body.get("agencyPerTxFee"));
            setBd(df::setSalesOfficePerTxFee, body.get("salesOfficePerTxFee"));
            if (body.get("applyStartDate") != null && !body.get("applyStartDate").toString().isBlank()) {
                try {
                    df.setApplyStartDate(LocalDate.parse(body.get("applyStartDate").toString().trim().substring(0, 10)));
                } catch (Exception ignored) {
                }
            }
            distributionFeeConfigRepository.save(df);
            syncPolicyPayFieldsFromDistribution(policy, df);
            commissionPolicyRepository.save(policy);

            MerchantCommissionExtra extra = merchantCommissionExtraRepository.findByOrgUnitId(ou.getId())
                    .orElseGet(() -> {
                        MerchantCommissionExtra e = new MerchantCommissionExtra();
                        e.setOrgUnitId(ou.getId());
                        return e;
                    });
            setBd(extra::setFeeAccountActivation, body.get("feeAccountActivation"));
            setBd(extra::setFeeAnnual, body.get("feeAnnual"));
            setBd(extra::setFeeTechService, body.get("feeTechService"));
            setBd(extra::setFeeSettlementPerTx, body.get("feeSettlementPerTx"));
            setBd(extra::setFeeRefund, body.get("feeRefund"));
            setPct(extra::setFeeUsdt, body.get("feeUsdt"));
            setPct(extra::setFeeFx, body.get("feeFx"));
            merchantCommissionExtraRepository.save(extra);

            Map<String, Object> snap = buildCommissionRow(ou, false);
            applyDistributionToMap(snap, df);
            putTotals(snap);
            snap.put("compNm", ou.getName());
            snap.put("compId", merchantCode);

            CommissionHistory hist = new CommissionHistory();
            hist.setCompId(merchantCode);
            hist.setChgType("COMMISSION");
            hist.setChgDesc("수수료 설정 변경 저장");
            hist.setChangedBy(currentUsername());
            try {
                hist.setSnapshotJson(MAPPER.writeValueAsString(snap));
            } catch (Exception e) {
                hist.setSnapshotJson("{}");
            }
            commissionHistoryRepository.save(hist);
            orgUnitChangeAuditService.appendIfChanged(ou.getId(), merchantCode,
                    ou.getName() != null ? ou.getName().trim() : "",
                    "[수수료관리] 수수료·배분 저장", "-", "저장 반영(상세: 수수료관리 히스토리)");
            return true;
        }).orElse(false);
    }

    /**
     * 가맹 등록에서 본사 수수료정책 템플릿을 적용한 뒤, 수수료관리와 동일한 이력 스냅샷을 남긴다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordHqTemplateApplyHistory(String merchantCode, String templateScope) {
        String mc = merchantCode != null ? merchantCode.trim() : "";
        if (mc.isEmpty()) {
            return;
        }
        Optional<OrgUnit> ouOpt = resolveOrgByCode(mc);
        if (ouOpt.isEmpty() || ouOpt.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return;
        }
        OrgUnit ou = ouOpt.get();
        String merchantNorm = normCompCode(ou);
        DistributionFeeConfig df = distributionFeeConfigRepository.findByCompId(merchantNorm)
                .or(() -> ou.getCode() != null && !merchantNorm.equals(ou.getCode())
                        ? distributionFeeConfigRepository.findByCompId(ou.getCode()) : Optional.empty())
                .orElse(null);
        Map<String, Object> snap = buildCommissionRow(ou, false);
        if (df != null && !isDistributionConfigEffectivelyEmpty(df)) {
            applyDistributionToMap(snap, df);
        }
        putTotals(snap);
        snap.put("compNm", ou.getName());
        snap.put("compId", merchantNorm);
        String scopeLabel = templateScope != null && !templateScope.isBlank() ? templateScope.trim() : "DEFAULT";
        CommissionHistory hist = new CommissionHistory();
        hist.setCompId(merchantNorm);
        hist.setChgType("COMMISSION");
        hist.setChgDesc("본사 수수료정책 템플릿 적용: " + scopeLabel);
        hist.setChangedBy(currentUsername());
        try {
            hist.setSnapshotJson(MAPPER.writeValueAsString(snap));
        } catch (Exception e) {
            hist.setSnapshotJson("{}");
        }
        commissionHistoryRepository.save(hist);
    }

    private void setBd(java.util.function.Consumer<BigDecimal> setter, Object raw) {
        if (raw == null || raw.toString().isEmpty()) return;
        try {
            setter.accept(new BigDecimal(raw.toString().trim()));
        } catch (Exception ignored) {
        }
    }

    /** 건당·고정액(통화 단위) — 소수 첫째 자리 */
    private void setAmtOne(java.util.function.Consumer<BigDecimal> setter, Object raw) {
        if (raw == null || raw.toString().isEmpty()) return;
        setter.accept(PercentDecimalHelper.parseAmountOneDecimal(raw));
    }

    /** 결제/USDT/FX/3DS/롤링·배분율 등 % 필드 — 소수 첫째 자리 */
    private void setPct(java.util.function.Consumer<BigDecimal> setter, Object raw) {
        if (raw == null || raw.toString().isEmpty()) return;
        setter.accept(PercentDecimalHelper.parsePercentOneDecimal(raw));
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            return u.getUsername() != null ? u.getUsername() : "";
        }
        return auth != null && auth.getName() != null ? auth.getName() : "system";
    }

    public PageResult<Map<String, Object>> history(String compId, int page, int size) {
        String c = compId != null ? compId.trim() : "";
        PageResult<Map<String, Object>> pr = new PageResult<>();
        int sz = Math.max(1, size);
        int pg = Math.max(1, page);
        pr.setPage(pg);
        pr.setSize(sz);
        if (c.isEmpty()) {
            pr.setList(List.of());
            pr.setTotalElements(0);
            pr.setTotalPages(1);
            return pr;
        }
        Optional<OrgUnit> ouOpt = resolveOrgByCode(c);
        if (ouOpt.isEmpty()) {
            pr.setList(List.of());
            pr.setTotalElements(0);
            pr.setTotalPages(1);
            return pr;
        }
        OrgUnit ou = ouOpt.get();

        List<OrgUnit> allOrgs = orgUnitRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
        Map<Long, OrgUnit> orgById = allOrgs.stream()
                .filter(o -> o.getId() != null)
                .collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));

        List<CommissionHistory> asc = commissionHistoryRepository.findByCompIdIgnoreCaseOrderByCreatedAtAsc(c);
        if (asc.isEmpty()) {
            asc = commissionHistoryRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                    .filter(h -> h.getCompId() != null && h.getCompId().trim().equalsIgnoreCase(c))
                    .collect(Collectors.toList());
        }

        LocalDateTime farFuture = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
        List<Map<String, Object>> allRows = new ArrayList<>();
        int n = asc.size();

        if (n == 0) {
            Map<String, Object> live = new LinkedHashMap<>(buildCommissionRow(ou));
            syncNamesFromCurrentOrgTree(live, ou, orgById);
            refreshAncestorNamesByCode(live);
            allRows.add(historyRowShell(inferCommissionEffectiveStart(ou), farFuture, "", live));
        } else {
            CommissionHistory newest = asc.get(n - 1);
            LocalDateTime tNew = newest.getCreatedAt() != null ? newest.getCreatedAt() : LocalDateTime.now();
            String byNew = newest.getChangedBy() != null ? newest.getChangedBy() : "";
            Map<String, Object> newestBody = copyHistorySnapshot(parseHistorySnapshotMap(newest));
            ensureTotalNmCurrencyIfBlank(newestBody, ou);
            syncNamesFromCurrentOrgTree(newestBody, ou, orgById);
            refreshAncestorNamesByCode(newestBody);
            allRows.add(historyRowShell(tNew, farFuture, byNew, newestBody));

            for (int idx = n - 2; idx >= 0; idx--) {
                CommissionHistory h = asc.get(idx);
                LocalDateTime start = h.getCreatedAt();
                LocalDateTime end = asc.get(idx + 1).getCreatedAt();
                Map<String, Object> snapBody = copyHistorySnapshot(parseHistorySnapshotMap(h));
                ensureTotalNmCurrencyIfBlank(snapBody, ou);
                syncNamesFromCurrentOrgTree(snapBody, ou, orgById);
                refreshAncestorNamesByCode(snapBody);
                String by = h.getChangedBy() != null ? h.getChangedBy() : "";
                allRows.add(historyRowShell(start, end, by, snapBody));
            }
        }

        int total = allRows.size();
        int from = (pg - 1) * sz;
        int to = Math.min(from + sz, total);
        List<Map<String, Object>> pageList = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Map<String, Object> copy = new LinkedHashMap<>(allRows.get(i));
            copy.put("rowNo", i + 1);
            pageList.add(copy);
        }

        pr.setList(pageList);
        pr.setTotalElements(total);
        pr.setTotalPages((int) Math.ceil((double) Math.max(0, total) / sz));
        return pr;
    }

    /** 합계 열 표시용: 가맹점 기준 통화(프로필 미설정 시 "-"). */
    private String resolveMerchantBaseCurrencyDisplay(OrgUnit merchant) {
        if (merchant == null || merchant.getId() == null) {
            return "-";
        }
        return merchantProfileRepository.findByOrgUnitId(merchant.getId())
                .map(MerchantProfile::getBaseCurrency)
                .map(s -> s != null ? s.trim() : "")
                .filter(s -> !s.isEmpty())
                .orElse("-");
    }

    /**
     * 과거 스냅샷 JSON에 totalNm(통화)이 없을 때 표시용으로 보강한다.
     */
    private void ensureTotalNmCurrencyIfBlank(Map<String, Object> snapBody, OrgUnit merchant) {
        if (snapBody == null || merchant == null) {
            return;
        }
        Object cur = snapBody.get("totalNm");
        String s = cur != null ? String.valueOf(cur).trim() : "";
        if (s.isEmpty()) {
            snapBody.put("totalNm", resolveMerchantBaseCurrencyDisplay(merchant));
        }
    }

    private static Map<String, Object> historyRowShell(LocalDateTime start, LocalDateTime end, String changedBy, Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (body != null) {
            row.putAll(body);
        }
        row.put("startDttm", start != null ? DT_FMT.format(start) : "");
        row.put("endDttm", end != null ? DT_FMT.format(end) : "");
        row.put("changedBy", changedBy != null ? changedBy : "");
        return row;
    }

    private Map<String, Object> parseHistorySnapshotMap(CommissionHistory h) {
        Map<String, Object> snap = new LinkedHashMap<>();
        if (h == null || h.getSnapshotJson() == null || h.getSnapshotJson().isBlank()) {
            return snap;
        }
        try {
            Map<String, Object> parsed = MAPPER.readValue(h.getSnapshotJson(), new TypeReference<>() {});
            if (parsed != null) {
                snap.putAll(parsed);
            }
        } catch (Exception ignored) {
        }
        return snap;
    }

    /** 이렉 행 간 Map 참조 공유 방지 — 표시용 복사본 */
    private static LinkedHashMap<String, Object> copyHistorySnapshot(Map<String, Object> src) {
        if (src == null || src.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(src);
    }

    private LocalDateTime inferCommissionEffectiveStart(OrgUnit ou) {
        if (ou == null) {
            return LocalDateTime.now();
        }
        String code = ou.getCode() != null ? ou.getCode().trim() : "";
        if (!code.isEmpty()) {
            Optional<DistributionFeeConfig> df = distributionFeeConfigRepository.findByCompId(code);
            if (df.isPresent() && df.get().getApplyStartDate() != null) {
                return df.get().getApplyStartDate().atStartOfDay();
            }
        }
        if (ou.getCreatedAt() != null) {
            return ou.getCreatedAt();
        }
        return LocalDateTime.now();
    }

    private void applyExtraFeesFromCommissionBody(CommissionPolicy p, Map<String, Object> body) {
        for (int i = 1; i <= 4; i++) {
            applyOneExtraFromBody(p, i, body);
        }
    }

    private void applyOneExtraFromBody(CommissionPolicy p, int slot, Map<String, Object> body) {
        String nk = "extraFee" + slot + "Name";
        String mk = "extraFee" + slot + "Mode";
        String vk = "extraFee" + slot + "Value";
        String name = body.get(nk) != null ? body.get(nk).toString().trim() : "";
        String modeNorm = normalizeExtraFeeMode(body.get(mk) != null ? body.get(mk).toString() : null);
        BigDecimal val = "PCT".equals(modeNorm)
                ? PercentDecimalHelper.parsePercentOneDecimal(body.get(vk))
                : parseBdExtra(body.get(vk));
        if (name.isEmpty() || modeNorm == null) {
            clearCommissionExtraSlot(p, slot);
            return;
        }
        String trimmed = name.length() > 64 ? name.substring(0, 64) : name;
        setCommissionExtraSlot(p, slot, trimmed, modeNorm, val);
    }

    private static BigDecimal parseBdExtra(Object o) {
        if (o == null || o.toString().isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(o.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static String normalizeExtraFeeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("PCT".equals(u) || "%".equals(u)) {
            return "PCT";
        }
        if ("FIX".equals(u) || "고정".equals(u)) {
            return "FIX";
        }
        return null;
    }

    private static void clearCommissionExtraSlot(CommissionPolicy p, int slot) {
        switch (slot) {
            case 1 -> {
                p.setExtraFee1Name(null);
                p.setExtraFee1Mode(null);
                p.setExtraFee1Value(null);
            }
            case 2 -> {
                p.setExtraFee2Name(null);
                p.setExtraFee2Mode(null);
                p.setExtraFee2Value(null);
            }
            case 3 -> {
                p.setExtraFee3Name(null);
                p.setExtraFee3Mode(null);
                p.setExtraFee3Value(null);
            }
            case 4 -> {
                p.setExtraFee4Name(null);
                p.setExtraFee4Mode(null);
                p.setExtraFee4Value(null);
            }
            default -> {
            }
        }
    }

    private static void setCommissionExtraSlot(CommissionPolicy p, int slot, String name, String mode, BigDecimal val) {
        switch (slot) {
            case 1 -> {
                p.setExtraFee1Name(name);
                p.setExtraFee1Mode(mode);
                p.setExtraFee1Value(val);
            }
            case 2 -> {
                p.setExtraFee2Name(name);
                p.setExtraFee2Mode(mode);
                p.setExtraFee2Value(val);
            }
            case 3 -> {
                p.setExtraFee3Name(name);
                p.setExtraFee3Mode(mode);
                p.setExtraFee3Value(val);
            }
            case 4 -> {
                p.setExtraFee4Name(name);
                p.setExtraFee4Mode(mode);
                p.setExtraFee4Value(val);
            }
            default -> {
            }
        }
    }

    public Map<String, Object> commissionOtpStatus(Authentication authentication) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("otpGraceMinutes", COMMISSION_OTP_GRACE_MINUTES);
        AppUser user = resolveAuthUser(authentication);
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            m.put("otpRequired", true);
            return m;
        }
        String uname = user.getUsername().trim();
        boolean required = requiresCommissionOtp(uname);
        m.put("otpRequired", required);
        if (!required) {
            Long at = commissionOtpPassedAtMs.get(uname);
            if (at != null) {
                m.put("otpGraceExpiresAtMs", at + COMMISSION_OTP_GRACE_MS);
            }
        }
        return m;
    }

    /**
     * 이미 OTP 유예 중일 때만 활동 시각을 갱신(슬라이딩 연장). OTP 없이 유예 시작은 하지 않음.
     */
    public Map<String, Object> touchCommissionOtpActivity(Authentication authentication) {
        AppUser user = resolveAuthUser(authentication);
        if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
            String uname = user.getUsername().trim();
            if (!requiresCommissionOtp(uname)) {
                markCommissionOtpActivity(uname);
            }
        }
        return commissionOtpStatus(authentication);
    }

    /**
     * 저장 전 OTP: 유예 밖이면 검증, 통과·유예 내면 활동 시각을 갱신해 10분을 연장한다.
     */
    public void verifyCommissionOtpForSave(Authentication authentication, String totpCode) {
        AppUser user = resolveAuthUser(authentication);
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String uname = user.getUsername().trim();
        if (requiresCommissionOtp(uname)) {
            authService.verifyTotpOrThrow(user, totpCode);
        }
        markCommissionOtpActivity(uname);
    }

    private boolean requiresCommissionOtp(String username) {
        if (username == null || username.isBlank()) {
            return true;
        }
        Long at = commissionOtpPassedAtMs.get(username.trim());
        if (at == null) {
            return true;
        }
        return System.currentTimeMillis() - at > COMMISSION_OTP_GRACE_MS;
    }

    private void markCommissionOtpActivity(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        commissionOtpPassedAtMs.put(username.trim(), System.currentTimeMillis());
    }

    private static AppUser resolveAuthUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser u)) {
            return null;
        }
        return u;
    }
}
