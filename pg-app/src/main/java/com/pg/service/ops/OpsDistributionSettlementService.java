package com.pg.service.ops;

import com.pg.api.dto.PageResult;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementRun;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SettlementRunRepository;
import com.pg.service.CommissionService;
import com.pg.service.OrgAccessService;
import com.pg.service.SettlementCalcService;
import com.pg.util.CommissionTierJsonHelper;
import com.pg.util.DistributionSettlementPerspectiveUtil;
import com.pg.util.FeeCurrencyRoundResolver;
import com.pg.util.FeeListRoundingPolicy;
import com.pg.util.PayDisplayCurrency;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 운영관리 — 유통망정산(가맹 정산 실행을 로그인 조직 시점으로 표시).
 */
@Service
public class OpsDistributionSettlementService {

    private final OpsDistributionAccessService opsDistributionAccessService;
    private final OrgAccessService orgAccessService;
    private final SettlementRunRepository settlementRunRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementCalcService settlementCalcService;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;
    private final CommissionService commissionService;
    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;

    public OpsDistributionSettlementService(OpsDistributionAccessService opsDistributionAccessService,
                                            OrgAccessService orgAccessService,
                                            SettlementRunRepository settlementRunRepository,
                                            OrgUnitRepository orgUnitRepository,
                                            SettlementCalcService settlementCalcService,
                                            DistributionFeeConfigRepository distributionFeeConfigRepository,
                                            CommissionService commissionService,
                                            HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository) {
        this.opsDistributionAccessService = opsDistributionAccessService;
        this.orgAccessService = orgAccessService;
        this.settlementRunRepository = settlementRunRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementCalcService = settlementCalcService;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
        this.commissionService = commissionService;
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        return opsDistributionAccessService.accessMeta(authentication, "distributionSettlement");
    }

