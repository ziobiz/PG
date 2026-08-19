package com.pg.service;

import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.PgAgency;
import com.pg.entity.PgAgencyCostPolicy;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.PgAgencyCostPolicyRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.util.PercentDecimalHelper;
import com.pg.util.VoidRefundSettlementModeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PgAgencyCostPolicyService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final PgAgencyCostPolicyRepository policyRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;

    public PgAgencyCostPolicyService(PgAgencyCostPolicyRepository policyRepository,
                                     PgAgencyRepository pgAgencyRepository,
                                     ChargebackFeePolicyRepository chargebackFeePolicyRepository) {
        this.policyRepository = policyRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
    }

    public Map<String, Object> bootstrap() {
        Map<Long, String> chargebackNames = chargebackFeePolicyRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(ChargebackFeePolicy::getId, ChargebackFeePolicy::getName, (a, b) -> a));
        Map<String, String> pgNmByCd = pgAgencyRepository.findAllByOrderByPgCdAsc().stream()
                .collect(Collectors.toMap(PgAgency::getPgCd, PgAgency::getPgNm, (a, b) -> a));
        List<Map<String, Object>> pgOptions = pgAgencyRepository.findAllByOrderByPgCdAsc().stream()
                .map(a -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("pgCd", a.getPgCd());
                    o.put("pgNm", a.getPgNm());
                    o.put("merchantMid", a.getMerchantMid() != null ? a.getMerchantMid() : "");
                    o.put("useYn", a.getUseYn() != null ? a.getUseYn() : "Y");
                    return o;
                })
                .toList();
        List<Map<String, Object>> policies = policyRepository.findAllByOrderByPgCdAsc().stream()
                .map(p -> toSummaryMap(p, pgNmByCd, chargebackNames))
                .toList();
        List<Map<String, Object>> chargebackOptions = chargebackFeePolicyRepository.findAllByOrderByNameAsc().stream()
                .map(cb -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", cb.getId());
                    m.put("name", cb.getName());
                    return m;
                })
                .toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pgOptions", pgOptions);
        out.put("policies", policies);
        out.put("chargebackOptions", chargebackOptions);
        out.put("defaults", emptyDefaults());
        return out;
    }

    public Map<String, Object> getByPgCd(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return emptyDefaults();
        }
        String cd = pgCd.trim().toUpperCase(Locale.ROOT);
        return policyRepository.findByPgCd(cd)
                .map(this::toDetailMap)
                .orElseGet(() -> {
                    Map<String, Object> d = emptyDefaults();
                    d.put("pgCd", cd);
                    pgAgencyRepository.findByPgCd(cd).ifPresent(a -> d.put("pgNm", a.getPgNm()));
                    return d;
                });
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        String pgCdRaw = str(body, "pgCd");
        if (pgCdRaw == null || pgCdRaw.isBlank()) {
            throw new IllegalArgumentException("결제코드를 선택하세요.");
        }
        String pgCd = pgCdRaw.trim().toUpperCase(Locale.ROOT);
        if (pgAgencyRepository.findByPgCd(pgCd).isEmpty()) {
            throw new IllegalArgumentException("등록된 PG대행사가 없습니다. API연동설정에서 먼저 등록하세요.");
        }
        PgAgencyCostPolicy p = policyRepository.findByPgCd(pgCd).orElseGet(() -> {
            PgAgencyCostPolicy n = new PgAgencyCostPolicy();
            n.setPgCd(pgCd);
            return n;
        });
        applyScalarsFromBody(p, body);
        p.setSettleBasis("TRANSACTION");
        String sched = str(body, "settleScheduleType");
        if (sched != null && !sched.isBlank()) {
            String u = sched.trim().toUpperCase(Locale.ROOT);
            if (!Set.of("T", "H", "D").contains(u)) {
                throw new IllegalArgumentException("정산 주기 유형은 T, H, D 중 하나여야 합니다.");
            }
            p.setSettleScheduleType(u);
        }
        Object lag = body.get("settleLagN");
        int lagN = lag != null && !lag.toString().isBlank() ? Integer.parseInt(lag.toString().trim()) : 1;
        if (lagN < 1 || lagN > 30) {
            throw new IllegalArgumentException("정산 N은 1~30 사이여야 합니다.");
        }
        p.setSettleLagN(lagN);
        if ("D".equalsIgnoreCase(p.getSettleScheduleType())) {
            String bt = str(body, "settleBatchTime");
            if (bt == null || bt.isBlank()) {
                throw new IllegalArgumentException("D(달력일) 모드는 일괄 시각(HH:mm)이 필요합니다.");
            }
            p.setSettleBatchTime(LocalTime.parse(bt.trim(), TIME_FMT));
        } else {
            p.setSettleBatchTime(null);
        }
        String useYn = str(body, "useYn");
        p.setUseYn("N".equalsIgnoreCase(useYn) ? "N" : "Y");
        policyRepository.save(p);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "저장되었습니다.");
        res.put("policy", toDetailMap(p));
        return res;
    }

    private Map<String, Object> emptyDefaults() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pgCd", "");
        m.put("perTxFee", "0.0");
        m.put("usageRate", "0.0");
        m.put("failFee", "0.0");
        m.put("cancelRate", "0.0");
        m.put("voidFeePerTx", "0");
        m.put("manualVoidFeePerTx", "0");
        m.put("refundRate", "0");
        m.put("payRate", "0");
        m.put("feeSettlementPerTx", "0");
        m.put("remittanceTransferFee", "0.0");
        m.put("usdtTransferFeeUsd", "0.0");
        m.put("feeUsdt", "0");
        m.put("feeFx", "0");
        m.put("rollingPct", "0");
        m.put("rollingDays", "0");
        m.put("currencyCode", "KRW");
        m.put("policyRemark", "");
        m.put("fee3dsRate", "0.0");
        m.put("chargebackFeePerTx", "0.0");
        m.put("chargebackPolicyId", "");
        m.put("voidSettlementMode", VoidRefundSettlementModeUtil.GENERAL);
        m.put("manualVoidSettlementMode", VoidRefundSettlementModeUtil.GENERAL);
        m.put("refundSettlementMode", VoidRefundSettlementModeUtil.GENERAL);
        m.put("forceRefundSettlementMode", VoidRefundSettlementModeUtil.GENERAL);
        putExtraFeeScalarsOnMap(m, null);
        m.put("settleBasis", "TRANSACTION");
        m.put("settleScheduleType", "T");
        m.put("settleLagN", 1);
        m.put("settleBatchTime", "");
        m.put("useYn", "Y");
        return m;
    }

    private void applyScalarsFromBody(PgAgencyCostPolicy p, Map<String, Object> body) {
        p.setPerTxFee(PercentDecimalHelper.parseAmountOneDecimal(body.get("perTxFee")));
        p.setUsageRate(PercentDecimalHelper.parseAmountOneDecimal(body.get("usageRate")));
        p.setFailFee(PercentDecimalHelper.parseAmountOneDecimal(body.get("failFee")));
        p.setCancelRate(PercentDecimalHelper.parseAmountOneDecimal(body.get("cancelRate")));
        p.setVoidFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("voidFeePerTx")));
        p.setManualVoidFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("manualVoidFeePerTx")));
        p.setRefundRate(PercentDecimalHelper.parseAmountOneDecimal(body.get("refundRate")));
        p.setPayRate(PercentDecimalHelper.parsePercentOneDecimal(body.get("payRate")));
        p.setFeeSettlementPerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("feeSettlementPerTx")));
        p.setRemittanceTransferFee(PercentDecimalHelper.parseAmountOneDecimal(body.get("remittanceTransferFee")));
        p.setUsdtTransferFeeUsd(PercentDecimalHelper.parseAmountOneDecimal(body.get("usdtTransferFeeUsd")));
        p.setFeeUsdt(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeUsdt")));
        p.setFeeFx(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeFx")));
        p.setFee3dsRate(PercentDecimalHelper.parsePercentOneDecimal(body.get("fee3dsRate")));
        p.setChargebackFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("chargebackFeePerTx")));
        applyExtraFeesFromBody(p, body);
        p.setRollingPct(PercentDecimalHelper.parsePercentOneDecimal(body.get("rollingPct")));
        Object rd = body.get("rollingDays");
        p.setRollingDays(rd != null && !rd.toString().isEmpty() ? Integer.parseInt(rd.toString()) : 0);
        String cc = str(body, "currencyCode");
        p.setCurrencyCode(cc != null && !cc.isBlank() ? cc.trim().toUpperCase(Locale.ROOT) : "KRW");
        p.setPolicyRemark(str(body, "policyRemark"));
        p.setChargebackPolicyId(parseOptionalLong(body.get("chargebackPolicyId")));
        if (body.containsKey("voidSettlementMode")) {
            p.setVoidSettlementMode(parseSettlementMode(body.get("voidSettlementMode")));
        }
        if (body.containsKey("manualVoidSettlementMode")) {
            p.setManualVoidSettlementMode(parseSettlementMode(body.get("manualVoidSettlementMode")));
        }
        if (body.containsKey("refundSettlementMode")) {
            p.setRefundSettlementMode(parseSettlementMode(body.get("refundSettlementMode")));
        }
        if (body.containsKey("forceRefundSettlementMode")) {
            p.setForceRefundSettlementMode(parseSettlementMode(body.get("forceRefundSettlementMode")));
        }
    }

    private Map<String, Object> toDetailMap(PgAgencyCostPolicy p) {
        Map<Long, String> chargebackNames = chargebackFeePolicyRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(ChargebackFeePolicy::getId, ChargebackFeePolicy::getName, (a, b) -> a));
        return toDetailMap(p, chargebackNames);
    }

    private Map<String, Object> toDetailMap(PgAgencyCostPolicy p, Map<Long, String> chargebackNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("pgCd", p.getPgCd());
        pgAgencyRepository.findByPgCd(p.getPgCd()).ifPresent(a -> m.put("pgNm", a.getPgNm()));
        m.put("perTxFee", plainAmt(p.getPerTxFee()));
        m.put("usageRate", plainAmt(p.getUsageRate()));
        m.put("failFee", plainAmt(p.getFailFee()));
        m.put("cancelRate", plainAmt(p.getCancelRate()));
        m.put("voidFeePerTx", plainAmt(p.getVoidFeePerTx()));
        m.put("manualVoidFeePerTx", plainAmt(p.getManualVoidFeePerTx()));
        m.put("refundRate", plainAmt(p.getRefundRate()));
        m.put("payRate", p.getPayRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getPayRate()) : "0");
        m.put("feeSettlementPerTx", plainAmt(p.getFeeSettlementPerTx()));
        m.put("remittanceTransferFee", plainAmt(p.getRemittanceTransferFee()));
        m.put("usdtTransferFeeUsd", plainAmt(p.getUsdtTransferFeeUsd()));
        m.put("feeUsdt", p.getFeeUsdt() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeUsdt()) : "0");
        m.put("feeFx", p.getFeeFx() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeFx()) : "0");
        m.put("rollingPct", p.getRollingPct() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getRollingPct()) : "0");
        m.put("rollingDays", p.getRollingDays() != null ? p.getRollingDays() : 0);
        m.put("currencyCode", p.getCurrencyCode() != null && !p.getCurrencyCode().isBlank() ? p.getCurrencyCode() : "KRW");
        m.put("policyRemark", p.getPolicyRemark() != null ? p.getPolicyRemark() : "");
        m.put("fee3dsRate", plainAmt(p.getFee3dsRate()));
        m.put("chargebackFeePerTx", plainAmt(p.getChargebackFeePerTx()));
        m.put("chargebackPolicyId", p.getChargebackPolicyId() != null ? p.getChargebackPolicyId() : "");
        if (p.getChargebackPolicyId() != null && chargebackNames.containsKey(p.getChargebackPolicyId())) {
            m.put("chargebackPolicyName", chargebackNames.get(p.getChargebackPolicyId()));
        } else {
            m.put("chargebackPolicyName", "");
        }
        m.put("voidSettlementMode", settlementModeForApi(p.getVoidSettlementMode()));
        m.put("manualVoidSettlementMode", settlementModeForApi(p.getManualVoidSettlementMode()));
        m.put("refundSettlementMode", settlementModeForApi(p.getRefundSettlementMode()));
        m.put("forceRefundSettlementMode", settlementModeForApi(p.getForceRefundSettlementMode()));
        putExtraFeeScalarsOnMap(m, p);
        m.put("settleBasis", p.getSettleBasis() != null ? p.getSettleBasis() : "TRANSACTION");
        m.put("settleScheduleType", p.getSettleScheduleType() != null ? p.getSettleScheduleType() : "T");
        m.put("settleLagN", p.getSettleLagN() != null ? p.getSettleLagN() : 1);
        m.put("settleBatchTime", p.getSettleBatchTime() != null ? p.getSettleBatchTime().format(TIME_FMT) : "");
        m.put("useYn", p.getUseYn() != null ? p.getUseYn() : "Y");
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> toSummaryMap(PgAgencyCostPolicy p, Map<String, String> pgNmByCd, Map<Long, String> chargebackNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pgCd", p.getPgCd());
        m.put("pgNm", pgNmByCd.getOrDefault(p.getPgCd(), p.getPgCd()));
        m.put("currencyCode", p.getCurrencyCode());
        m.put("payRate", p.getPayRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getPayRate()) : "0");
        m.put("rollingPct", p.getRollingPct() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getRollingPct()) : "0");
        m.put("rollingDays", p.getRollingDays() != null ? p.getRollingDays() : 0);
        m.put("settleScheduleLabel", settleScheduleLabel(p));
        m.put("useYn", p.getUseYn());
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
        if (p.getChargebackPolicyId() != null && chargebackNames.containsKey(p.getChargebackPolicyId())) {
            m.put("chargebackPolicyName", chargebackNames.get(p.getChargebackPolicyId()));
        }
        return m;
    }

    private static String settleScheduleLabel(PgAgencyCostPolicy p) {
        String t = p.getSettleScheduleType() != null ? p.getSettleScheduleType() : "T";
        int n = p.getSettleLagN() != null ? p.getSettleLagN() : 1;
        if ("D".equalsIgnoreCase(t)) {
            String tm = p.getSettleBatchTime() != null ? p.getSettleBatchTime().format(TIME_FMT) : "";
            return "D+" + n + (tm.isEmpty() ? "" : " " + tm);
        }
        if ("H".equalsIgnoreCase(t)) {
            return "H×" + n + " (24h)";
        }
        return "T+" + n;
    }

    private static String plainAmt(BigDecimal b) {
        return b != null ? PercentDecimalHelper.toPlainAmountOneDecimal(b) : "0.0";
    }

    private static String settlementModeForApi(String v) {
        if (v == null || v.isBlank()) {
            return VoidRefundSettlementModeUtil.GENERAL;
        }
        return VoidRefundSettlementModeUtil.normalize(v.trim());
    }

    private static String parseSettlementMode(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        if (s.isEmpty() || "FOLLOW".equalsIgnoreCase(s)) {
            return null;
        }
        return VoidRefundSettlementModeUtil.normalize(s);
    }

    private static Long parseOptionalLong(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            long v = Long.parseLong(s);
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString() : null;
    }

    private static void putExtraFeeScalarsOnMap(Map<String, Object> data, PgAgencyCostPolicy p) {
        for (int i = 1; i <= 4; i++) {
            String name = "";
            String mode = "";
            String val = "0";
            if (p != null) {
                switch (i) {
                    case 1 -> {
                        name = nz(p.getExtraFee1Name());
                        mode = nz(p.getExtraFee1Mode());
                        val = extraValStr(p.getExtraFee1Value(), mode);
                    }
                    case 2 -> {
                        name = nz(p.getExtraFee2Name());
                        mode = nz(p.getExtraFee2Mode());
                        val = extraValStr(p.getExtraFee2Value(), mode);
                    }
                    case 3 -> {
                        name = nz(p.getExtraFee3Name());
                        mode = nz(p.getExtraFee3Mode());
                        val = extraValStr(p.getExtraFee3Value(), mode);
                    }
                    case 4 -> {
                        name = nz(p.getExtraFee4Name());
                        mode = nz(p.getExtraFee4Mode());
                        val = extraValStr(p.getExtraFee4Value(), mode);
                    }
                    default -> {
                    }
                }
            }
            data.put("extraFee" + i + "Name", name);
            data.put("extraFee" + i + "Mode", mode);
            data.put("extraFee" + i + "Value", val);
        }
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private static String extraValStr(BigDecimal b, String mode) {
        if (b == null) {
            return "0";
        }
        if ("PCT".equalsIgnoreCase(mode)) {
            return PercentDecimalHelper.toPlainOneDecimal(b);
        }
        return b.stripTrailingZeros().toPlainString();
    }

    private void applyExtraFeesFromBody(PgAgencyCostPolicy p, Map<String, Object> body) {
        for (int i = 1; i <= 4; i++) {
            applyExtraFeeSlot(p, i, body);
        }
    }

    private void applyExtraFeeSlot(PgAgencyCostPolicy p, int slot, Map<String, Object> body) {
        String nk = "extraFee" + slot + "Name";
        String mk = "extraFee" + slot + "Mode";
        String vk = "extraFee" + slot + "Value";
        String name = str(body, nk);
        String modeNorm = normalizeExtraMode(str(body, mk));
        BigDecimal val = "PCT".equals(modeNorm)
                ? PercentDecimalHelper.parsePercentOneDecimal(body.get(vk))
                : PercentDecimalHelper.parseAmountOneDecimal(body.get(vk));
        if (name == null || name.isBlank() || modeNorm == null) {
            clearExtraFeeSlot(p, slot);
            return;
        }
        String trimmed = name.trim();
        if (trimmed.length() > 64) {
            trimmed = trimmed.substring(0, 64);
        }
        setExtraFeeSlot(p, slot, trimmed, modeNorm, val);
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

    private static void clearExtraFeeSlot(PgAgencyCostPolicy p, int slot) {
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

    private static void setExtraFeeSlot(PgAgencyCostPolicy p, int slot, String name, String mode, BigDecimal val) {
        switch (slot) {
            case 1 -> {
                p.setExtraFee1Name(name);
                p.setExtraFee1Mode(mode);
                p.setExtraFee1Value(val);
            }
            case 2 -> {
                p.setExtraFee2Name(name);
                p.setExtraFee2Mode(mode);
                p.setExtraFee2Value(val);
            }
            case 3 -> {
                p.setExtraFee3Name(name);
                p.setExtraFee3Mode(mode);
                p.setExtraFee3Value(val);
            }
            case 4 -> {
                p.setExtraFee4Name(name);
                p.setExtraFee4Mode(mode);
                p.setExtraFee4Value(val);
            }
            default -> {
            }
        }
    }
}
