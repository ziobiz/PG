package com.pg.util;

/**
 * ElementPay INLINE 폴링 — getStatus 코드와 로컬 거래상태 해석.
 * <p>
 * 샌드박스 kCards 는 getStatus {@code 204}(rejected by bank) 를 먼저 주고,
 * 이후 Callback {@code pay} 또는 getStatus {@code 203}/{@code 205} 가 승인을 통보할 수 있다.
 * 폴링 중 {@code 204} 를 로컬 실패로 확정하면 결제창이 먼저 실패를 고정하고,
 * 이후 pay 웹훅만 관리자 내역을 성공으로 뒤집는다.
 */
public final class ElementPayInlineStatusUtil {

    private ElementPayInlineStatusUtil() {
    }

    public enum Kind {
        PAID, FAILED, PENDING, DISPUTED, REFUNDED
    }

    public record Mapped(
            Kind kind,
            boolean persistPaid,
            boolean persistFail,
            boolean persistRefund,
            boolean provisionalReject,
            String defaultMessageKey
    ) {
        public String paymentStatus() {
            return kind.name();
        }

        public boolean paid() {
            return kind == Kind.PAID;
        }
    }

    public static boolean isLocalPaid(String st) {
        return "10".equals(trim(st));
    }

    /** 취소·무효 등 — 폴링으로 회복하지 않는 실패. */
    public static boolean isLocalHardFail(String st) {
        String s = trim(st);
        return "20".equals(s) || "21".equals(s);
    }

    /**
     * getStatus 204 선제 반영 등. {@code pay}/203/205 로 승인 회복 가능.
     * 폴링 중에는 최종 실패로 단축하지 않는다.
     */
    public static boolean isLocalProvisionalFail(String st) {
        return "99".equals(trim(st));
    }

    public static boolean isLocalRefundOrChargeback(String st) {
        String s = trim(st);
        return "42".equals(s) || "31".equals(s) || "30".equals(s);
    }

    /** getStatus 승인 반영 시 건너뛸 로컬 상태. {@code 99} 는 회복 대상이라 건너뛰지 않음. */
    public static boolean skipSyncWhenPaid(String st) {
        return isLocalPaid(st) || isLocalHardFail(st) || isLocalRefundOrChargeback(st);
    }

    /** getStatus 거절 확정 시 건너뛸 로컬 상태. 이미 실패(99)·승인·환불은 유지. */
    public static boolean skipSyncWhenUnpaid(String st) {
        return isLocalPaid(st) || isLocalProvisionalFail(st)
                || isLocalHardFail(st) || isLocalRefundOrChargeback(st);
    }

    public static Mapped fromGetStatus(int st, boolean finalizeReject) {
        if (st == 203 || st == 205) {
            return new Mapped(Kind.PAID, true, false, false, false, null);
        }
        if (st == 208) {
            return new Mapped(Kind.DISPUTED, false, true, false, false, "ELEMENTPAY_DISPUTED");
        }
        if (st == 207) {
            return new Mapped(Kind.REFUNDED, false, false, true, false, "ELEMENTPAY_PAYMENT_REFUNDED");
        }
        if (st == 204 || st == 209) {
            if (finalizeReject) {
                return new Mapped(Kind.FAILED, false, true, false, false, "ELEMENTPAY_PAYMENT_REJECTED");
            }
            return new Mapped(Kind.PENDING, false, false, false, true, "ELEMENTPAY_PAYMENT_REJECTED");
        }
        return new Mapped(Kind.PENDING, false, false, false, false, null);
    }

    private static String trim(String st) {
        return st != null ? st.trim() : "";
    }
}
