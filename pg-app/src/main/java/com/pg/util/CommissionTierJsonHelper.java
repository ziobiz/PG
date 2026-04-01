package com.pg.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 본사 기본 수수료 정책: 조직 단계별(총본사~가맹) 격자 JSON ↔ {@link CommissionPolicy} 스칼라(가맹 합계)·{@link DistributionFeeConfig}.
 */
public final class CommissionTierJsonHelper {

    private static final ObjectMapper OM = new ObjectMapper();

    public static final List<String> LEVEL_KEYS = List.of(
            "hq", "regional", "master", "branch", "agency", "salesOffice", "merchant");

    /** 가맹(merchant) 열 = 아래 6단계 합계 (merchant 제외) */
    private static final List<String> SUM_LEVEL_KEYS = List.of(
            "hq", "regional", "master", "branch", "agency", "salesOffice");

    /** 격자 행 키(프론트·API와 동일) */
    public static final List<String> ROW_KEYS = List.of(
            "payRate", "perTxFee", "failFee", "cancelRate", "voidFeePerTx", "manualVoidFeePerTx",
            "refundRate", "feeSettlementPerTx", "remittanceTransferFee", "usdtTransferFeeUsd",
            "feeUsdt", "feeFx", "usageRate", "fee3dsRate", "chargebackFeePerTx");

    private CommissionTierJsonHelper() {
    }

    public static boolean hasTierJson(String json) {
        return json != null && !json.isBlank();
    }

    /** 스칼라만 있는 정책용: 가맹 열에만 값, 나머지 빈 문자열 */
    public static String buildTierJsonFromPolicyScalars(CommissionPolicy p) {
        ObjectNode root = OM.createObjectNode();
        ObjectNode rows = OM.createObjectNode();
        for (String rk : ROW_KEYS) {
            rows.set(rk, merchantOnlyNode(stringScalarForRow(p, rk)));
        }
        root.set("rows", rows);
        var arr = OM.createArrayNode();
        for (int i = 1; i <= 4; i++) {
            arr.add(extraSlotToNode(p, i));
        }
        root.set("extras", arr);
        try {
            return OM.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"rows\":{},\"extras\":[]}";
        }
    }

    private static ObjectNode merchantOnlyNode(String merchantVal) {
        ObjectNode o = OM.createObjectNode();
        for (String lv : LEVEL_KEYS) {
            o.put(lv, "merchant".equals(lv) ? nz(merchantVal) : "");
        }
        return o;
    }

    private static ObjectNode extraSlotToNode(CommissionPolicy p, int slot) {
        String name = "";
        String mode = "";
        String mv = "0";
        switch (slot) {
            case 1 -> {
                name = nz(p.getExtraFee1Name());
                mode = nz(p.getExtraFee1Mode());
                mv = extraMerchantString(p.getExtraFee1Value(), p.getExtraFee1Mode());
            }
            case 2 -> {
                name = nz(p.getExtraFee2Name());
                mode = nz(p.getExtraFee2Mode());
                mv = extraMerchantString(p.getExtraFee2Value(), p.getExtraFee2Mode());
            }
            case 3 -> {
                name = nz(p.getExtraFee3Name());
                mode = nz(p.getExtraFee3Mode());
                mv = extraMerchantString(p.getExtraFee3Value(), p.getExtraFee3Mode());
            }
            case 4 -> {
                name = nz(p.getExtraFee4Name());
                mode = nz(p.getExtraFee4Mode());
                mv = extraMerchantString(p.getExtraFee4Value(), p.getExtraFee4Mode());
            }
            default -> {
            }
        }
        ObjectNode slotN = OM.createObjectNode();
        slotN.put("name", name);
        slotN.put("mode", mode);
        slotN.set("tiers", merchantOnlyNode(mv));
        return slotN;
    }

    private static String extraMerchantString(BigDecimal val, String mode) {
        if (val == null) {
            return "0";
        }
        if ("PCT".equalsIgnoreCase(mode != null ? mode : "")) {
            return PercentDecimalHelper.toPlainOneDecimal(val);
        }
        return PercentDecimalHelper.toPlainAmountOneDecimal(val);
    }

