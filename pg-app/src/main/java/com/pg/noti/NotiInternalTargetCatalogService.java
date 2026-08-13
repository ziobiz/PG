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
        Map<String, Object> detailed = listFromNotiDetailed(cfg, acceptLanguage);
        Object items = detailed.get("items");
        if (items instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> row = (Map<String, Object>) m;
                    out.add(row);
                }
            }
            return out;
        }
        return List.of();
    }

    /**
     * NOTI internal-targets 조회 결과(상태·메시지 포함).
     * items / status / message / endpoint / httpStatus / provisionConfigured
     */
    public Map<String, Object> listFromNotiDetailed(HqNotifyEnvConfig cfg, String acceptLanguage) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", List.of());
        out.put("provisionConfigured", false);
        if (!isProvisionConfigured(cfg)) {
            out.put("status", "NOT_CONFIGURED");
            out.put("httpStatus", 0);
            out.put("endpoint", "");
            out.put("message",
                    "Provision API가 꺼져 있거나 베이스 URL·API 키가 없습니다. "
                            + "같은 화면 위쪽 「NOTI Provision API (노티생성 연동)」에서 사용=Y, 베이스 URL, API 키를 저장하세요. "
                            + "목록이 없어도 JPY/USD/THB 매핑 ID를 직접 넣으면 노티생성은 동작합니다.");
            return out;
        }
        out.put("provisionConfigured", true);
        Map<String, Object> detailed = notiProvisionClient.listInternalTargetsDetailed(
                cfg.getNotiProvisionBaseUrl(),
                cfg.getNotiProvisionApiKey(),
                acceptLanguage);
        String status = str(detailed.get("status"));
        out.put("status", status.isEmpty() ? "ERROR" : status);
        out.put("httpStatus", detailed.get("httpStatus") != null ? detailed.get("httpStatus") : 0);
        out.put("endpoint", str(detailed.get("endpoint")));
        out.put("message", str(detailed.get("message")));
        out.put("errorCode", str(detailed.get("errorCode")));

        List<Map<String, Object>> normalized = new ArrayList<>();
        Object rawItems = detailed.get("items");
        if (rawItems instanceof List<?> raw) {
            for (Object rowObj : raw) {
                if (!(rowObj instanceof Map<?, ?>)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> row = (Map<String, Object>) rowObj;
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
                String pgProvider = str(row.get("pgProvider"));
                if (pgProvider.isEmpty()) {
                    pgProvider = str(row.get("pgKind"));
                }
                if (!pgProvider.isEmpty()) {
                    m.put("pgProvider", pgProvider.toLowerCase(Locale.ROOT));
                }
                String apiLabel = str(row.get("label"));
                if (!apiLabel.isEmpty() && !apiLabel.equals(id) && !apiLabel.equals(name)) {
                    m.put("apiLabel", apiLabel);
                }
                normalized.add(m);
            }
        }
        out.put("items", normalized);
        if ("OK".equals(out.get("status")) && normalized.isEmpty()) {
            out.put("status", "EMPTY");
            out.put("message",
                    "NOTI 전산 대상이 비어 있습니다. NOTI 관리화면에서 internal-targets를 등록하거나, 위 JPY/USD/THB 매핑에 ID를 직접 입력하세요.");
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

    /**
     * NOTI 목록에서 통화(currency)가 일치하는 전산 대상 ID를 반환. 없으면 빈 문자열.
     */
    public String findIdByCurrency(List<Map<String, Object>> targets, String currency) {
        String cur = currency != null ? currency.trim().toUpperCase(Locale.ROOT) : "";
        if (cur.isEmpty() || targets == null || targets.isEmpty()) {
            return "";
        }
        for (Map<String, Object> t : targets) {
            if (t == null) {
                continue;
            }
            String ccy = str(t.get("currency")).toUpperCase(Locale.ROOT);
            if (ccy.isEmpty()) {
                continue;
            }
            if (ccy.equals(cur) || cur.contains(ccy) || ccy.contains(cur)) {
                String id = str(t.get("id"));
                if (!id.isEmpty()) {
                    return id;
                }
            }
        }
        return "";
    }

    public List<Map<String, Object>> buildSelectOptions(List<Map<String, Object>> notiTargets,
                                                        HqNotifyEnvConfig cfg) {
        return buildSelectOptions(notiTargets, cfg, null);
    }

    /**
     * @param pgKindFilter optional: jpay|elementpay|chillpay — null/blank = all
     */
    public List<Map<String, Object>> buildSelectOptions(List<Map<String, Object>> notiTargets,
                                                        HqNotifyEnvConfig cfg,
                                                        String pgKindFilter) {
        List<Map<String, Object>> opts = new ArrayList<>();
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("id", "");
        empty.put("label", "(선택)");
        opts.add(empty);
        String want = normalizePgFilter(pgKindFilter);
        if (notiTargets != null && !notiTargets.isEmpty()) {
            for (Map<String, Object> t : notiTargets) {
                String id = str(t.get("id"));
                if (id.isEmpty()) {
                    continue;
                }
                String pg = str(t.get("pgProvider")).toLowerCase(Locale.ROOT);
                if (!want.isEmpty() && !pg.isEmpty() && !want.equals(pg)) {
                    continue;
                }
                // When filtering ElementPay, skip chillpay/jpay rows; when rows lack pgProvider keep them.
                if (!want.isEmpty() && pg.isEmpty() && ("elementpay".equals(want) || "jpay".equals(want))) {
                    // keep untagged only for non-strict catalogs — for EP prefer tagged only
                    if ("elementpay".equals(want)) {
                        continue;
                    }
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", id);
                String name = str(t.get("name"));
                String ccy = str(t.get("currency")).toUpperCase(Locale.ROOT);
                String apiLabel = str(t.get("apiLabel"));
                String label;
                if (!apiLabel.isEmpty()) {
                    label = apiLabel;
                } else if (!ccy.isEmpty() && !name.isEmpty()) {
                    label = pgLabelPrefix(pg) + ccy + " · " + id + " · " + name;
                } else if (!ccy.isEmpty()) {
                    label = pgLabelPrefix(pg) + ccy + " · " + id;
                } else if (!name.isEmpty()) {
                    label = pgLabelPrefix(pg) + id + " · " + name;
                } else {
                    label = pgLabelPrefix(pg) + id;
                }
                m.put("label", label);
                if (!ccy.isEmpty()) {
                    m.put("currency", ccy);
                }
                if (!pg.isEmpty()) {
                    m.put("pgProvider", pg);
                }
                opts.add(m);
            }
            if (opts.size() > 1) {
                return opts;
            }
        }
        addCfgOpt(opts, "JPY", nz(cfg != null ? cfg.getNotiProvisionInternalTargetJpy() : null));
        addCfgOpt(opts, "USD", nz(cfg != null ? cfg.getNotiProvisionInternalTargetUsd() : null));
        addCfgOpt(opts, "THB", nz(cfg != null ? cfg.getNotiProvisionInternalTargetThb() : null));
        String def = nz(cfg != null ? cfg.getNotiProvisionDefaultInternalTargetId() : null);
        if (!def.isEmpty() && opts.stream().noneMatch(o -> def.equals(o.get("id")))) {
            addCfgOpt(opts, "DEFAULT", def);
        }
        return opts;
    }

    private static String normalizePgFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String k = raw.trim().toLowerCase(Locale.ROOT);
        if ("elementpay".equals(k) || "ep".equals(k) || "element".equals(k)) {
            return "elementpay";
        }
        if ("jpay".equals(k)) {
            return "jpay";
        }
        if ("chillpay".equals(k) || "chill".equals(k)) {
            return "chillpay";
        }
        return "";
    }

    private static String pgLabelPrefix(String pg) {
        if ("elementpay".equals(pg)) {
            return "ElementPay · ";
        }
        if ("jpay".equals(pg)) {
            return "JPAY · ";
        }
        if ("chillpay".equals(pg)) {
            return "ChillPay · ";
        }
        return "";
    }

    private static void addCfgOpt(List<Map<String, Object>> opts, String labelKey, String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id.trim());
        m.put("label", labelKey + " · " + id.trim());
        if (!"DEFAULT".equals(labelKey)) {
            m.put("currency", labelKey);
        }
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
