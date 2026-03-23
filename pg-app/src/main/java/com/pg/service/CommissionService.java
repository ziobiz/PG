package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.entity.CommissionHistory;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.MerchantCommissionExtra;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.CommissionHistoryRepository;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.DistributionFeeConfigRepository;
import com.pg.repository.MerchantCommissionExtraRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommissionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrgUnitRepository orgUnitRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    private final CommissionHistoryRepository commissionHistoryRepository;
    private final DistributionFeeConfigRepository distributionFeeConfigRepository;

    public CommissionService(OrgUnitRepository orgUnitRepository,
                             CommissionPolicyRepository commissionPolicyRepository,
                             MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                             CommissionHistoryRepository commissionHistoryRepository,
                             DistributionFeeConfigRepository distributionFeeConfigRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.commissionHistoryRepository = commissionHistoryRepository;
        this.distributionFeeConfigRepository = distributionFeeConfigRepository;
    }

    public PageResult<Map<String, Object>> search(String searchCompId, String searchCompNm, int page, int size) {
        List<OrgUnit> all = orgUnitRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
        List<OrgUnit> filtered = all.stream()
                .filter(o -> o.getOrgLevel() == OrgLevel.MERCHANT)
                .filter(o -> (searchCompId == null || searchCompId.isEmpty() || (o.getCode() != null && o.getCode().contains(searchCompId))))
                .filter(o -> (searchCompNm == null || searchCompNm.isEmpty() || (o.getName() != null && o.getName().contains(searchCompNm))))
                .collect(Collectors.toList());
        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<OrgUnit> pageList = start < total ? filtered.subList(start, end) : new ArrayList<>();

        Optional<CommissionPolicy> defaultPolicy = commissionPolicyRepository.findByScope("DEFAULT");
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrgUnit ou : pageList) {
            Map<String, Object> m = buildCommissionRow(ou, defaultPolicy);
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

    private Map<String, Object> buildCommissionRow(OrgUnit merchant, Optional<CommissionPolicy> defaultPolicy) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", merchant.getId());
        m.put("compId", merchant.getCode());
        m.put("compNm", merchant.getName());
        m.put("compDiv", merchant.getOrgLevel() != null ? merchant.getOrgLevel().name() : "");
        String regDt = merchant.getCreatedAt() != null ? merchant.getCreatedAt().toString().substring(0, 10) : null;
        m.put("regDt", regDt);
        fillAncestorNames(m, merchant);

        CommissionPolicy policy = commissionPolicyRepository.findByScope(merchant.getCode()).or(() -> defaultPolicy).orElse(null);
        if (policy != null) {
            m.put("cmsnRate", policy.getPayRate());
            m.put("perTxFee", policy.getPerTxFee());
            m.put("cancelRate", policy.getCancelRate());
            m.put("payRate", policy.getPayRate());
            m.put("refundRate", policy.getRefundRate());
            m.put("rollingPct", policy.getRollingPct());
            m.put("rollingDays", policy.getRollingDays());
        }
        merchantCommissionExtraRepository.findByOrgUnitId(merchant.getId()).ifPresent(ex -> {
            m.put("feeAccountActivation", ex.getFeeAccountActivation());
            m.put("feeAnnual", ex.getFeeAnnual());
            m.put("feeTechService", ex.getFeeTechService());
            m.put("feeSettlementPerTx", ex.getFeeSettlementPerTx());
            m.put("feeRefund", ex.getFeeRefund());
        });
        Optional<DistributionFeeConfig> odf = distributionFeeConfigRepository.findByCompId(merchant.getCode());
        if (odf.isPresent()) {
            applyDistributionToMap(m, odf.get());
        } else {
            applyDistributionToMap(m, null);
        }

        m.put("totalNm", "");
        putTotals(m);
        LocalDate apply = m.get("applyStartDate") instanceof LocalDate d ? d
                : (m.get("applyStartDate") != null ? LocalDate.parse(m.get("applyStartDate").toString()) : null);
        m.put("applyDt", apply != null ? apply.toString() : regDt);
        return m;
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
        OrgUnit cur = merchant;
        for (int i = 0; i < 20 && cur != null; i++) {
            OrgLevel lv = cur.getOrgLevel();
            if (lv != null) {
                String nm = cur.getName() != null ? cur.getName() : "";
                switch (lv) {
                    case HEADQUARTERS -> {
                        if (hqNm.isEmpty()) hqNm = nm;
                    }
                    case REGIONAL -> {
                        if (regionalNm.isEmpty()) regionalNm = nm;
                    }
                    case MASTER_DIST -> {
                        if (masterNm.isEmpty()) masterNm = nm;
                    }
                    case BRANCH -> {
                        if (branchNm.isEmpty()) branchNm = nm;
                    }
                    case AGENCY -> {
                        if (agencyNm.isEmpty()) agencyNm = nm;
                    }
                    case SALES_OFFICE -> {
                        if (salesOfficeNm.isEmpty()) salesOfficeNm = nm;
                    }
                    default -> {
                    }
                }
            }
            cur = cur.getParentId() != null ? orgUnitRepository.findById(cur.getParentId()).orElse(null) : null;
        }
        m.put("hqNm", hqNm);
        m.put("regionalNm", regionalNm);
        m.put("masterNm", masterNm);
        m.put("branchNm", branchNm);
        m.put("agencyNm", agencyNm);
        m.put("salesOfficeNm", salesOfficeNm);
    }

    public Optional<Map<String, Object>> getDetail(String compId) {
        return orgUnitRepository.findByCode(compId != null ? compId : "").map(ou -> {
            Map<String, Object> m = new HashMap<>();
            m.put("compId", ou.getCode());
            m.put("compNm", ou.getName());
            m.put("orgUnitId", ou.getId());
            CommissionPolicy policy = commissionPolicyRepository.findByScope(ou.getCode()).orElse(null);
            if (policy != null) {
                m.put("perTxFee", policy.getPerTxFee());
                m.put("cancelRate", policy.getCancelRate());
                m.put("usageRate", policy.getUsageRate());
                m.put("failFee", policy.getFailFee());
                m.put("payRate", policy.getPayRate());
                m.put("refundRate", policy.getRefundRate());
                m.put("rollingPct", policy.getRollingPct());
                m.put("rollingDays", policy.getRollingDays());
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
            Optional<DistributionFeeConfig> odf2 = distributionFeeConfigRepository.findByCompId(ou.getCode());
            if (odf2.isPresent()) {
                applyDistributionToMap(m, odf2.get());
            } else {
                applyDistributionToMap(m, null);
            }
            putTotals(m);
            if (m.get("applyStartDate") instanceof LocalDate d) m.put("applyStartDateStr", d.toString());
            else if (m.get("applyStartDate") != null) m.put("applyStartDateStr", m.get("applyStartDate").toString());
            return m;
        });
    }

    public boolean save(String compId, Map<String, Object> body) {
        return orgUnitRepository.findByCode(compId != null ? compId : "").map(ou -> {
            CommissionPolicy policy = commissionPolicyRepository.findByScope(ou.getCode())
                    .orElseGet(() -> {
                        CommissionPolicy p = new CommissionPolicy();
                        p.setScope(ou.getCode());
                        return p;
                    });
            setBd(policy::setPerTxFee, body.get("perTxFee"));
            setBd(policy::setCancelRate, body.get("cancelRate"));
            setBd(policy::setUsageRate, body.get("usageRate"));
            setBd(policy::setFailFee, body.get("failFee"));
            setBd(policy::setPayRate, body.get("payRate"));
            setBd(policy::setRefundRate, body.get("refundRate"));
            setBd(policy::setRollingPct, body.get("rollingPct"));
            if (body.get("rollingDays") != null && !body.get("rollingDays").toString().isEmpty()) {
                policy.setRollingDays(Integer.parseInt(body.get("rollingDays").toString()));
            }
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
            merchantCommissionExtraRepository.save(extra);

            DistributionFeeConfig df = distributionFeeConfigRepository.findByCompId(compId).orElseGet(() -> {
                DistributionFeeConfig x = new DistributionFeeConfig();
                x.setCompId(compId);
                return x;
            });
            setBd(df::setHqRate, body.get("hqRate"));
            setBd(df::setRegionalRate, body.get("regionalRate"));
            setBd(df::setMasterRate, body.get("masterRate"));
            setBd(df::setBranchRate, body.get("branchRate"));
            setBd(df::setAgencyRate, body.get("agencyRate"));
            setBd(df::setSalesOfficeRate, body.get("salesOfficeRate"));
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

            Optional<CommissionPolicy> defaultPolicy = commissionPolicyRepository.findByScope("DEFAULT");
            Map<String, Object> snap = buildCommissionRow(ou, defaultPolicy);
            snap.put("compNm", ou.getName());
            snap.put("compId", compId);

            CommissionHistory hist = new CommissionHistory();
            hist.setCompId(compId);
            hist.setChgType("COMMISSION");
            hist.setChgDesc("수수료 설정 변경 저장");
            hist.setChangedBy(currentUsername());
            try {
                hist.setSnapshotJson(MAPPER.writeValueAsString(snap));
            } catch (Exception e) {
                hist.setSnapshotJson("{}");
            }
            commissionHistoryRepository.save(hist);
            return true;
        }).orElse(false);
    }

    private void setBd(java.util.function.Consumer<BigDecimal> setter, Object raw) {
        if (raw == null || raw.toString().isEmpty()) return;
        try {
            setter.accept(new BigDecimal(raw.toString().trim()));
        } catch (Exception ignored) {
        }
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
        pr.setPage(page);
        pr.setSize(size);
        if (c.isEmpty()) {
            pr.setList(List.of());
            pr.setTotalElements(0);
            pr.setTotalPages(1);
            return pr;
        }
        List<CommissionHistory> desc = commissionHistoryRepository.findAllByCompIdOrderByCreatedAtDesc(c);
        int total = desc.size();
        int sz = Math.max(1, size);
        int pg = Math.max(1, page);
        int from = (pg - 1) * sz;
        int to = Math.min(from + sz, total);

        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDateTime farFuture = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
        for (int i = from; i < to; i++) {
            CommissionHistory h = desc.get(i);
            LocalDateTime start = h.getCreatedAt();
            LocalDateTime end = (i == 0) ? farFuture : desc.get(i - 1).getCreatedAt();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rowNo", i + 1);
            row.put("startDttm", start != null ? DT_FMT.format(start) : "");
            row.put("endDttm", end != null ? DT_FMT.format(end) : "");
            row.put("changedBy", h.getChangedBy() != null ? h.getChangedBy() : "");
            if (h.getSnapshotJson() != null && !h.getSnapshotJson().isBlank()) {
                try {
                    Map<String, Object> snap = MAPPER.readValue(h.getSnapshotJson(), new TypeReference<>() {});
                    if (snap != null) {
                        for (Map.Entry<String, Object> e : snap.entrySet()) {
                            if (!row.containsKey(e.getKey())) {
                                row.put(e.getKey(), e.getValue());
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            rows.add(row);
        }

        pr.setList(rows);
        pr.setTotalElements(total);
        pr.setTotalPages(sz <= 0 ? 1 : (int) Math.ceil((double) Math.max(0, total) / sz));
        return pr;
    }
}
