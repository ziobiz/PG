package com.pg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqRiskCardPolicy;
import com.pg.entity.OrgUnit;
import com.pg.entity.PayRiskFilterEvent;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PayRiskFilterEventRepository;
import com.pg.util.JpayPostSaleRiskOutcomeUtil;
import com.pg.util.PayPresaleRiskFilterI18n;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** JPAY 사후 고위험·PY0124 — 리스크 현황 이벤트·쿨다운 집계 on/off */
@Service
public class JpayPostSaleRiskCooldownService {

    private final HqRiskCardPolicyService hqRiskCardPolicyService;
    private final PayRiskFilterEventRepository eventRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ObjectMapper objectMapper;

    public JpayPostSaleRiskCooldownService(HqRiskCardPolicyService hqRiskCardPolicyService,
                                           PayRiskFilterEventRepository eventRepository,
                                           OrgUnitRepository orgUnitRepository,
                                           ObjectMapper objectMapper) {
        this.hqRiskCardPolicyService = hqRiskCardPolicyService;
        this.eventRepository = eventRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.objectMapper = objectMapper;
    }

    public boolean shouldCountCooldown(String classification) {
        if (classification == null || classification.isBlank()) {
            return true;
        }
        HqRiskCardPolicy hq = hqRiskCardPolicyService.getOrCreate();
        if (JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_PY0124.equals(classification)) {
            return yn(hq.getPostsaleCooldownJpayPy0124Yn());
        }
        if (JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_HIGH_RISK.equals(classification)) {
            return yn(hq.getPostsaleCooldownJpayHighriskYn());
        }
        return true;
    }

    @Transactional
    public void recordPostSaleEvent(PgTrnsctn t, String classification, String outcomeMsg) {
        if (t == null || classification == null || classification.isBlank()) {
            return;
        }
        PayRiskFilterEvent row = new PayRiskFilterEvent();
        row.setOrgUnitId(resolveOrgUnitId(t));
        row.setMerchantId(t.getMerchantId());
        row.setOrderNo(t.getOrderNo());
        row.setTrnId(t.getTrnId());
        row.setPgVendor("JPAY");
        row.setFilterCode(classification);
        row.setFilterDesc(PayPresaleRiskFilterI18n.filterLabelKo(classification));
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("filterCode", classification);
        detail.put("phase", "POSTSALE");
        detail.put("outcomeReason", truncate(outcomeMsg, 500));
        try {
            row.setDetailJson(objectMapper.writeValueAsString(detail));
        } catch (Exception ignored) {
            row.setDetailJson("{}");
        }
        eventRepository.save(row);
    }

    private Long resolveOrgUnitId(PgTrnsctn t) {
        if (t == null || t.getMerchantId() == null || t.getMerchantId().isBlank()) {
            return null;
        }
        return orgUnitRepository.findByCode(t.getMerchantId().trim()).map(OrgUnit::getId).orElse(null);
    }

    private static boolean yn(String raw) {
        return raw == null || raw.isBlank() || "Y".equalsIgnoreCase(raw.trim());
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
