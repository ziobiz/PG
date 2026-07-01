package com.pg.noti;

import com.pg.entity.HqNotifyEnvConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * NOTI internal-targets 목록 조회·ID 정규화.
 */
@Service
public class NotiInternalTargetCatalogService {

    private final NotiProvisionClient notiProvisionClient;

    public NotiInternalTargetCatalogService(NotiProvisionClient notiProvisionClient) {
        this.notiProvisionClient = notiProvisionClient;
    }

    public boolean isProvisionConfigured(HqNotifyEnvConfig cfg) {
        return cfg != null
                && "Y".equalsIgnoreCase(yn(cfg.getNotiProvisionEnabledYn()))
                && cfg.getNotiProvisionBaseUrl() != null && !cfg.getNotiProvisionBaseUrl().isBlank()
                && cfg.getNotiProvisionApiKey() != null && !cfg.getNotiProvisionApiKey().isBlank();
    }

    public List<Map<String, Object>> listFromNoti(HqNotifyEnvConfig cfg, String acceptLanguage) {
        if (!isProvisionConfigured(cfg)) {
            return List.of();
        }
        List<Map<String, Object>> raw = notiProvisionClient.listInternalTargets(
                cfg.getNotiProvisionBaseUrl(),
                cfg.getNotiProvisionApiKey(),
                acceptLanguage);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            if (row == null) {
                continue;
            }
            String id = str(row.get("id"));
            if (id.isEmpty()) {
                id = str(row.get("internalTargetId"));
            }
            if (id.isEmpty()) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            String name = str(row.get("name"));
            if (name.isEmpty()) {
                name = str(row.get("label"));
            }
            m.put("name", name.isEmpty() ? id : name);
            String currency = str(row.get("currency"));
            if (!currency.isEmpty()) {
                m.put("currency", currency.toUpperCase(Locale.ROOT));
            }
            out.add(m);
        }
        return out;
    }

    /**
     * NOTI 목록이 있으면 id·name(표시명) 기준으로 정규화. 목록이 없으면 trim만 반환.
     */
    public String resolveCanonicalId(String raw, List<Map<String, Object>> targets) {
        String v = raw != null ? raw.trim() : "";
        if (v.isEmpty()) {
            return "";
        }
        if (targets == null || targets.isEmpty()) {
            return v;
        }
        for (Map<String, Object> t : targets) {
            String id = str(t.get("id"));
            if (v.equalsIgnoreCase(id)) {
                return id;
            }
            String name = str(t.get("name"));
            if (!name.isEmpty() && v.equalsIgnoreCase(name)) {
                return id;
            }
        }
        return "";
    }

    public void assertRegistered(String raw, List<Map<String, Object>> targets) {
        String v = raw != null ? raw.trim() : "";
        if (v.isEmpty()) {
            return;
        }
        if (targets == null || targets.isEmpty()) {
            return;
        }
        if (resolveCanonicalId(v, targets).isEmpty()) {
            throw new IllegalArgumentException("등록되지 않은 NOTI 전산 대상 ID입니다: " + v);
        }
    }

    public List<Map<String, Object>> buildSelectOptions(List<Map<String, Object>> notiTargets,
                                                        HqNotifyEnvConfig cfg) {
        List<Map<String, Object>> opts = new ArrayList<>();
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("id", "");
        empty.put("label", "(선택)");
        opts.add(empty);
        if (notiTargets != null && !notiTargets.isEmpty()) {
            for (Map<String, Object> t : notiTargets) {
                String id = str(t.get("id"));
                if (id.isEmpty()) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", id);
                String name = str(t.get("name"));
                m.put("label", name.isEmpty() ? id : (id + " · " + name));
                opts.add(m);
            }
            return opts;
        }
        addCfgOpt(opts, "JPY", nz(cfg.getNotiProvisionInternalTargetJpy()));
        addCfgOpt(opts, "USD", nz(cfg.getNotiProvisionInternalTargetUsd()));
        String def = nz(cfg.getNotiProvisionDefaultInternalTargetId());
        if (!def.isEmpty() && opts.stream().noneMatch(o -> def.equals(o.get("id")))) {
            addCfgOpt(opts, "DEFAULT", def);
        }
        return opts;
    }

    private static void addCfgOpt(List<Map<String, Object>> opts, String labelKey, String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id.trim());
        m.put("label", labelKey + " · " + id.trim());
        opts.add(m);
    }

    private static String str(Object v) {
        return v != null ? String.valueOf(v).trim() : "";
    }

    private static String nz(String v) {
        return v != null ? v.trim() : "";
    }

    private static String yn(String v) {
        return "Y".equalsIgnoreCase(v != null ? v.trim() : "") ? "Y" : "N";
    }
}
