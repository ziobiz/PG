package com.pg.util;

import com.pg.entity.PgTrnsctn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** 노티·재반영 시 승인 시각({@code paid_at}) 결정 — 재전송일이 거래일로 잡히지 않게 합니다. */
public final class NotifyTxnPaidAtUtil {

    private static final DateTimeFormatter PAY_DD_MM =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ROOT);

    private NotifyTxnPaidAtUtil() {
    }

    /**
     * 승인(10) 반영 시 paid_at.
     * 1) 노티 본문 파싱 시각 2) 기존 paid_at 유지 3) 최초 적재 created_at 4) 현재 시각
     */
    public static LocalDateTime resolvePaidAtForApproval(PgTrnsctn t, LocalDateTime parsedFromNotify, ZoneId wall) {
        if (parsedFromNotify != null) {
            return parsedFromNotify;
        }
        if (t != null && t.getPaidAt() != null) {
            return t.getPaidAt();
        }
        if (t != null && t.getCreatedAt() != null) {
            return t.getCreatedAt();
        }
        ZoneId z = wall != null ? wall : ZoneId.of("Asia/Bangkok");
        return LocalDateTime.now(z);
    }

    /** 재반영 모달 등에서 지정한 거래일로 paid_at 날짜만 덮어씁니다(시각은 기존 paid_at·created_at 유지). */
    public static void applyTrnDateOverride(PgTrnsctn t, LocalDate overrideDate, ZoneId wall) {
        if (t == null || overrideDate == null) {
            return;
        }
        LocalTime time = LocalTime.MIDNIGHT;
        if (t.getPaidAt() != null) {
            time = t.getPaidAt().toLocalTime();
        } else if (t.getCreatedAt() != null) {
            time = t.getCreatedAt().toLocalTime();
        }
        t.setPaidAt(LocalDateTime.of(overrideDate, time));
    }

    public static LocalDate parseTrnDateOverride(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDateTime parsePaymentDateString(String pd) {
        if (pd == null || pd.isBlank()) {
            return null;
        }
        String t = pd.trim();
        try {
            return LocalDateTime.parse(t, PAY_DD_MM);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            if (t.length() >= 10) {
                return LocalDateTime.parse(t.substring(0, 10) + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (DateTimeParseException ignored) {
        }
        try {
            if (t.matches("^\\d{14}$")) {
                return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT));
            }
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}
