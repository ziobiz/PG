package com.pg.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 결제내역 검색일(당일·전일·당월 등) = 전산설정 <strong>운영 시간대</strong> 달력.
 * {@code paid_at} 은 표준 시간대 naive 이므로, 운영일 구간을 표준 벽시계 구간으로 변환한다.
 */
public final class PayListOperationalDayBounds {

    public record Bounds(LocalDateTime fromStdInclusive, LocalDateTime toStdInclusive) {
    }

    private PayListOperationalDayBounds() {
    }

    /**
     * @param fromDay 운영 달력 시작일(포함), null 이면 from 미제한
     * @param toDay   운영 달력 종료일(포함), null 이면 to 미제한
     * @param operational 운영 Zone (예: Asia/Tokyo)
     * @param standard    표준 Zone — paid_at 해석 (예: Asia/Bangkok)
     */
    public static Bounds toStandardWallInclusive(LocalDate fromDay, LocalDate toDay,
                                                 ZoneId operational, ZoneId standard) {
        ZoneId op = operational != null ? operational : ZoneId.of("Asia/Tokyo");
        ZoneId std = standard != null ? standard : ZoneId.of("Asia/Bangkok");
        LocalDateTime fromStd = null;
        LocalDateTime toStd = null;
        if (fromDay != null) {
            fromStd = fromDay.atStartOfDay(op).toInstant().atZone(std).toLocalDateTime();
        }
        if (toDay != null) {
            /* 종료일 다음날 운영 00:00 직전 = 종료일 포함 */
            toStd = toDay.plusDays(1).atStartOfDay(op).toInstant().atZone(std).toLocalDateTime()
                    .minusNanos(1);
            if (toStd.toLocalTime().equals(LocalTime.MAX)) {
                /* 나노 절삭 환경 대비 — 초 단위 MAX 유지 */
                toStd = toStd.withNano(999_000_000);
            }
        }
        return new Bounds(fromStd, toStd);
    }

    public static LocalDate todayOperational(ZoneId operational) {
        ZoneId op = operational != null ? operational : ZoneId.of("Asia/Tokyo");
        return LocalDate.now(op);
    }
}
