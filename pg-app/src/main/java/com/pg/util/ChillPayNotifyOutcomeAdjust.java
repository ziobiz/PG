package com.pg.util;

import com.pg.entity.PgTrnsctn;

import java.util.Locale;
import java.util.Optional;

/**
 * 칠페이 CALLBACK 후속 노티에서 {@code PaymentStatus} 한 자리 {@code "2"} 는
 * 매퍼상 취소(20)로 해석되나, <strong>이미 승인(10)인 동일 거래</strong>에는 무효(21)로 덮어쓴다.
 * (노티매핑 경로·칠페이 직접 경로 공통)
 * <p>URL 결제(직접 적재 {@code origin=URL}·{@code serviceType}=URL_*)는 무효 후속이 {@code PaymentStatus=0} 이고
 * 취소 코드만 {@code Status}/Resp 에 있는 경우가 있어, 동일 조건에서 {@code "0"} 도 무효(21)로 승격한다.
 * 일반 노티 거래(NOTI)는 기존처럼 {@code "2"} 만 승격해 동작을 유지한다.</p>
 */
public final class ChillPayNotifyOutcomeAdjust {

    private ChillPayNotifyOutcomeAdjust() {
    }

    /** DirectCredit·URL 결제 페이지에서 적재한 행 또는 명시적 URL 서비스 유형 */
    public static boolean isUrlPayStoredTxn(PgTrnsctn t) {
        if (t == null) {
            return false;
        }
        String o = t.getOrigin();
        if (o != null && "URL".equalsIgnoreCase(o.trim())) {
            return true;
        }
        String st = t.getServiceType();
        if (st == null || st.isBlank()) {
            return false;
        }
        String u = st.trim().toUpperCase(Locale.ROOT);
        return u.startsWith("URL_");
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
        if (existingOpt.isEmpty()) {
            return computed;
        }
        PgTrnsctn ex = existingOpt.get();
        String prev = ex.getStatus();
        if (prev == null || prev.isBlank()) {
            return computed;
        }
        if (!"10".equals(prev.trim())) {
            return computed;
        }
        if ("2".equals(ps)) {
            return "21";
        }
        if ("0".equals(ps) && isUrlPayStoredTxn(ex)) {
            return "21";
        }
        return computed;
    }
}
