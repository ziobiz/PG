package com.pg.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

/**
 * JPAY 포털 Export·Trade Query로 {@link com.pg.entity.PgTrnsctn} 상태를 갱신할 때의 안전 규칙.
 * <p>승인(10)은 서명된 JPAY 노티·pay_index 동기 성공만 확정합니다.
 * Trade Query의 {@code returncode=00}은 API 호출 성공이지 결제 승인이 아닙니다.
 */
public final class JpayReconcileStatusPolicy {

    private JpayReconcileStatusPolicy() {
    }

    /** 포털·Trade Query 결과를 DB에 반영 가능한지 — 승인(10)은 항상 거부 */
    public static boolean mayApplyReconcileMapping(String mappedInternal) {
        if (mappedInternal == null || mappedInternal.isBlank()) {
            return false;
        }
        return !PgNotifyInternalStatusMapper.ST_PAID.equals(mappedInternal.trim());
    }

    /** JPAY Trade Query — trade_state 가 확정 승인인지 */
    public static boolean isConfirmedJpayPaidTradeState(String tradeState) {
        if (tradeState == null || tradeState.isBlank()) {
            return false;
        }
        String s = tradeState.trim().toUpperCase(Locale.ROOT);
        return "SUCCESS".equals(s) || "SUCCEEDED".equals(s) || "PAID".equals(s);
    }

    /**
     * 요청(08)·오승인(10) 건이 staleMinutes 경과했고 JPAY 조회가 확정 승인이 아닐 때 UNPAID 임시 취소(20).
     */
    public static boolean mayApplyStaleUnpaidProvisional(String oldStatus, String tradeState,
                                                        LocalDateTime createdAt, int staleMinutes,
                                                        ZoneId zone) {
        if (createdAt == null) {
            return false;
        }
        String old = oldStatus != null ? oldStatus.trim() : "";
        if (!PgNotifyInternalStatusMapper.ST_AUTH_PENDING.equals(old)
                && !PgNotifyInternalStatusMapper.ST_PAID.equals(old)) {
            return false;
        }
        ZoneId z = zone != null ? zone : ZoneId.of("Asia/Seoul");
        int minutes = Math.max(1, staleMinutes);
        LocalDateTime staleBefore = LocalDateTime.now(z).minusMinutes(minutes);
        if (createdAt.isAfter(staleBefore)) {
            return false;
        }
        return !isConfirmedJpayPaidTradeState(tradeState);
    }
}
