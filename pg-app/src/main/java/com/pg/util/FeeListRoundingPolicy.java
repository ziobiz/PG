package com.pg.util;

import com.pg.entity.HqLedgerSysSettings;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 본사 전산설정(tb_hq_ledger_sys_settings) 기준 수수료내역 금액 소수 처리.
 * 기본: 소수 2자리, 셋째 자리부터 절상(CEILING).
 */
public record FeeListRoundingPolicy(int decimalPlaces, RoundingMode roundMode) {

    public static final int DEFAULT_DECIMAL_PLACES = 2;
    public static final RoundingMode DEFAULT_ROUND_MODE = RoundingMode.CEILING;

    public FeeListRoundingPolicy {
        if (decimalPlaces < 0) {
            decimalPlaces = 0;
        } else if (decimalPlaces > 8) {
            decimalPlaces = 8;
        }
        /* 소수 0이면 금액은 정수 스케일만 의미 있음 — 절상/반올림은 적용하지 않고 버림(DOWN)으로 통일 */
        if (decimalPlaces == 0) {
            roundMode = RoundingMode.DOWN;
        } else if (roundMode == null) {
            roundMode = DEFAULT_ROUND_MODE;
        }
    }

    public static FeeListRoundingPolicy defaults() {
        return new FeeListRoundingPolicy(DEFAULT_DECIMAL_PLACES, DEFAULT_ROUND_MODE);
    }

    public static FeeListRoundingPolicy fromSettings(HqLedgerSysSettings s) {
        if (s == null) {
            return defaults();
        }
        int scale = DEFAULT_DECIMAL_PLACES;
        if (s.getFeeListDecimalPlaces() != null) {
            scale = Math.min(8, Math.max(0, s.getFeeListDecimalPlaces()));
        }
        return new FeeListRoundingPolicy(scale, parseRoundMode(s.getFeeListRoundMode()));
    }

    public static RoundingMode parseRoundMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_ROUND_MODE;
        }
        String u = raw.trim().toUpperCase();
        return switch (u) {
            case "HALF_UP", "HALFUP", "ROUND" -> RoundingMode.HALF_UP;
            case "DOWN", "FLOOR", "TRUNC", "그대로" -> RoundingMode.DOWN;
            case "CEILING", "UP", "절상" -> RoundingMode.CEILING;
            default -> DEFAULT_ROUND_MODE;
        };
    }

    public static BigDecimal round(BigDecimal v, FeeListRoundingPolicy p) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        int sc = p != null ? p.decimalPlaces() : DEFAULT_DECIMAL_PLACES;
        RoundingMode m = p != null ? p.roundMode() : DEFAULT_ROUND_MODE;
        return v.setScale(Math.max(0, sc), m);
    }

    public static double roundToDouble(double v, FeeListRoundingPolicy p) {
        return round(BigDecimal.valueOf(v), p).doubleValue();
    }
}