    private static String stringScalarForRow(CommissionPolicy p, String rowKey) {
        if (p == null) {
            return "";
        }
        return switch (rowKey) {
            case "payRate" -> p.getPayRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getPayRate()) : "";
            case "perTxFee" -> p.getPerTxFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getPerTxFee()) : "";
            case "failFee" -> p.getFailFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getFailFee()) : "";
            case "cancelRate" -> p.getCancelRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getCancelRate()) : "";
            case "voidFeePerTx" -> p.getVoidFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getVoidFeePerTx()) : "";
            case "manualVoidFeePerTx" -> p.getManualVoidFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getManualVoidFeePerTx()) : "";
            case "refundRate" -> p.getRefundRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getRefundRate()) : "";
            case "feeSettlementPerTx" -> p.getFeeSettlementPerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getFeeSettlementPerTx()) : "";
            case "remittanceTransferFee" -> p.getRemittanceTransferFee() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getRemittanceTransferFee()) : "";
            case "usdtTransferFeeUsd" -> p.getUsdtTransferFeeUsd() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getUsdtTransferFeeUsd()) : "";
            case "feeUsdt" -> p.getFeeUsdt() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeUsdt()) : "";
            case "feeFx" -> p.getFeeFx() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeFx()) : "";
            case "usageRate" -> p.getUsageRate() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getUsageRate()) : "";
            case "fee3dsRate" -> p.getFee3dsRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFee3dsRate()) : "";
            case "chargebackFeePerTx" -> p.getChargebackFeePerTx() != null ? PercentDecimalHelper.toPlainAmountOneDecimal(p.getChargebackFeePerTx()) : "";
            default -> "";
        };
    }

    /** 격자의 총본사~영업점 6단계 합 → 정책 스칼라 + 기타수수료 스칼라(가맹 열은 합계 표시용) */
    public static void applyTierJsonToPolicy(CommissionPolicy p, String json) throws Exception {
        if (json == null || json.isBlank()) {
            return;
        }
        JsonNode root = OM.readTree(json);
        JsonNode rows = root.get("rows");
        if (rows != null && rows.isObject()) {
            p.setPayRate(sumLevels(rows, "payRate", true));
            p.setPerTxFee(sumLevels(rows, "perTxFee", false));
            p.setFailFee(sumLevels(rows, "failFee", false));
            p.setCancelRate(sumLevels(rows, "cancelRate", false));
            p.setVoidFeePerTx(sumLevels(rows, "voidFeePerTx", false));
            p.setManualVoidFeePerTx(sumLevels(rows, "manualVoidFeePerTx", false));
            p.setRefundRate(sumLevels(rows, "refundRate", false));
            p.setFeeSettlementPerTx(sumLevels(rows, "feeSettlementPerTx", false));
            p.setRemittanceTransferFee(sumLevels(rows, "remittanceTransferFee", false));
            p.setUsdtTransferFeeUsd(sumLevels(rows, "usdtTransferFeeUsd", false));
            p.setFeeUsdt(sumLevels(rows, "feeUsdt", true));
            p.setFeeFx(sumLevels(rows, "feeFx", true));
            p.setUsageRate(sumLevels(rows, "usageRate", false));
            p.setFee3dsRate(sumLevels(rows, "fee3dsRate", true));
            p.setChargebackFeePerTx(sumLevels(rows, "chargebackFeePerTx", false));
        }
        JsonNode extras = root.get("extras");
        if (extras != null && extras.isArray()) {
            for (int i = 0; i < 4; i++) {
                JsonNode slot = extras.size() > i ? extras.get(i) : null;
                applyExtraSlotFromTier(p, i + 1, slot);
            }
        }
    }

    private static BigDecimal sumLevels(JsonNode rows, String rowKey, boolean pct) {
        JsonNode row = rows.get(rowKey);
        return sumSixLevels(row, pct);
    }

    private static BigDecimal sumSixLevels(JsonNode rowOrTiers, boolean pct) {
        if (rowOrTiers == null || !rowOrTiers.isObject()) {
            return BigDecimal.ZERO;
        }
        BigDecimal s = BigDecimal.ZERO;
        for (String lv : SUM_LEVEL_KEYS) {
            JsonNode n = rowOrTiers.get(lv);
            if (n == null || n.isNull()) {
                continue;
            }
            String t = n.asText("").trim();
            if (t.isEmpty()) {
                continue;
            }
            s = s.add(pct ? PercentDecimalHelper.parsePercentOneDecimal(t) : PercentDecimalHelper.parseAmountOneDecimal(t));
        }
        return s;
    }

    private static boolean isPctRowKey(String rowKey) {
        return switch (rowKey) {
            case "payRate", "feeUsdt", "feeFx", "fee3dsRate" -> true;
            default -> false;
        };
    }

    /**
     * 각 행·기타슬롯의 가맹(merchant) 필드를 6단계 합으로 덮어씁니다. 저장 JSON·API 응답과 목록 표시를 맞춥니다.
     */
    public static String normalizeTierJsonMerchantSums(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            JsonNode rootN = OM.readTree(json);
            if (!(rootN instanceof ObjectNode root)) {
                return json;
            }
            JsonNode rows = root.get("rows");
            if (rows != null && rows.isObject()) {
                ObjectNode rowsO = (ObjectNode) rows;
                for (String rk : ROW_KEYS) {
                    JsonNode row = rowsO.get(rk);
                    if (row == null || !row.isObject()) {
                        continue;
                    }
                    boolean pct = isPctRowKey(rk);
                    BigDecimal s = sumSixLevels(row, pct);
                    ((ObjectNode) row).put("merchant", PercentDecimalHelper.toPlainOneDecimal(s));
                }
            }
            JsonNode extras = root.get("extras");
            if (extras != null && extras.isArray()) {
                for (int i = 0; i < extras.size(); i++) {
                    JsonNode slot = extras.get(i);
                    if (slot == null || !slot.isObject()) {
                        continue;
                    }
                    String mode = normalizeExtraMode(text(slot, "mode"));
                    JsonNode tiers = slot.get("tiers");
                    if (tiers == null || !tiers.isObject() || mode == null) {
                        continue;
                    }
                    boolean pct = "PCT".equals(mode);
                    BigDecimal s = sumSixLevels(tiers, pct);
                    ((ObjectNode) tiers).put("merchant", PercentDecimalHelper.toPlainOneDecimal(s));
                }
            }
            return OM.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * tier_commission_json 이 있을 때 목록·폼 상단 스칼라 필드를 6단계 합과 동일하게 맞춥니다.
     */
    public static void applyTierJsonSumsToDisplayMap(Map<String, Object> m, CommissionPolicy p) {
        if (p == null || !hasTierJson(p.getTierCommissionJson())) {
            return;
        }
        try {
            JsonNode root = OM.readTree(p.getTierCommissionJson());
            JsonNode rows = root.get("rows");
            if (rows != null && rows.isObject()) {
                for (String rk : ROW_KEYS) {
                    boolean pct = isPctRowKey(rk);
                    JsonNode row = rows.get(rk);
                    if (row == null || !row.isObject()) {
                        continue;
                    }
                    BigDecimal s = sumSixLevels(row, pct);
                    m.put(rk, PercentDecimalHelper.toPlainOneDecimal(s));
                }
            }
            for (int i = 1; i <= 4; i++) {
                putExtraSumOnMapFromJson(m, root, i);
            }
        } catch (Exception ignored) {
        }
    }

    private static void putExtraSumOnMapFromJson(Map<String, Object> m, JsonNode root, int slot) {
        JsonNode extras = root.get("extras");
        if (extras == null || !extras.isArray() || extras.size() < slot) {
            return;
        }
        JsonNode node = extras.get(slot - 1);
        if (node == null || !node.isObject()) {
            return;
        }
        String name = text(node, "name");
        String mode = normalizeExtraMode(text(node, "mode"));
        if (name.isBlank() || mode == null) {
            return;
        }
        JsonNode tiers = node.get("tiers");
        if (tiers == null || !tiers.isObject()) {
            return;
        }
        boolean pct = "PCT".equals(mode);
        BigDecimal s = sumSixLevels(tiers, pct);
        m.put("extraFee" + slot + "Value", PercentDecimalHelper.toPlainOneDecimal(s));
    }

    private static void applyExtraSlotFromTier(CommissionPolicy p, int slot, JsonNode node) {
        if (node == null || node.isNull()) {
            clearExtra(p, slot);
            return;
        }
        String name = text(node, "name");
        String mode = normalizeExtraMode(text(node, "mode"));
        JsonNode tiers = node.get("tiers");
        if (name.isBlank() || mode == null) {
            clearExtra(p, slot);
            return;
        }
        BigDecimal val = sumSixLevels(tiers, "PCT".equals(mode));
        switch (slot) {
            case 1 -> {
                p.setExtraFee1Name(name.trim());
                p.setExtraFee1Mode(mode);
                p.setExtraFee1Value(val);
            }
            case 2 -> {
                p.setExtraFee2Name(name.trim());
                p.setExtraFee2Mode(mode);
                p.setExtraFee2Value(val);
            }
            case 3 -> {
                p.setExtraFee3Name(name.trim());
                p.setExtraFee3Mode(mode);
                p.setExtraFee3Value(val);
            }
            case 4 -> {
                p.setExtraFee4Name(name.trim());
                p.setExtraFee4Mode(mode);
                p.setExtraFee4Value(val);
            }
            default -> {
            }
        }
    }

    private static void clearExtra(CommissionPolicy p, int slot) {
        switch (slot) {
            case 1 -> {
                p.setExtraFee1Name(null);
                p.setExtraFee1Mode(null);
                p.setExtraFee1Value(null);
            }
            case 2 -> {
                p.setExtraFee2Name(null);
                p.setExtraFee2Mode(null);
                p.setExtraFee2Value(null);
            }
            case 3 -> {
                p.setExtraFee3Name(null);
                p.setExtraFee3Mode(null);
                p.setExtraFee3Value(null);
            }
            case 4 -> {
                p.setExtraFee4Name(null);
                p.setExtraFee4Mode(null);
                p.setExtraFee4Value(null);
            }
            default -> {
            }
        }
    }

    private static String normalizeExtraMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("PCT".equals(u) || "%".equals(u)) {
            return "PCT";
        }
        if ("FIX".equals(u) || "고정".equals(u)) {
            return "FIX";
        }
        return null;
    }

    /** 결제율·건당수수료 상위 6단계 → 배분 설정(가맹 합계는 정책 스칼라에 있음) */
    public static void applyTierJsonToDistribution(String json, DistributionFeeConfig df) {
        if (df == null || json == null || json.isBlank()) {
            return;
        }
        try {
            JsonNode root = OM.readTree(json);
            JsonNode rows = root.get("rows");
            if (rows == null || !rows.isObject()) {
                return;
            }
            df.setHqRate(parsePctTier(rows.get("payRate"), "hq"));
            df.setRegionalRate(parsePctTier(rows.get("payRate"), "regional"));
            df.setMasterRate(parsePctTier(rows.get("payRate"), "master"));
            df.setBranchRate(parsePctTier(rows.get("payRate"), "branch"));
            df.setAgencyRate(parsePctTier(rows.get("payRate"), "agency"));
            df.setSalesOfficeRate(parsePctTier(rows.get("payRate"), "salesOffice"));

            df.setHqPerTxFee(parseAmtTier(rows.get("perTxFee"), "hq"));
            df.setRegionalPerTxFee(parseAmtTier(rows.get("perTxFee"), "regional"));
            df.setMasterPerTxFee(parseAmtTier(rows.get("perTxFee"), "master"));
            df.setBranchPerTxFee(parseAmtTier(rows.get("perTxFee"), "branch"));
            df.setAgencyPerTxFee(parseAmtTier(rows.get("perTxFee"), "agency"));
            df.setSalesOfficePerTxFee(parseAmtTier(rows.get("perTxFee"), "salesOffice"));
        } catch (Exception ignored) {
        }
    }

    private static BigDecimal parsePctTier(JsonNode row, String level) {
        if (row == null || !row.isObject()) {
            return BigDecimal.ZERO;
        }
        JsonNode n = row.get(level);
        if (n == null || n.isNull()) {
            return BigDecimal.ZERO;
        }
        String s = n.asText("").trim();
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return PercentDecimalHelper.parsePercentOneDecimal(s);
    }

    private static BigDecimal parseAmtTier(JsonNode row, String level) {
        if (row == null || !row.isObject()) {
            return BigDecimal.ZERO;
        }
        JsonNode n = row.get(level);
        if (n == null || n.isNull()) {
            return BigDecimal.ZERO;
        }
        String s = n.asText("").trim();
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return PercentDecimalHelper.parseAmountOneDecimal(s);
    }

    public static Map<String, Object> parseTierJsonToMap(String json) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            out.put("rows", Map.of());
            out.put("extras", List.of());
            return out;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = OM.readValue(json, Map.class);
            return m;
        } catch (Exception e) {
            out.put("rows", Map.of());
            out.put("extras", List.of());
            return out;
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return "";
        }
        JsonNode n = node.get(field);
        return n == null || n.isNull() ? "" : n.asText("");
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }
}
