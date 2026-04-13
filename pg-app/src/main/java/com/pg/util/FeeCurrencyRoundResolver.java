package com.pg.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.entity.HqLedgerSysSettings;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 전산설정 JSON + 전역 수수료 소수 설정을 합쳐 통화별 {@link FeeListRoundingPolicy} 를 제공합니다.
 * JSON 예: [{"currency":"THB","decimalPlaces":2,"roundMode":"CEILING"}, ...]
 */
public final class FeeCurrencyRoundResolver {

    private static final ObjectMapper OM = new ObjectMapper();

    /** 표시·편집 기본 통화(알파 코드) */
    public static final List<String> DEFAULT_CURRENCY_ORDER = List.of("KRW", "USD", "JPY", "THB", "SGD");

    private final FeeListRoundingPolicy fallback;
    private final Map<String, FeeListRoundingPolicy> byCurrencyUpper;

    private FeeCurrencyRoundResolver(FeeListRoundingPolicy fallback, Map<String, FeeListRoundingPolicy> byCurrencyUpper) {
        this.fallback = fallback != null ? fallback : FeeListRoundingPolicy.defaults();
        this.byCurrencyUpper = byCurrencyUpper != null ? byCurrencyUpper : Map.of();
    }

    public static FeeCurrencyRoundResolver from(HqLedgerSysSettings s) {
        FeeListRoundingPolicy fb = FeeListRoundingPolicy.fromSettings(s);
        Map<String, FeeListRoundingPolicy> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (s != null && s.getFeeCurrencyFormatJson() != null && !s.getFeeCurrencyFormatJson().isBlank()) {
            mergeJsonInto(map, s.getFeeCurrencyFormatJson(), fb);
        }
        for (String c : DEFAULT_CURRENCY_ORDER) {
            map.putIfAbsent(c, fb);
        }
        return new FeeCurrencyRoundResolver(fb, map);
    }

