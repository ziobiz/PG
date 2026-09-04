package com.pg.util;

import java.util.Locale;

/**
 * ElementPay Callback API 비동기 이벤트 분류.
 * check/pay 이외: {@code payment.*}, {@code refund.*}, {@code card_auth.*}, transfer/payout.
 */
public final class ElementPayCallbackEventUtil {

    public enum Kind {
        /** Cabinet Events {@code payment.paid}/{@code payment.charged} — 승인(10). */
        PAY_PAID,
        /** TTL 만료 등 — 미승인이면 실패(99). 이미 승인이면 유지. */
        PAY_REJECT,
        /** 기승인 취소(은행) — 강제환불(31). */
        PAY_REVERSED,
        /** 환불 완료 — 자동환불(42). */
        PAY_REFUNDED,
        /** 결제자 불일치 — 대기는 실패, 승인은 강제환불. */
        WRONG_PAYER,
        /** 환불 요청만 생성 — 승인 유지. */
        REFUND_CREATED,
        /** 환불 요청 취소 — 승인 유지. */
        REFUND_CANCELED,
        /** 거래 상태 변경 없음. 270만 응답. */
        ACK
    }

    public record Spec(Kind kind, String messageKey, boolean requireTxn) {
        public boolean changesTxn() {
            return kind != Kind.ACK;
        }
    }

    private ElementPayCallbackEventUtil() {
    }

    public static boolean isCheckOrPay(String method) {
        String m = norm(method);
        return "check".equals(m) || "pay".equals(m);
    }

    public static Spec classify(String method) {
        String m = norm(method);
        if (m.isEmpty() || isCheckOrPay(m)) {
            return new Spec(Kind.ACK, null, false);
        }
        return switch (m) {
            case "payment.paid", "payment.charged" -> new Spec(Kind.PAY_PAID, null, true);
            case "payment.rejected" -> new Spec(Kind.PAY_REJECT, "ELEMENTPAY_PAYMENT_REJECTED", true);
            case "payment.reversed" -> new Spec(Kind.PAY_REVERSED, "ELEMENTPAY_PAYMENT_REVERSED", true);
            case "payment.refunded" -> new Spec(Kind.PAY_REFUNDED, "ELEMENTPAY_PAYMENT_REFUNDED", true);
            case "payment.wrong_payer" -> new Spec(Kind.WRONG_PAYER, "ELEMENTPAY_WRONG_PAYER", true);
            case "refund.paid" -> new Spec(Kind.PAY_REFUNDED, "ELEMENTPAY_REFUND_PAID", true);
            case "refund.created" -> new Spec(Kind.REFUND_CREATED, "ELEMENTPAY_REFUND_CREATED", true);
            case "refund.canceled" -> new Spec(Kind.REFUND_CANCELED, "ELEMENTPAY_REFUND_CANCELED", true);
            default -> new Spec(Kind.ACK, null, false);
        };
    }

    public static String defaultMessage(Spec spec, String fallbackMethod) {
        if (spec != null && spec.messageKey() != null && !spec.messageKey().isBlank()) {
            return spec.messageKey();
        }
        return fallbackMethod != null ? fallbackMethod.trim() : "notification";
    }

    private static String norm(String method) {
        return method == null ? "" : method.trim().toLowerCase(Locale.ROOT);
    }
}
