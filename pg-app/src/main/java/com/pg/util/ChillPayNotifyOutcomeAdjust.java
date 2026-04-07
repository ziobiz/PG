package com.pg.util;

import com.pg.entity.PgTrnsctn;

import java.util.Optional;

/**
 * 칠페이 CALLBACK 후속 노티에서 {@code PaymentStatus} 한 자리 {@code "2"} 는
 * 매퍼상 취소(20)로 해석되나, <strong>이미 승인(10)인 동일 거래</strong>에는 무효(21)로 덮어쓴다.
 * (노티매핑 경로·칠페이 직접 경로 공통)
 */
public final class ChillPayNotifyOutcomeAdjust {

    private ChillPayNotifyOutcomeAdjust() {
    }

    /**
     * @param paymentStatusRaw {@link com.fasterxml.jackson.databind.JsonNode} 에서 읽은 PaymentStatus 계열 원문(보통 {@code "0"}·{@code "2"})
     */
    public static String reclassifyPaymentStatusTwoAfterPaid(Optional<PgTrnsctn> existingOpt,
                                                             String paymentStatusRaw,
                                                             String computed) {
        if (computed == null || !"20".equals(computed)) {
            return computed;
        }
        String ps = paymentStatusRaw != null ? paymentStatusRaw.trim() : "";
        if (!"2".equals(ps)) {
            return computed;
        }
        if (existingOpt.isEmpty()) {
            return computed;
        }
        String prev = existingOpt.get().getStatus();
        if (prev == null || prev.isBlank()) {
            return computed;
        }
        if ("10".equals(prev.trim())) {
            return "21";
        }
        return computed;
    }
}