    private static void mergeJsonInto(Map<String, FeeListRoundingPolicy> map, String json, FeeListRoundingPolicy fb) {
        try {
            JsonNode root = OM.readTree(json);
            if (!root.isArray()) {
                return;
            }
            for (JsonNode n : root) {
                if (n == null || !n.isObject()) {
                    continue;
                }
                String cur = text(n, "currency");
                if (cur == null || cur.isBlank()) {
                    continue;
                }
                String u = cur.trim().toUpperCase(Locale.ROOT);
                int dp = fb.decimalPlaces();
                JsonNode dpNode = n.get("decimalPlaces");
                if (dpNode != null && dpNode.isNumber()) {
                    dp = Math.min(8, Math.max(0, dpNode.intValue()));
                } else if (dpNode != null && dpNode.isTextual()) {
                    try {
                        dp = Math.min(8, Math.max(0, Integer.parseInt(dpNode.asText().trim())));
                    } catch (NumberFormatException ignored) {
                        dp = fb.decimalPlaces();
                    }
                }
                RoundingMode rm = FeeListRoundingPolicy.parseRoundMode(text(n, "roundMode"));
                map.put(u, new FeeListRoundingPolicy(dp, rm));
            }
        } catch (Exception ignored) {
            // leave map partial / empty
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText(null);
    }

    /**
     * 클라이언트 금액 표시용: 통화(대문자) → decimalPlaces, roundMode(알파벳 대문자)
     */
    public Map<String, Map<String, Object>> toClientByCurrencyMap() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map.Entry<String, FeeListRoundingPolicy> e : byCurrencyUpper.entrySet()) {
            FeeListRoundingPolicy p = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("decimalPlaces", p.decimalPlaces());
            row.put("roundMode", roundingLabel(p.roundMode()));
            out.put(e.getKey().toUpperCase(Locale.ROOT), row);
        }
        return out;
    }

    private static String roundingLabel(RoundingMode m) {
        if (m == RoundingMode.HALF_UP) {
            return "HALF_UP";
        }
        if (m == RoundingMode.DOWN) {
            return "DOWN";
        }
        return "CEILING";
    }

    public FeeListRoundingPolicy forCurrency(String currencyAlpha) {
        if (currencyAlpha == null || currencyAlpha.isBlank()) {
            return fallback;
        }
        FeeListRoundingPolicy p = byCurrencyUpper.get(currencyAlpha.trim().toUpperCase(Locale.ROOT));
        return p != null ? p : fallback;
    }

    public FeeListRoundingPolicy fallback() {
        return fallback;
    }

    /**
     * 전산설정 화면 표용: 기본 통화 순 + JSON에만 있는 추가 통화.
     */
    public List<Map<String, Object>> toDisplayRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        LinkedHashMap<String, FeeListRoundingPolicy> ordered = new LinkedHashMap<>();
        for (String c : DEFAULT_CURRENCY_ORDER) {
            ordered.put(c, forCurrency(c));
        }
        for (Map.Entry<String, FeeListRoundingPolicy> e : byCurrencyUpper.entrySet()) {
            ordered.putIfAbsent(e.getKey().toUpperCase(Locale.ROOT), e.getValue());
        }
        for (Map.Entry<String, FeeListRoundingPolicy> e : ordered.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("currency", e.getKey());
            row.put("decimalPlaces", e.getValue().decimalPlaces());
            row.put("roundMode", roundingLabel(e.getValue().roundMode()));
            rows.add(row);
        }
        return rows;
    }

    public static List<Map<String, Object>> buildDisplayRows(HqLedgerSysSettings s) {
        return from(s).toDisplayRows();
    }

    /**
     * 저장 본문 정규화: 알파 통화 3~8자, 소수 0~8, roundMode 화이트리스트.
     */
    public static String normalizePolicyJson(String rawJson, HqLedgerSysSettings s) {
        FeeListRoundingPolicy fb = FeeListRoundingPolicy.fromSettings(s);
        ArrayNode arr = OM.createArrayNode();
        if (rawJson == null || rawJson.isBlank()) {
            for (String c : DEFAULT_CURRENCY_ORDER) {
                arr.add(rowNode(c, fb.decimalPlaces(), roundingLabel(fb.roundMode())));
            }
            return arr.toString();
        }
        try {
            JsonNode root = OM.readTree(rawJson);
            if (!root.isArray()) {
                throw new IllegalArgumentException("feeCurrencyFormatJson 은 배열(JSON)이어야 합니다.");
            }
            LinkedHashMap<String, ObjectNode> byCur = new LinkedHashMap<>();
            for (JsonNode n : root) {
                if (n == null || !n.isObject()) {
                    continue;
                }
                String cur = text(n, "currency");
                if (cur == null || cur.isBlank()) {
                    continue;
                }
                String u = cur.trim().toUpperCase(Locale.ROOT);
                if (u.length() < 3 || u.length() > 8 || !u.matches("[A-Z0-9]+")) {
                    throw new IllegalArgumentException("통화 코드가 올바르지 않습니다: " + cur);
                }
                int dp = fb.decimalPlaces();
                JsonNode dpNode = n.get("decimalPlaces");
                if (dpNode != null && dpNode.isNumber()) {
                    dp = Math.min(8, Math.max(0, dpNode.intValue()));
                } else if (dpNode != null && dpNode.isTextual()) {
                    try {
                        dp = Math.min(8, Math.max(0, Integer.parseInt(dpNode.asText().trim())));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("decimalPlaces 가 숫자가 아닙니다: " + dpNode.asText());
                    }
                }
                String rm = text(n, "roundMode");
                if (rm == null || rm.isBlank()) {
                    rm = roundingLabel(fb.roundMode());
                } else {
                    rm = rm.trim().toUpperCase(Locale.ROOT);
                    if (!List.of("CEILING", "HALF_UP", "DOWN").contains(rm)) {
                        throw new IllegalArgumentException("roundMode 는 CEILING, HALF_UP, DOWN 중 하나여야 합니다.");
                    }
                }
                if (dp == 0) {
                    rm = "DOWN";
                }
                byCur.put(u, rowNode(u, dp, rm));
            }
            for (String c : DEFAULT_CURRENCY_ORDER) {
                if (!byCur.containsKey(c)) {
                    byCur.put(c, rowNode(c, fb.decimalPlaces(), roundingLabel(fb.roundMode())));
                }
            }
            TreeSet<String> extras = new TreeSet<>(byCur.keySet());
            extras.removeAll(DEFAULT_CURRENCY_ORDER);
            for (String c : DEFAULT_CURRENCY_ORDER) {
                if (byCur.containsKey(c)) {
                    arr.add(byCur.get(c));
                }
            }
            for (String c : extras) {
                arr.add(byCur.get(c));
            }
            return arr.toString();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("feeCurrencyFormatJson 파싱 실패: " + e.getMessage());
        }
    }

    private static ObjectNode rowNode(String currency, int dp, String rm) {
        ObjectNode o = OM.createObjectNode();
        o.put("currency", currency);
        o.put("decimalPlaces", dp);
        o.put("roundMode", rm);
        return o;
    }
}
