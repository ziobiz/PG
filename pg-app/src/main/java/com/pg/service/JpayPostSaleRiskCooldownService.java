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

/** JPAY 사후 고위험·PY0124 — 리스크 현황 이벤트 기록 on/off (위험관리 쿨다운 집계와 분리) */
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

    /**
     * 운영관리 「리스크 현황」에 JPAY 사후 이벤트를 남길지.
     * <p>위험관리(카드 실패 쿨다운·자동 비활성) 집계와는 무관 — 고위험 FAIL 도 위험관리에는 항상 포함.</p>
     */
    public boolean shouldRecordPostSaleEvent(String classification) {
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

    /** @deprecated use {@link #shouldRecordPostSaleEvent(String)} — 쿨다운 집계 게이트가 아님 */
    @Deprecated
    public boolean shouldCountCooldown(String classification) {
        return shouldRecordPostSaleEvent(classification);
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
