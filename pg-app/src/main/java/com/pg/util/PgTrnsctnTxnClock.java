package com.pg.util;

import com.pg.entity.PgTrnsctn;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 결제내역 거래일·거래시각 기준 — {@link PgTrnsctn#getPaidAt()} 우선, 없으면 {@link PgTrnsctn#getCreatedAt()}.
 * <p>그리드 {@code trnDate}/{@code trnTime}·일자 검색·일별 집계는 동일 기준을 씁니다(적재일만으로 필터하지 않음).</p>
 */
public final class PgTrnsctnTxnClock {

    private PgTrnsctnTxnClock() {
    }

    public static LocalDateTime effectiveTxnDateTime(PgTrnsctn t) {
        if (t == null) {
            return null;
        }
        if (t.getPaidAt() != null) {
            return t.getPaidAt();
        }
        return t.getCreatedAt();
    }

    public static LocalDate effectiveTxnDate(PgTrnsctn t) {
        LocalDateTime dt = effectiveTxnDateTime(t);
        return dt != null ? dt.toLocalDate() : null;
    }
}
