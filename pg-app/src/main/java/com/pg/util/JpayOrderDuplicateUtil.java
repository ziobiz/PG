package com.pg.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * JPAY {@code pay_orderid} 중복·재시도 가드 — 메시지 판별 및 API/결제창용 오류 페이로드.
 */
public final class JpayOrderDuplicateUtil {

    public static final String CODE_ORDER_DUP = "ORDER_DUP";
    public static final String CODE_ORDER_PENDING = "ORDER_PENDING";
    public static final String CODE_ORDER_ALREADY_ATTEMPTED = "ORDER_ALREADY_ATTEMPTED";
    public static final String MESSAGE_KEY_ORDER_DUP = "ICOPAY_ORDER_DUP";
    public static final String MESSAGE_KEY_ORDER_PENDING = "ICOPAY_ORDER_PENDING";
    public static final String MESSAGE_KEY_ORDER_ALREADY_ATTEMPTED = "ICOPAY_ORDER_ALREADY_ATTEMPTED";

    private JpayOrderDuplicateUtil() {
    }

    public static boolean isDuplicateOrderMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String r = msg.trim().toLowerCase(Locale.ROOT);
        if (r.contains("duplicate") && r.contains("order")) {
            return true;
        }
        if (r.contains("重复") && r.contains("订单")) {
            return true;
        }
        if (r.contains("중복") && r.contains("주문")) {
            return true;
        }
        if (r.contains("重複") && (r.contains("注文") || r.contains("オーダー"))) {
            return true;
        }
        if (r.contains("duplicate order")) {
            return true;
        }
        return r.contains("orderid") && r.contains("exist");
    }

    public static Map<String, String> i18nMessages(String messageKey) {
        return switch (messageKey) {
            case MESSAGE_KEY_ORDER_DUP -> orderDupMessages();
            case MESSAGE_KEY_ORDER_PENDING -> orderPendingMessages();
            case MESSAGE_KEY_ORDER_ALREADY_ATTEMPTED -> orderAlreadyAttemptedMessages();
            default -> Map.of();
        };
    }

    public static Map<String, Object> orderDupFailPayload(String orderNo) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", CODE_ORDER_DUP);
        out.put("messageKey", MESSAGE_KEY_ORDER_DUP);
        out.put("messages", orderDupMessages());
        out.put("message", orderDupMessages().get("ENG"));
        out.put("requiresNewPrepare", true);
        out.put("orderNo", orderNo != null ? orderNo.trim() : "");
        return out;
    }

    public static Map<String, Object> orderPendingFailPayload(String orderNo) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", CODE_ORDER_PENDING);
        out.put("messageKey", MESSAGE_KEY_ORDER_PENDING);
        out.put("messages", orderPendingMessages());
        out.put("message", orderPendingMessages().get("ENG"));
        out.put("requiresNewPrepare", false);
        out.put("orderNo", orderNo != null ? orderNo.trim() : "");
        return out;
    }

    public static Map<String, Object> orderAlreadyAttemptedFailPayload(String orderNo) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", CODE_ORDER_ALREADY_ATTEMPTED);
        out.put("messageKey", MESSAGE_KEY_ORDER_ALREADY_ATTEMPTED);
        out.put("messages", orderAlreadyAttemptedMessages());
        out.put("message", orderAlreadyAttemptedMessages().get("ENG"));
        out.put("requiresNewPrepare", true);
        out.put("orderNo", orderNo != null ? orderNo.trim() : "");
        return out;
    }

    private static Map<String, String> orderDupMessages() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", "이 주문번호는 이미 결제가 시도되었습니다. 쇼핑몰에서 새 orderNo로 prepare를 다시 호출한 뒤 결제해 주세요.");
        m.put("ENG", "This order number was already used for a payment attempt. Call prepare again from your store with a new orderNo.");
        m.put("JPN", "この注文番号はすでに決済が試行されています。ストア側で新しい orderNo で prepare を再度呼び出してからお支払いください。");
        m.put("CHN", "该订单号已用于支付尝试。请从商城使用新的 orderNo 重新调用 prepare 后再支付。");
        m.put("THA", "หมายเลขคำสั่งซื้อนี้ถูกใช้ชำระเงินแล้ว โปรดเรียก prepare ใหม่ด้วย orderNo ใหม่จากร้านค้า");
        return m;
    }

    private static Map<String, String> orderPendingMessages() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", "이 주문번호로 결제가 진행 중입니다. 3DS 인증을 완료하거나 status API로 결과를 확인해 주세요. 같은 orderNo로 다시 제출하지 마세요.");
        m.put("ENG", "A payment is already in progress for this orderNo. Complete 3DS or check status API. Do not submit again with the same orderNo.");
        m.put("JPN", "この注文番号で決済が進行中です。3DS認証を完了するか status API で結果を確認してください。同じ orderNo で再送信しないでください。");
        m.put("CHN", "该订单号支付进行中。请完成3DS或通过 status API 查询结果，勿用相同 orderNo 重复提交。");
        m.put("THA", "คำสั่งซื้อนี้กำลังชำระอยู่ ให้ทำ 3DS ให้เสร็จหรือตรวจ status API อย่าส่งซ้ำด้วย orderNo เดิม");
        return m;
    }

    private static Map<String, String> orderAlreadyAttemptedMessages() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", "이 orderNo는 이미 결제 시도 이력이 있습니다. 재시도 시 반드시 새 orderNo를 발급한 뒤 prepare를 호출하세요.");
        m.put("ENG", "This orderNo already has a payment attempt. Issue a new orderNo and call prepare before retrying.");
        m.put("JPN", "この orderNo には決済試行履歴があります。再試行する前に新しい orderNo を発行して prepare を呼び出してください。");
        m.put("CHN", "该 orderNo 已有支付尝试记录。重试前请生成新 orderNo 并调用 prepare。");
        m.put("THA", "orderNo นี้มีประวัติชำระแล้ว ให้ออก orderNo ใหม่และเรียก prepare ก่อนลองใหม่");
        return m;
    }
}
