package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.MerchantCommissionExtra;
import com.pg.entity.OrgUnit;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.CommissionHistoryRepository;
import com.pg.repository.MerchantCommissionExtraRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.entity.CommissionHistory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommissionService {

    private final OrgUnitRepository orgUnitRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final MerchantCommissionExtraRepository merchantCommissionExtraRepository;
    private final CommissionHistoryRepository commissionHistoryRepository;

    public CommissionService(OrgUnitRepository orgUnitRepository,
                             CommissionPolicyRepository commissionPolicyRepository,
                             MerchantCommissionExtraRepository merchantCommissionExtraRepository,
                             CommissionHistoryRepository commissionHistoryRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.merchantCommissionExtraRepository = merchantCommissionExtraRepository;
        this.commissionHistoryRepository = commissionHistoryRepository;
    }

    public PageResult<Map<String, Object>> search(String searchCompId, String searchCompNm, int page, int size) {
        List<OrgUnit> all = orgUnitRepository.findAll(Sort.by(Sort.Direction.ASC, "code"));
        List<OrgUnit> filtered = all.stream()
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
            Map<String, Object> m = new HashMap<>();
            m.put("id", ou.getId());
            m.put("compId", ou.getCode());
            m.put("compNm", ou.getName());
            m.put("compDiv", ou.getOrgLevel() != null ? ou.getOrgLevel().name() : "");
            String regDt = ou.getCreatedAt() != null ? ou.getCreatedAt().toString().substring(0, 10) : null;
            m.put("regDt", regDt);
            m.put("applyDt", regDt);
            CommissionPolicy policy = commissionPolicyRepository.findByScope(ou.getCode()).or(() -> defaultPolicy).orElse(null);
            if (policy != null) {
                m.put("cmsnRate", policy.getPayRate());
                m.put("perTxFee", policy.getPerTxFee());
                m.put("cancelRate", policy.getCancelRate());
                m.put("payRate", policy.getPayRate());
                m.put("refundRate", policy.getRefundRate());
                m.put("rollingPct", policy.getRollingPct());
                m.put("rollingDays", policy.getRollingDays());
            }
            merchantCommissionExtraRepository.findByOrgUnitId(ou.getId()).ifPresent(ex -> {
                m.put("feeAccountActivation", ex.getFeeAccountActivation());
                m.put("feeAnnual", ex.getFeeAnnual());
                m.put("feeTechService", ex.getFeeTechService());
                m.put("feeSettlementPerTx", ex.getFeeSettlementPerTx());
                m.put("feeRefund", ex.getFeeRefund());
            });
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
            if (body.get("perTxFee") != null && !body.get("perTxFee").toString().isEmpty()) policy.setPerTxFee(new BigDecimal(body.get("perTxFee").toString()));
            if (body.get("cancelRate") != null && !body.get("cancelRate").toString().isEmpty()) policy.setCancelRate(new BigDecimal(body.get("cancelRate").toString()));
            if (body.get("usageRate") != null && !body.get("usageRate").toString().isEmpty()) policy.setUsageRate(new BigDecimal(body.get("usageRate").toString()));
            if (body.get("failFee") != null && !body.get("failFee").toString().isEmpty()) policy.setFailFee(new BigDecimal(body.get("failFee").toString()));
            if (body.get("payRate") != null && !body.get("payRate").toString().isEmpty()) policy.setPayRate(new BigDecimal(body.get("payRate").toString()));
            if (body.get("refundRate") != null && !body.get("refundRate").toString().isEmpty()) policy.setRefundRate(new BigDecimal(body.get("refundRate").toString()));
            if (body.get("rollingPct") != null && !body.get("rollingPct").toString().isEmpty()) policy.setRollingPct(new BigDecimal(body.get("rollingPct").toString()));
            if (body.get("rollingDays") != null && !body.get("rollingDays").toString().isEmpty()) policy.setRollingDays(Integer.parseInt(body.get("rollingDays").toString()));
            commissionPolicyRepository.save(policy);

            MerchantCommissionExtra extra = merchantCommissionExtraRepository.findByOrgUnitId(ou.getId())
                    .orElseGet(() -> {
                        MerchantCommissionExtra e = new MerchantCommissionExtra();
                        e.setOrgUnitId(ou.getId());
                        return e;
                    });
            if (body.get("feeAccountActivation") != null && !body.get("feeAccountActivation").toString().isEmpty()) extra.setFeeAccountActivation(new BigDecimal(body.get("feeAccountActivation").toString()));
            if (body.get("feeAnnual") != null && !body.get("feeAnnual").toString().isEmpty()) extra.setFeeAnnual(new BigDecimal(body.get("feeAnnual").toString()));
            if (body.get("feeTechService") != null && !body.get("feeTechService").toString().isEmpty()) extra.setFeeTechService(new BigDecimal(body.get("feeTechService").toString()));
            if (body.get("feeSettlementPerTx") != null && !body.get("feeSettlementPerTx").toString().isEmpty()) extra.setFeeSettlementPerTx(new BigDecimal(body.get("feeSettlementPerTx").toString()));
            if (body.get("feeRefund") != null && !body.get("feeRefund").toString().isEmpty()) extra.setFeeRefund(new BigDecimal(body.get("feeRefund").toString()));
            merchantCommissionExtraRepository.save(extra);
            CommissionHistory hist = new CommissionHistory();
            hist.setCompId(compId);
            hist.setChgType("COMMISSION");
            hist.setChgDesc("수수료 설정 변경 저장");
            commissionHistoryRepository.save(hist);
            return true;
        }).orElse(false);
    }
}
