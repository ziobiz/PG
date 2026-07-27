package com.pg.service.ops;

import com.pg.entity.OrgUnit;
import com.pg.entity.PayRiskFilterEvent;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PayRiskFilterEventRepository;
import com.pg.util.PayPresaleRiskFilterI18n;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpsPayRiskFilterService {

    private final PayRiskFilterEventRepository eventRepository;
    private final OrgUnitRepository orgUnitRepository;

    public OpsPayRiskFilterService(PayRiskFilterEventRepository eventRepository,
                                   OrgUnitRepository orgUnitRepository) {
        this.eventRepository = eventRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(String merchantId,
                                      String filterCode,
                                      LocalDate fromDate,
                                      LocalDate toDate,
                                      int page,
                                      int size) {
        LocalDateTime fromDt = fromDate != null ? fromDate.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime toDt = toDate != null ? toDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);
        int p = Math.max(1, page);
        int s = Math.min(Math.max(size, 1), 200);
        Page<PayRiskFilterEvent> pg = eventRepository.search(
                trim(merchantId), trim(filterCode), fromDt, toDt,
                PageRequest.of(p - 1, s, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Map<String, Object>> list = new ArrayList<>();
        for (PayRiskFilterEvent e : pg.getContent()) {
            list.add(toRow(e));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", list);
        out.put("totalElements", pg.getTotalElements());
        out.put("totalPages", pg.getTotalPages());
        out.put("page", p);
        out.put("size", s);
        return out;
    }

    public List<Map<String, String>> filterCodeOptions() {
        List<Map<String, String>> out = new ArrayList<>();
        for (String code : List.of(
                "BUYER_EMAIL_MISMATCH", "BUYER_PHONE_MISMATCH", "BUYER_NAME_MISMATCH",
                "HOLDER_NAME_SUSPICIOUS", "VELOCITY_CARD", "VELOCITY_EMAIL", "VELOCITY_IP",
                "PHONE_INVALID", "EMAIL_INVALID",
                com.pg.util.JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_HIGH_RISK,
                com.pg.util.JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_PY0124)) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("v", code);
            row.put("t", PayPresaleRiskFilterI18n.filterLabelKo(code));
            out.add(row);
        }
        return out;
    }

    private Map<String, Object> toRow(PayRiskFilterEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("compId", e.getMerchantId());
        m.put("compNm", resolveCompNm(e.getMerchantId()));
        m.put("orderNo", e.getOrderNo());
        m.put("trnId", e.getTrnId());
        m.put("pgVendor", e.getPgVendor());
        String code = e.getFilterCode() != null ? e.getFilterCode().trim() : "";
        m.put("riskDiv", code);
        /* 저장 desc가 한국어여도 코드 기준으로 라벨 재생성 → 프론트 i18n 키로 사용 */
        String descKo = !code.isEmpty()
                ? PayPresaleRiskFilterI18n.filterLabelKo(code)
                : (e.getFilterDesc() != null ? e.getFilterDesc() : "");
        m.put("riskDesc", descKo);
        m.put("regDt", e.getCreatedAt());
        return m;
    }

    private String resolveCompNm(String compId) {
        if (compId == null || compId.isBlank()) {
            return "";
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(compId.trim());
        return ou.map(OrgUnit::getName).orElse("");
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
