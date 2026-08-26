package com.pg.util;

import java.util.Locale;

/**
 * URL 결제(ElementPay) {@code /merchant/initRefund} 거절 — 운영자용 안내.
 * 환불은 승인 취서가 아니라 캐비닛 <em>가용잔액</em>에서 지급된다.
 */
public final class ElementPayRefundRejectMapper {

    /** 409 · Insufficient of available fund — 가맹 캐비닛 가용잔액 부족 */
    public static final String MSG_INSUFFICIENT_AVAILABLE_FUND =
            "결제망 가용잔액이 부족하여 환불을 거절했습니다. 승인 직후에는 해당 결제 금액이 홀드되어 가용잔액에 아직 안 잡힐 수 있습니다. 캐비닛 가용잔액을 확인하거나 홀드가 풀린 뒤(보통 정산 이후) 다시 시도해 주세요.";

    public static final String MSG_AMOUNT_EXCEEDS_PAYMENT =
            "환불 금액이 해당 결제의 환불 가능 금액을 초과합니다.";

    public static final String MSG_ALREADY_PAYOUT =
            "해당 결제는 이미 출금(페이아웃) 처리되어 환불할 수 없습니다.";

    public static final String MSG_PAYMENT_NOT_FOUND =
            "결제망에서 해당 결제를 찾지 못했습니다. payment_id를 확인해 주세요.";

    private ElementPayRefundRejectMapper() {
    }

    public static String operatorMessage(String code, String pgMessage) {
        String c = code == null ? "" : code.trim();
        String msg = pgMessage == null ? "" : pgMessage.trim();
        String low = msg.toLowerCase(Locale.ROOT);
        if (isInsufficientAvailableFund(c, low)) {
            return MSG_INSUFFICIENT_AVAILABLE_FUND;
        }
        if ("476".equals(c) || "4003".equals(c) || low.contains("exceeds the available payment")
                || low.contains("more than available for the payment")) {
            return MSG_AMOUNT_EXCEEDS_PAYMENT;
        }
        if ("473".equals(c) || low.contains("already marked to payout")) {
            return MSG_ALREADY_PAYOUT;
        }
        if ("475".equals(c) || low.contains("payment with the specified parameters is not found")) {
            return MSG_PAYMENT_NOT_FOUND;
        }
        StringBuilder sb = new StringBuilder("결제망 환불 거부");
        if (!c.isBlank()) {
            sb.append(" (").append(c).append(')');
        }
        if (!msg.isBlank()) {
            sb.append(": ").append(msg);
        }
        return sb.toString();
    }

    static boolean isInsufficientAvailableFund(String code, String messageLower) {
        if ("409".equals(code)) {
            return true;
        }
        if (messageLower == null || messageLower.isBlank()) {
            return false;
        }
        if (messageLower.contains("more than available for the payment")
                || messageLower.contains("exceeds the available payment")) {
            return false;
        }
        return messageLower.contains("insufficient of available fund")
                || messageLower.contains("insufficient available fund")
                || (messageLower.contains("insufficient") && messageLower.contains("available fund"));
    }
}
