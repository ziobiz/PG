package com.pg.util;

import com.pg.entity.SettlementSetting;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 가맹점 정산설정(tb_settlement_setting) 기준 수수료 부가세(VAT).
 */
public final class MerchantFeeVatUtil {

    private MerchantFeeVatUtil() {
    }

    public static boolean isFeeVatOn(SettlementSetting ss) {
        return ss != null && "Y".equalsIgnoreCase(trim(ss.getFeeVatApplyYn()));
    }

    public static BigDecimal effectiveFeeVatRatePct(SettlementSetting ss) {
        if (!isFeeVatOn(ss)) {
            return BigDecimal.ZERO;
        }
        BigDecimal r = ss.getFeeVatRatePct();
        if (r == null || r.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return r.min(new BigDecimal("100")).max(BigDecimal.ZERO);
    }

    /**
     * @param taxableFee 부가세 과표가 되는 수수료 합(음이면 0 처리)
     * @param scale        소수 자리(0이면 정수 반올림)
     */
    public static BigDecimal vatOnFeeAmount(BigDecimal taxableFee, SettlementSetting ss, int scale) {
        if (taxableFee == null || taxableFee.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = effectiveFeeVatRatePct(ss);
        if (rate.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        int sc = Math.max(0, scale);
        return taxableFee.multiply(rate).divide(BigDecimal.valueOf(100), sc, RoundingMode.HALF_UP);
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }
}
