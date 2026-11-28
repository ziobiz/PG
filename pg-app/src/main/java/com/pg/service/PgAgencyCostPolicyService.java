package com.pg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.ChargebackFeePolicy;
import com.pg.entity.PgAgencyCostPolicy;
import com.pg.entity.PgAgencyCostPolicyHist;
import com.pg.repository.ChargebackFeePolicyRepository;
import com.pg.repository.PgAgencyCostPolicyHistRepository;
import com.pg.repository.PgAgencyCostPolicyRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.util.PercentDecimalHelper;
import com.pg.util.VoidRefundSettlementModeUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PgAgencyCostPolicyService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ObjectMapper OM = new ObjectMapper();

    private final PgAgencyCostPolicyRepository policyRepository;
    private final PgAgencyCostPolicyHistRepository histRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final ChargebackFeePolicyRepository chargebackFeePolicyRepository;

    public PgAgencyCostPolicyService(PgAgencyCostPolicyRepository policyRepository,
                                     PgAgencyCostPolicyHistRepository histRepository,
                                     PgAgencyRepository pgAgencyRepository,
                                     ChargebackFeePolicyRepository chargebackFeePolicyRepository) {
        this.policyRepository = policyRepository;
        this.histRepository = histRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.chargebackFeePolicyRepository = chargebackFeePolicyRepository;
    }

    public Map<String, Object> bootstrap() {
        Map<Long, String> chargebackNames = chargebackFeePolicyRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(ChargebackFeePolicy::getId, ChargebackFeePolicy::getName, (a, b) -> a));
        List<Map<String, Object>> policies = policyRepository.findAllByOrderByPgCdAsc().stream()
                .map(p -> toSummaryMap(p, chargebackNames))
                .toList();
        List<Map<String, Object>> costOptions = policyRepository.findAllByOrderByPgCdAsc().stream()
                .filter(p -> p.getUseYn() == null || !"N".equalsIgnoreCase(p.getUseYn().trim()))
                .map(p -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("id", p.getId());
                    o.put("costNm", displayCostNm(p));
                    o.put("vendorNm", nz(p.getVendorNm()));
                    o.put("pgCd", p.getPgCd());
                    o.put("currencyCode", nz(p.getCurrencyCode()));
                    o.put("displayCurrency", displayCur(p));
                    o.put("label", costOptionLabel(p));
                    return o;
                })
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
        out.put("policies", policies);
        out.put("costOptions", costOptions);
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
                    return d;
                });
    }

    public Map<String, Object> getById(Long id) {
        if (id == null || id <= 0) {
            return emptyDefaults();
        }
        return policyRepository.findById(id).map(this::toDetailMap).orElseGet(this::emptyDefaults);
    }

    public List<Map<String, Object>> history(Long policyId) {
        if (policyId == null || policyId <= 0) {
            return List.of();
        }
        return histRepository.findByPolicyIdOrderByCreatedAtDesc(policyId).stream()
                .map(h -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", h.getId());
                    m.put("policyId", h.getPolicyId());
                    m.put("costNm", h.getCostNm());
                    m.put("vendorNm", h.getVendorNm());
                    m.put("pgCd", h.getPgCd());
                    m.put("changedBy", h.getChangedBy() != null ? h.getChangedBy() : "");
                    m.put("createdAt", h.getCreatedAt() != null ? h.getCreatedAt().toString() : "");
                    m.put("snapshotJson", h.getSnapshotJson());
                    return m;
                })
                .toList();
    }

    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        String costNm = str(body, "costNm");
        String vendorNm = str(body, "vendorNm");
        String pgCdRaw = str(body, "pgCd");
        if (vendorNm == null || vendorNm.isBlank()) {
            throw new IllegalArgumentException("계약대행사가 없습니다.");
        }
        if (pgCdRaw == null || pgCdRaw.isBlank()) {
            throw new IllegalArgumentException("결제코드가 없습니다.");
        }
        String chargeCur = str(body, "currencyCode");
        if (chargeCur == null || chargeCur.isBlank()) {
            chargeCur = str(body, "chargeCurrency");
        }
        if (chargeCur == null || chargeCur.isBlank()) {
            throw new IllegalArgumentException("청구통화가 없습니다.");
        }
        String multiYn = "Y".equalsIgnoreCase(str(body, "multiYn")) ? "Y" : "N";
        String displayCur = str(body, "displayCurrency");
        if ("N".equals(multiYn)) {
            displayCur = chargeCur;
        }
        if (displayCur == null || displayCur.isBlank()) {
            throw new IllegalArgumentException("표시통화가 없습니다.");
        }
        Object vatRaw = body.get("vatRatePct");
        if (vatRaw == null || vatRaw.toString().isBlank()) {
            throw new IllegalArgumentException("부가세율이 없습니다.");
        }
        Object rollingPctRaw = body.get("rollingPct");
        if (rollingPctRaw == null || rollingPctRaw.toString().isBlank()) {
            throw new IllegalArgumentException("롤링(%)이 없습니다.");
        }
        Object rollingDaysRaw = body.get("rollingDays");
        if (rollingDaysRaw == null || rollingDaysRaw.toString().isBlank()) {
            throw new IllegalArgumentException("보류일수가 없습니다.");
        }
        String schedReq = str(body, "settleScheduleType");
        if (schedReq == null || schedReq.isBlank()) {
            throw new IllegalArgumentException("정산주기 유형이 없습니다.");
        }
        Object lagReq = body.get("settleLagN");
        if (lagReq == null || lagReq.toString().isBlank()) {
            throw new IllegalArgumentException("정산주기 N이 없습니다.");
        }
        if (str(body, "multiYn") == null || str(body, "multiYn").isBlank()) {
            throw new IllegalArgumentException("멀티 여부가 없습니다.");
        }
        if (costNm == null || costNm.isBlank()) {
            costNm = vendorNm.trim();
        }
        String pgCd = pgCdRaw.trim().toUpperCase(Locale.ROOT);
        Long id = parseOptionalLong(body.get("id"));
        PgAgencyCostPolicy p;
        if (id != null) {
            p = policyRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("원가 정책을 찾을 수 없습니다."));
            if (!pgCd.equalsIgnoreCase(p.getPgCd())) {
                Optional<PgAgencyCostPolicy> other = policyRepository.findByPgCd(pgCd);
                if (other.isPresent() && !other.get().getId().equals(p.getId())) {
                    throw new IllegalArgumentException("이미 등록된 결제코드입니다.");
                }
                p.setPgCd(pgCd);
            }
        } else {
            if (policyRepository.findByPgCd(pgCd).isPresent()) {
                throw new IllegalArgumentException("이미 등록된 결제코드입니다.");
            }
            p = new PgAgencyCostPolicy();
            p.setPgCd(pgCd);
        }
        p.setCostNm(costNm.trim());
        p.setVendorNm(vendorNm.trim());
        body.put("multiYn", multiYn);
        body.put("currencyCode", chargeCur.trim().toUpperCase(Locale.ROOT));
        body.put("displayCurrency", displayCur.trim().toUpperCase(Locale.ROOT));
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
        appendHistory(p);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "저장되었습니다.");
        res.put("policy", toDetailMap(p));
        return res;
    }

    private void appendHistory(PgAgencyCostPolicy p) {
        try {
            PgAgencyCostPolicyHist h = new PgAgencyCostPolicyHist();
            h.setPolicyId(p.getId());
            h.setCostNm(p.getCostNm());
            h.setVendorNm(p.getVendorNm());
            h.setPgCd(p.getPgCd());
            h.setSnapshotJson(OM.writeValueAsString(toDetailMap(p)));
            h.setChangedBy(currentUser());
            histRepository.save(h);
        } catch (Exception ignored) {
            /* history best-effort */
        }
    }

    private static String currentUser() {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a != null && a.getName() != null && !a.getName().isBlank()) {
                return a.getName();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private Map<String, Object> emptyDefaults() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", "");
        m.put("costNm", "");
        m.put("vendorNm", "");
        m.put("pgCd", "");
        m.put("multiYn", "N");
        m.put("cardFeeMode", "UNIFIED");
        m.put("feeCardVisa", "0");
        m.put("feeCardMaster", "0");
        m.put("feeCardJcb", "0");
        m.put("feeCardAmex", "0");
        m.put("feeCardUnion", "0");
        m.put("feeCardDiners", "0");
        m.put("feeApmCrypto", "0");
        m.put("feeApmPaypay", "0");
        m.put("feeApmRakuten", "0");
        m.put("feeApmPromptpay", "0");
        m.put("feeApmLine", "0");
        m.put("feeApmKakao", "0");
        m.put("feeApmWechat", "0");
        m.put("perTxFee", "0.0");
        m.put("usageRate", "0.0");
        m.put("failFee", "0.0");
        m.put("cancelRate", "0.0");
        m.put("voidFeePerTx", "0");
        m.put("handVoidFeePerTx", "0");
        m.put("manualVoidFeePerTx", "0");
        m.put("refundRate", "0");
        m.put("handRefundFeePerTx", "0");
        m.put("payRate", "0");
        m.put("feeSettlementPerTx", "0");
        m.put("remittanceTransferFee", "0.0");
        m.put("usdtTransferFeeUsd", "0.0");
        m.put("feeUsdt", "0");
        m.put("feeFx", "0");
        m.put("rollingPct", "0");
        m.put("rollingDays", "0");
        m.put("currencyCode", "KRW");
        m.put("displayCurrency", "KRW");
        m.put("vatRatePct", "0");
        m.put("vatExcluded", "0");
        m.put("vatIncluded", "0");
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
        p.setHandVoidFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("handVoidFeePerTx")));
        p.setManualVoidFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("manualVoidFeePerTx")));
        p.setRefundRate(PercentDecimalHelper.parseAmountOneDecimal(body.get("refundRate")));
        p.setHandRefundFeePerTx(PercentDecimalHelper.parseAmountOneDecimal(body.get("handRefundFeePerTx")));
        p.setPayRate(PercentDecimalHelper.parsePercentOneDecimal(body.get("payRate")));
        p.setCardFeeMode(com.pg.util.PgAgencyCostCardFeeUtil.normalizeMode(str(body, "cardFeeMode")));
        p.setFeeCardVisa(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeCardVisa")));
        p.setFeeCardMaster(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeCardMaster")));
        p.setFeeCardJcb(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeCardJcb")));
        p.setFeeCardAmex(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeCardAmex")));
        p.setFeeCardUnion(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeCardUnion")));
        p.setFeeCardDiners(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeCardDiners")));
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
        if (cc == null || cc.isBlank()) {
            cc = str(body, "chargeCurrency");
        }
        p.setCurrencyCode(cc != null && !cc.isBlank() ? cc.trim().toUpperCase(Locale.ROOT) : "KRW");
        String multi = "Y".equalsIgnoreCase(str(body, "multiYn")) ? "Y" : "N";
        p.setMultiYn(multi);
        String dc = str(body, "displayCurrency");
        if ("N".equals(multi)) {
            p.setDisplayCurrency(p.getCurrencyCode());
        } else {
            p.setDisplayCurrency(dc != null && !dc.isBlank() ? dc.trim().toUpperCase(Locale.ROOT) : p.getCurrencyCode());
        }
        p.setVatRatePct(PercentDecimalHelper.parsePercentOneDecimal(body.get("vatRatePct")));
        p.setFeeApmCrypto(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeApmCrypto")));
        p.setFeeApmPaypay(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeApmPaypay")));
        p.setFeeApmRakuten(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeApmRakuten")));
        p.setFeeApmPromptpay(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeApmPromptpay")));
        p.setFeeApmLine(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeApmLine")));
        p.setFeeApmKakao(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeApmKakao")));
        p.setFeeApmWechat(PercentDecimalHelper.parsePercentOneDecimal(body.get("feeApmWechat")));
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
        m.put("costNm", displayCostNm(p));
        m.put("vendorNm", nz(p.getVendorNm()));
        m.put("pgCd", p.getPgCd());
        m.put("pgNm", nz(p.getVendorNm()));
        m.put("perTxFee", plainAmt(p.getPerTxFee()));
        m.put("usageRate", plainAmt(p.getUsageRate()));
        m.put("failFee", plainAmt(p.getFailFee()));
        m.put("cancelRate", plainAmt(p.getCancelRate()));
        m.put("voidFeePerTx", plainAmt(p.getVoidFeePerTx()));
        m.put("handVoidFeePerTx", plainAmt(p.getHandVoidFeePerTx()));
        m.put("manualVoidFeePerTx", plainAmt(p.getManualVoidFeePerTx()));
        m.put("refundRate", plainAmt(p.getRefundRate()));
        m.put("handRefundFeePerTx", plainAmt(p.getHandRefundFeePerTx()));
        m.put("payRate", p.getPayRate() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getPayRate()) : "0");
        m.put("cardFeeMode", com.pg.util.PgAgencyCostCardFeeUtil.normalizeMode(p.getCardFeeMode()));
        m.put("feeCardVisa", pctPlain(p.getFeeCardVisa()));
        m.put("feeCardMaster", pctPlain(p.getFeeCardMaster()));
        m.put("feeCardJcb", pctPlain(p.getFeeCardJcb()));
        m.put("feeCardAmex", pctPlain(p.getFeeCardAmex()));
        m.put("feeCardUnion", pctPlain(p.getFeeCardUnion()));
        m.put("feeCardDiners", pctPlain(p.getFeeCardDiners()));
        m.put("cardBrandScopeHint", com.pg.util.PgAgencyCostCardFeeUtil.brandScopeHint(p));
        m.put("feeSettlementPerTx", plainAmt(p.getFeeSettlementPerTx()));
        m.put("remittanceTransferFee", plainAmt(p.getRemittanceTransferFee()));
        m.put("usdtTransferFeeUsd", plainAmt(p.getUsdtTransferFeeUsd()));
        m.put("feeUsdt", p.getFeeUsdt() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeUsdt()) : "0");
        m.put("feeFx", p.getFeeFx() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getFeeFx()) : "0");
        m.put("rollingPct", p.getRollingPct() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getRollingPct()) : "0");
        m.put("rollingDays", p.getRollingDays() != null ? p.getRollingDays() : 0);
        m.put("currencyCode", p.getCurrencyCode() != null && !p.getCurrencyCode().isBlank() ? p.getCurrencyCode() : "KRW");
        m.put("chargeCurrency", m.get("currencyCode"));
        m.put("multiYn", p.getMultiYn() != null && "Y".equalsIgnoreCase(p.getMultiYn().trim()) ? "Y" : "N");
        m.put("displayCurrency", displayCur(p));
        m.put("vatRatePct", p.getVatRatePct() != null ? PercentDecimalHelper.toPlainOneDecimal(p.getVatRatePct()) : "0");
        m.put("feeApmCrypto", pctPlain(p.getFeeApmCrypto()));
        m.put("feeApmPaypay", pctPlain(p.getFeeApmPaypay()));
        m.put("feeApmRakuten", pctPlain(p.getFeeApmRakuten()));
        m.put("feeApmPromptpay", pctPlain(p.getFeeApmPromptpay()));
        m.put("feeApmLine", pctPlain(p.getFeeApmLine()));
        m.put("feeApmKakao", pctPlain(p.getFeeApmKakao()));
        m.put("feeApmWechat", pctPlain(p.getFeeApmWechat()));
        putVatAmountCols(m, p);
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
        m.put("settleScheduleLabel", settleScheduleLabel(p));
        m.put("useYn", p.getUseYn() != null ? p.getUseYn() : "Y");
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> toSummaryMap(PgAgencyCostPolicy p, Map<Long, String> chargebackNames) {
        Map<String, Object> m = toDetailMap(p, chargebackNames);
        long linked = p.getId() != null ? pgAgencyRepository.countByCostPolicyId(p.getId()) : 0L;
        m.put("linkedAgencyCnt", linked);
        return m;
    }

    /**
     * 근원원가 삭제. 결제대행사 설정에 1건이라도 연결돼 있으면(사용·미사용·운영유무 무관) 거부.
     */
    @Transactional
    public Map<String, Object> delete(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("삭제할 원가 정책이 없습니다.");
        }
        PgAgencyCostPolicy p = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("원가 정책을 찾을 수 없습니다."));
        long linked = pgAgencyRepository.countByCostPolicyId(id);
        if (linked > 0) {
            throw new IllegalArgumentException(
                    "연결된 결제대행사 설정이 " + linked + "건 있어 삭제할 수 없습니다. (미사용·비활성 포함)");
        }
        histRepository.deleteByPolicyId(id);
        policyRepository.delete(p);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("deletedId", id);
        out.put("message", "삭제되었습니다.");
        return out;
    }

    /** VAT제외=건당수수료(저장값), VAT포함=건당×(1+부가세율/100) */
    private static void putVatAmountCols(Map<String, Object> m, PgAgencyCostPolicy p) {
        BigDecimal ex = p.getPerTxFee() != null ? p.getPerTxFee() : BigDecimal.ZERO;
        BigDecimal rate = p.getVatRatePct() != null ? p.getVatRatePct() : BigDecimal.ZERO;
        BigDecimal inc = ex;
        if (rate.signum() > 0 && ex.signum() > 0) {
            inc = ex.multiply(BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)))
                    .setScale(Math.max(1, ex.scale()), RoundingMode.HALF_UP);
        }
        m.put("vatExcluded", plainAmt(ex));
        m.put("vatIncluded", plainAmt(inc));
    }

    private static String displayCostNm(PgAgencyCostPolicy p) {
        if (p.getCostNm() != null && !p.getCostNm().isBlank()) {
            return p.getCostNm().trim();
        }
        return p.getPgCd() != null ? p.getPgCd() : "";
    }

    private static String displayCur(PgAgencyCostPolicy p) {
        if (p.getDisplayCurrency() != null && !p.getDisplayCurrency().isBlank()) {
            return p.getDisplayCurrency().trim().toUpperCase(Locale.ROOT);
        }
        return p.getCurrencyCode() != null && !p.getCurrencyCode().isBlank() ? p.getCurrencyCode() : "KRW";
    }

    private static String costOptionLabel(PgAgencyCostPolicy p) {
        return displayCostNm(p) + " · " + nz(p.getVendorNm()) + " · " + p.getPgCd()
                + " · " + displayCur(p) + "/" + (p.getCurrencyCode() != null ? p.getCurrencyCode() : "");
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

    private static String pctPlain(BigDecimal b) {
        return b != null ? PercentDecimalHelper.toPlainOneDecimal(b) : "0";
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

    private static String nz(String s) {
        return s != null ? s : "";
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
