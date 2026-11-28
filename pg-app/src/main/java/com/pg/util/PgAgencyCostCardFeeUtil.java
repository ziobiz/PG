package com.pg.util;

import com.pg.entity.PgAgencyCostPolicy;

import java.math.BigDecimal;

/**
 * 대행 근원원가 — 카드수수료(%) 통합/개별 해석.
 * 개별 모드에서 브랜드 수수료 0 이면 해당 브랜드 미사용(원가 0).
 */
public final class PgAgencyCostCardFeeUtil {

    private PgAgencyCostCardFeeUtil() {
    }

    public static boolean isPerBrand(PgAgencyCostPolicy pol) {
        if (pol == null || pol.getCardFeeMode() == null) {
            return false;
        }
        return "PER_BRAND".equalsIgnoreCase(pol.getCardFeeMode().trim());
    }

    /** 거래 카드번호(마스킹 가능) 기준 적용 카드수수료율. */
    public static BigDecimal resolvePayRate(PgAgencyCostPolicy pol, String cardPanDisplay) {
        if (pol == null) {
            return BigDecimal.ZERO;
        }
        if (!isPerBrand(pol)) {
            return nz(pol.getPayRate());
        }
        String digits = PayCardBrandDetector.normalizePan(cardPanDisplay);
        PayCardBrand brand = PayCardBrandDetector.detect(digits);
        return rateForBrand(pol, brand);
    }

    public static BigDecimal rateForBrand(PgAgencyCostPolicy pol, PayCardBrand brand) {
        if (pol == null) {
            return BigDecimal.ZERO;
        }
        if (!isPerBrand(pol)) {
            return nz(pol.getPayRate());
        }
        if (brand == null || brand == PayCardBrand.UNKNOWN) {
            return BigDecimal.ZERO;
        }
        return switch (brand) {
            case VISA -> nz(pol.getFeeCardVisa());
            case MASTERCARD -> nz(pol.getFeeCardMaster());
            case JCB -> nz(pol.getFeeCardJcb());
            case AMEX -> nz(pol.getFeeCardAmex());
            case UNIONPAY -> nz(pol.getFeeCardUnion());
            case DINERS -> nz(pol.getFeeCardDiners());
            default -> BigDecimal.ZERO;
        };
    }

    /** 개별 모드에서 0 초과 수수료가 있는 브랜드만 사용 가능으로 본다. */
    public static boolean brandEnabled(PgAgencyCostPolicy pol, PayCardBrand brand) {
        if (pol == null || brand == null || brand == PayCardBrand.UNKNOWN) {
            return false;
        }
        if (!isPerBrand(pol)) {
            return nz(pol.getPayRate()).signum() > 0;
        }
        return rateForBrand(pol, brand).signum() > 0;
    }

    public static String normalizeMode(String raw) {
        if (raw != null && "PER_BRAND".equalsIgnoreCase(raw.trim())) {
            return "PER_BRAND";
        }
        return "UNIFIED";
    }

    public static String brandScopeHint(PgAgencyCostPolicy pol) {
        if (pol == null || !isPerBrand(pol)) {
            return "ALL";
        }
        StringBuilder sb = new StringBuilder();
        appendIf(sb, brandEnabled(pol, PayCardBrand.VISA), "V");
        appendIf(sb, brandEnabled(pol, PayCardBrand.MASTERCARD), "M");
        appendIf(sb, brandEnabled(pol, PayCardBrand.JCB), "J");
        appendIf(sb, brandEnabled(pol, PayCardBrand.AMEX), "A");
        appendIf(sb, brandEnabled(pol, PayCardBrand.UNIONPAY), "U");
        appendIf(sb, brandEnabled(pol, PayCardBrand.DINERS), "D");
        return sb.length() == 0 ? "NONE" : sb.toString();
    }

    private static void appendIf(StringBuilder sb, boolean on, String code) {
        if (!on) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('+');
        }
        sb.append(code);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
