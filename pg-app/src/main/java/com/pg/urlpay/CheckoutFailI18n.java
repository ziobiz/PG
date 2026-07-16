package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 공개 결제·가맹 API 실패 응답용 5개국어 메시지 팩.
 * 사용자 노출 문구에는 운영 PG명(ILK 등)을 넣지 않는다.
 */
public final class CheckoutFailI18n {

    private CheckoutFailI18n() {
    }

    public static Map<String, String> pack(String kor, String eng, String jpn, String chn, String tha) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", kor);
        m.put("ENG", eng);
        m.put("JPN", jpn);
        m.put("CHN", chn);
        m.put("THA", tha);
        return m;
    }

    public static Map<String, Object> fail(String errorCode, String kor, String eng, String jpn, String chn, String tha) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", errorCode);
        out.put("message", kor);
        out.put("messageKey", errorCode);
        out.put("messages", pack(kor, eng, jpn, chn, tha));
        return out;
    }

    public static Map<String, Object> merchantNotFound() {
        return fail("NOT_FOUND",
                "가맹점을 찾을 수 없습니다.",
                "Merchant not found.",
                "加盟店が見つかりません。",
                "找不到商户。",
                "ไม่พบร้านค้า");
    }

    public static Map<String, Object> invalidOrderNo() {
        return fail("INVALID_ORDER_NO",
                "orderNo가 필요합니다.",
                "orderNo is required.",
                "orderNo が必要です。",
                "需要 orderNo。",
                "ต้องระบุ orderNo");
    }

    public static Map<String, Object> invalidAmount() {
        return fail("INVALID_AMOUNT",
                "유효한 amount가 필요합니다.",
                "A valid amount is required.",
                "有効な amount が必要です。",
                "需要有效的 amount。",
                "ต้องระบุจำนวนเงินที่ถูกต้อง");
    }

    public static Map<String, Object> cardRequired() {
        return fail("CARD_REQUIRED",
                "카드번호·유효기간이 필요합니다.",
                "Card number and expiry are required.",
                "カード番号・有効期限が必要です。",
                "需要卡号和有效期。",
                "ต้องระบุหมายเลขบัตรและวันหมดอายุ");
    }

    public static Map<String, Object> sessionError() {
        return fail("SESSION_ERROR",
                "세션 토큰 생성에 실패했습니다.",
                "Failed to create session token.",
                "セッショントークンの作成に失敗しました。",
                "无法创建会话令牌。",
                "สร้างเซสชันโทเค็นไม่สำเร็จ");
    }

    public static Map<String, Object> urlPayPgMissing() {
        return fail("URL_PAYMENT_PG_MISSING",
                "URL 결제(운영) 설정이 없습니다.",
                "URL payment is not configured for this merchant.",
                "URL決済（運用）設定がありません。",
                "未配置 URL 支付（运营）。",
                "ยังไม่ได้ตั้งค่าการชำระ URL (ใช้งานจริง)");
    }

    public static Map<String, Object> subscriptionPgMissing() {
        return fail("SUBSCRIPTION_PG_MISSING",
                "구독(운영) 결제 설정이 없습니다.",
                "Subscription payment is not configured for this merchant.",
                "サブスク（運用）決済設定がありません。",
                "未配置订阅（运营）支付。",
                "ยังไม่ได้ตั้งค่าการสมัครสมาชิก (ใช้งานจริง)");
    }

    public static Map<String, Object> pgConfigMissing() {
        return fail("PG_CONFIG_MISSING",
                "결제 설정을 찾을 수 없습니다.",
                "Payment configuration was not found.",
                "決済設定が見つかりません。",
                "未找到支付配置。",
                "ไม่พบการตั้งค่าการชำระเงิน");
    }

    public static Map<String, Object> pgCredentialsMissing() {
        return fail("PG_CREDENTIALS_MISSING",
                "결제 자격증명이 설정되지 않았습니다.",
                "Payment credentials are not configured.",
                "決済認証情報が設定されていません。",
                "未配置支付凭证。",
                "ยังไม่ได้ตั้งค่าข้อมูลรับรองการชำระเงิน");
    }

    public static Map<String, Object> pgHttpFailed() {
        return fail("PG_HTTP_FAILED",
                "결제 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                "Payment request failed. Please try again later.",
                "決済リクエストに失敗しました。しばらくして再度お試しください。",
                "支付请求失败，请稍后重试。",
                "คำขอชำระเงินล้มเหลว โปรดลองอีกครั้งในภายหลัง");
    }

    public static Map<String, Object> paymentFailed() {
        return fail("PAYMENT_FAILED",
                "결제에 실패했습니다.",
                "Payment failed.",
                "決済に失敗しました。",
                "支付失败。",
                "การชำระเงินล้มเหลว");
    }
}