    public PageResult<Map<String, Object>> list(Authentication authentication,
                                                  LocalDate searchFromDate,
                                                  LocalDate searchToDate,
                                                  String searchCompId,
                                                  String searchCompNm,
                                                  String searchFieldType,
                                                  String searchKeyword,
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

        final Set<String> merchantNameFilter;
        if ("COMP_NM".equals(effFt) && !effKw.isEmpty()) {
            Set<String> nm = new HashSet<>();
            for (OrgUnit ou : orgUnitRepository.findByOrgLevelAndNameContainingIgnoreCase(OrgLevel.MERCHANT, effKw)) {
                if (ou.getCode() == null || ou.getCode().isBlank()) {
                    continue;
                }
                String code = ou.getCode().trim();
                if (allowedMerchants == null || allowedMerchants.contains(code)) {
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

        Specification<SettlementRun> spec = buildSpec(fromDate, toDate, allowedMerchants, merchantNameFilter, effFt, effKw);
        int pageSize = Math.min(500, Math.max(1, size));
        int pageOneBased = Math.max(1, page);
        Pageable pageable = PageRequest.of(pageOneBased - 1, pageSize,
                Sort.by(sortDirection(searchOrderDir), "calcDt")
                        .and(Sort.by(sortDirection(searchOrderDir), "merchantId")));
        Page<SettlementRun> slice = settlementRunRepository.findAll(spec, pageable);

        FeeListRoundingPolicy ledgerRp = hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc()
                .map(s -> FeeCurrencyRoundResolver.from(s).forCurrency(PayDisplayCurrency.alphaFromSettings(s)))
                .orElseGet(FeeListRoundingPolicy::defaults);
        Map<String, DistributionFeeConfig> distCfgCache = new HashMap<>();

        List<Map<String, Object>> rows = new ArrayList<>();
        int rowNoStart = (pageOneBased - 1) * pageSize + 1;
        int rowIdx = 0;
        for (SettlementRun r : slice.getContent()) {
            if (!isVisibleRun(r)) {
                continue;
            }
            Map<String, Object> row = buildRow(r, distCfgCache, ledgerRp, viewerLevel);
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
        if (viewerLevel != null) {
            meta.put("viewerOrgLevel", viewerLevel.name());
            meta.put("viewerOrgLevelNm", viewerLevel.getNameKo());
        }
        pr.setMeta(meta);
        return pr;
    }

    private boolean isVisibleRun(SettlementRun r) {
        if (r == null) {
            return false;
        }
        if ("Y".equalsIgnoreCase(r.getPayoutHoldYn() != null ? r.getPayoutHoldYn() : "")) {
            return false;
        }
        if (!settlementCalcService.isDistributedForMerchantStatementView(r)) {
            return false;
        }
        return settlementCalcService.isMerchantStatementVisibleSettlementRun(r);
    }

    private Map<String, Object> buildRow(SettlementRun r,
                                         Map<String, DistributionFeeConfig> distCfgCache,
                                         FeeListRoundingPolicy rp,
                                         OrgLevel viewerLevel) {
        String compId = r.getMerchantId() != null ? r.getMerchantId().trim() : "";
        OrgUnit merchant = orgUnitRepository.findByCode(compId).orElse(null);
        DistributionFeeConfig cfg = distCfgCache.computeIfAbsent(compId, this::resolveDistributionConfig);
        DistributionSettlementPerspectiveUtil.TierFeeMap tierMap =
                DistributionSettlementPerspectiveUtil.tierFeesFromRun(r, cfg, rp);
        DistributionSettlementPerspectiveUtil.ViewerSlice slice =
                DistributionSettlementPerspectiveUtil.viewerSlice(tierMap, viewerLevel, r);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("calcDt", r.getCalcDt() != null ? r.getCalcDt().toString() : "");
        m.put("compId", compId);
        m.put("compNm", merchant != null && merchant.getName() != null ? merchant.getName() : compId);
        m.put("curType", resolveMerchantCurrency(compId));
        m.put("approveAmt", money(r.getApproveAmt(), rp));
        m.put("cancelAmt", money(r.getCancelAmt(), rp));
        m.put("merchantTotalFee", money(r.getTotalFee(), rp));
        m.put("merchantPayAmt", money(r.getPayAmt(), rp));
        m.put("hqFee", money(tierMap.get(OrgLevel.HEADQUARTERS), rp));
        m.put("regionalFee", money(tierMap.get(OrgLevel.REGIONAL), rp));
        m.put("masterFee", money(tierMap.get(OrgLevel.MASTER_DIST), rp));
        m.put("branchFee", money(tierMap.get(OrgLevel.BRANCH), rp));
        m.put("agencyFee", money(tierMap.get(OrgLevel.AGENCY), rp));
        m.put("salesOfficeFee", money(tierMap.get(OrgLevel.SALES_OFFICE), rp));
        m.put("upstreamDistFee", money(slice.upstreamDistFee(), rp));
        m.put("ownTierDistFee", money(slice.ownTierDistFee(), rp));
        m.put("downstreamDistFee", money(slice.downstreamDistFee(), rp));
        m.put("passThroughDistFee", money(slice.passThroughDistFee(), rp));
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

    private String resolveMerchantCurrency(String compId) {
        if (compId == null || compId.isBlank()) {
            return "KRW";
        }
        CommissionPolicy pol = commissionService.resolveCommissionPolicyForSettlement(compId.trim());
        if (pol == null || pol.getCurrencyCode() == null || pol.getCurrencyCode().isBlank()) {
            return "KRW";
        }
        return pol.getCurrencyCode().trim().toUpperCase(Locale.ROOT);
    }

    private Specification<SettlementRun> buildSpec(LocalDate from,
                                                   LocalDate to,
                                                   Set<String> allowedMerchants,
                                                   Set<String> merchantNameFilter,
                                                   String effFt,
                                                   String effKw) {
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            parts.add(cb.between(root.get("calcDt"), from, to));
            parts.add(cb.isNotNull(root.get("merchantId")));
            if (allowedMerchants != null) {
                parts.add(root.get("merchantId").in(allowedMerchants));
            }
            if (merchantNameFilter != null) {
                parts.add(root.get("merchantId").in(merchantNameFilter));
            }
            if ("COMP_ID".equals(effFt) && effKw != null && !effKw.isBlank()) {
                parts.add(cb.like(cb.lower(root.get("merchantId")),
                        "%" + effKw.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            return cb.and(parts.toArray(new Predicate[0]));
        };
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

    private static Sort.Direction sortDirection(String searchOrderDir) {
        if (searchOrderDir != null && "ASC".equalsIgnoreCase(searchOrderDir.trim())) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
    }
}
