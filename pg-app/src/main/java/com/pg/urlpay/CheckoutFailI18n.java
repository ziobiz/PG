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

    /** prepare·sale: buyer 객체 자체 누락 */
    public static Map<String, Object> buyerObjectRequired() {
        return fail("BUYER_REQUIRED",
                "buyer 객체(email·phone·countryIso2)가 필요합니다. 누락 시 prepare가 실패합니다.",
                "The buyer object (email, phone, countryIso2) is required. Prepare fails if it is omitted.",
                "buyer オブジェクト（email・phone・countryIso2）が必要です。欠けると prepare は失敗します。",
                "需要 buyer 对象（email、phone、countryIso2）。缺省则 prepare 失败。",
                "ต้องมีออบเจ็กต์ buyer (email, phone, countryIso2) หากขาด prepare จะล้มเหลว");
    }

    public static Map<String, Object> buyerJsonInvalid() {
        return fail("BUYER_JSON_INVALID",
                "buyer JSON 형식이 올바르지 않습니다.",
                "buyer JSON is not valid.",
                "buyer の JSON 形式が正しくありません。",
                "buyer JSON 格式无效。",
                "รูปแบบ JSON ของ buyer ไม่ถูกต้อง");
    }

    public static Map<String, Object> buyerEmailRequired() {
        return fail("BUYER_EMAIL_REQUIRED",
                "이메일이 필수입니다. 결제창 또는 prepare의 buyer.email 을 입력하세요.",
                "Email is required. Enter it on the payment page or as buyer.email on prepare.",
                "メールは必須です。決済画面または prepare の buyer.email を入力してください。",
                "邮箱为必填。请在支付页或 prepare 的 buyer.email 中填写。",
                "ต้องระบุอีเมล กรอกในหน้าชำระหรือ buyer.email ใน prepare");
    }

    public static Map<String, Object> buyerPhoneRequired() {
        return fail("BUYER_PHONE_REQUIRED",
                "전화번호(로컬 번호)가 필수입니다. 국가번호 + 는 빼고 입력하세요.",
                "Phone (local number) is required. Strip the +country prefix.",
                "電話番号（国内番号）は必須です。国番号 + は除いてください。",
                "电话（本地号）为必填。请去掉 +国家码。",
                "ต้องระบุโทรศัพท์ (เลขในประเทศ) ตัดรหัสประเทศ +");
    }

    public static Map<String, Object> buyerCountryRequired() {
        return fail("BUYER_COUNTRY_REQUIRED",
                "국가코드(ISO2, 대문자 2자)가 필수입니다. 예: KR, US, TH, JP.",
                "Country code (ISO2, two uppercase letters) is required. e.g. KR, US, TH, JP.",
                "国コード（ISO2、大文字2文字）は必須です。例: KR, US, TH, JP。",
                "国家代码（ISO2，两位大写）为必填。例：KR、US、TH、JP。",
                "ต้องระบุรหัสประเทศ (ISO2 ตัวพิมพ์ใหญ่ 2 ตัว) เช่น KR US TH JP");
    }

    /**
     * 결제창: 도시·우편번호는 ICOPAY 필수가 아니므로 안내하지 않는다.
     * 연락처 누락과 구분 — 초기화 실패 시 {@link #checkoutStartFailed()}.
     */
    public static Map<String, Object> checkoutAttributeRequired() {
        return checkoutStartFailed();
    }

    /** 결제 초기화 실패(구매자에게 주소 입력을 요구하지 않음) */
    public static Map<String, Object> checkoutStartFailed() {
        return cardPathFailed();
    }

    /** 결제창에 이메일·전화·국가가 비어 있을 때(prepare 가 아님) */
    public static Map<String, Object> checkoutContactRequired() {
        return fail("BUYER_REQUIRED",
                "이메일·전화번호·국가코드를 결제창에 입력해 주세요.",
                "Please enter email, phone, and country code on the payment page.",
                "決済画面でメール・電話番号・国コードを入力してください。",
                "请在支付页填写邮箱、电话和国家代码。",
                "กรอกอีเมล โทรศัพท์ และรหัสประเทศในหน้าชำระเงิน");
    }

    /** 카드 승인 진행 URL을 못 받았을 때 — 구매자 문구에 운영 PG명 없음 */
    public static Map<String, Object> cardPathFailed() {
        return fail("CHECKOUT_CARD_PATH_FAILED",
                "결제를 진행할 수 없습니다. 잠시 후 다시 시도해 주세요.",
                "Payment could not continue. Please try again shortly.",
                "決済を続けられません。しばらくしてから再度お試しください。",
                "无法继续支付。请稍后再试。",
                "ดำเนินการชำระต่อไม่ได้ กรุณาลองใหม่ในอีกสักครู่");
    }

    /** 가맹 prepare API 전용 */
    public static Map<String, Object> buyerRequiredGeneric() {
        return fail("BUYER_REQUIRED",
                "구매자 이메일·전화·국가코드가 필수입니다. prepare의 buyer.email / buyer.phone / buyer.countryIso2 를 확인하세요.",
                "Buyer email, phone, and country code are required. Check buyer.email, buyer.phone, and buyer.countryIso2 on prepare.",
                "購入者のメール・電話・国コードは必須です。prepare の buyer.email / buyer.phone / buyer.countryIso2 を確認してください。",
                "买家邮箱、电话和国家代码为必填。请核对 prepare 中的 buyer.email / buyer.phone / buyer.countryIso2。",
                "อีเมล โทรศัพท์ และรหัสประเทศผู้ซื้อจำเป็น ตรวจ buyer.email / buyer.phone / buyer.countryIso2 ใน prepare");
    }
}
