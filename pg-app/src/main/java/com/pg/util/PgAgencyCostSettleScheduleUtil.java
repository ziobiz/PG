package com.pg.util;

import com.pg.entity.PgAgencyCostPolicy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

/**
 * 대행수수료설정({@code tb_pg_agency_cost_policy})의 T/H/D 규칙으로 PG 계약 정산 도래 시각·정산유무를 판단한다.
 */
public final class PgAgencyCostSettleScheduleUtil {

    private PgAgencyCostSettleScheduleUtil() {
    }

    public static LocalDateTime computeDueAt(PgAgencyCostPolicy policy, LocalDateTime txnAt) {
        if (policy == null || txnAt == null || !policyActive(policy)) {
            return null;
        }
        String type = policy.getSettleScheduleType() != null
                ? policy.getSettleScheduleType().trim().toUpperCase(Locale.ROOT) : "T";
        int n = policy.getSettleLagN() != null ? Math.max(1, policy.getSettleLagN()) : 1;
        LocalTime batch = policy.getSettleBatchTime();
        return switch (type) {
            case "H" -> txnAt.plusHours(24L * n);
            case "D" -> {
                LocalDate d = txnAt.toLocalDate().plusDays(n);
                LocalTime t = batch != null ? batch : txnAt.toLocalTime();
                yield LocalDateTime.of(d, t);
            }
            case "T" -> {
                LocalDate d = addBusinessDays(txnAt.toLocalDate(), n);
                LocalTime t = batch != null ? batch : txnAt.toLocalTime();
                yield LocalDateTime.of(d, t);
            }
            default -> null;
        };
    }

    /** 정산 도래 여부: Y=도래·N=미도래, 정책·시각 없으면 빈 문자열 */
    public static String agencySettleYn(PgAgencyCostPolicy policy, LocalDateTime txnAt, LocalDateTime now) {
        LocalDateTime due = computeDueAt(policy, txnAt);
        if (due == null) {
            return "";
        }
        LocalDateTime ref = now != null ? now : LocalDateTime.now();
        return !ref.isBefore(due) ? "Y" : "N";
    }

    private static boolean policyActive(PgAgencyCostPolicy policy) {
        return policy.getUseYn() == null || "Y".equalsIgnoreCase(policy.getUseYn().trim());
    }

    /** 주말 제외 영업일 가산 (공휴일 미반영) */
    private static LocalDate addBusinessDays(LocalDate start, int businessDays) {
        if (businessDays <= 0) {
            return start;
        }
        LocalDate d = start;
        int added = 0;
        while (added < businessDays) {
            d = d.plusDays(1);
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }
}
