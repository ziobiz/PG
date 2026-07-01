package com.pg.service;

import com.pg.entity.HqNotiWebhookPartner;
import com.pg.repository.HqNotiWebhookPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HqNotiWebhookPartnerService {

    private final HqNotiWebhookPartnerRepository repository;
    private final HqNotifyEnvService hqNotifyEnvService;

    public HqNotiWebhookPartnerService(HqNotiWebhookPartnerRepository repository,
                                       HqNotifyEnvService hqNotifyEnvService) {
        this.repository = repository;
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    public List<Map<String, Object>> listAll() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toMap).toList();
    }

    public List<Map<String, Object>> listActive() {
        return repository.findByUseYnOrderBySortOrderAscIdAsc("Y").stream().map(this::toMap).toList();
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String code = str(body, "partnerCode");
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Partner 코드를 입력하세요.");
        }
        if (repository.findByPartnerCode(code).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 Partner 코드입니다: " + code);
        }
        HqNotiWebhookPartner row = new HqNotiWebhookPartner();
        row.setPartnerCode(code);
        row.setPartnerLabel(str(body, "partnerLabel"));
        row.setSortOrder(parseSort(body.get("sortOrder")));
        row.setUseYn(yn(str(body, "useYn"), "Y"));
        return toMap(repository.save(row));
    }

    public boolean isActivePartnerCode(String partnerCode) {
        String code = partnerCode != null ? partnerCode.trim() : "";
        if (code.isEmpty()) {
            return false;
        }
        return repository.findByPartnerCode(code)
                .filter(p -> "Y".equalsIgnoreCase(yn(p.getUseYn(), "Y")))
                .isPresent();
    }

    @Transactional
    public Map<String, Object> delete(long id) {
        HqNotiWebhookPartner row = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partner를 찾을 수 없습니다."));
        String code = row.getPartnerCode() != null ? row.getPartnerCode().trim() : "";
        repository.delete(row);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "삭제되었습니다.");
        out.put("partnerCode", code);
        if (!code.isEmpty()) {
            out.putAll(hqNotifyEnvService.clearProvisionRefsForWebhookPartnerCode(code));
        }
        return out;
    }

    private Map<String, Object> toMap(HqNotiWebhookPartner p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("partnerCode", p.getPartnerCode());
        m.put("partnerLabel", p.getPartnerLabel() != null ? p.getPartnerLabel() : "");
        m.put("sortOrder", p.getSortOrder() != null ? p.getSortOrder() : 0);
        m.put("useYn", p.getUseYn() != null ? p.getUseYn() : "Y");
        return m;
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key)).trim();
    }

    private static int parseSort(Object v) {
        if (v == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String yn(String v, String def) {
        if (v == null || v.isBlank()) {
            return def;
        }
        return "Y".equalsIgnoreCase(v.trim()) ? "Y" : "N";
    }
}
