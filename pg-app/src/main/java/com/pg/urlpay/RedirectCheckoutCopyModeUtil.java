package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * REDIRECT(RE) 결제 확인창 상단·하단 안내 문구 모드.
 * 가맹: FOLLOW_HQ|ACTIVE|CUSTOM|DISABLED · 본사: ACTIVE|CUSTOM|DISABLED
 */
public final class RedirectCheckoutCopyModeUtil {

    public static final String FOLLOW_HQ = "FOLLOW_HQ";
    public static final String ACTIVE = "ACTIVE";
    public static final String CUSTOM = "CUSTOM";
    public static final String DISABLED = "DISABLED";

    public static final String DEFAULT_LEAD_KO = "주문·구매자 정보를 확인한 뒤 결제 페이지로 이동합니다.";
    public static final String DEFAULT_HINT_KO = "다음 화면에서 카드정보를 입력 후 결제 완료 합니다.";

    private RedirectCheckoutCopyModeUtil() {
    }

    /** 본사 저장·실효 (FOLLOW_HQ → ACTIVE) */
    public static String normalizeHq(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case DISABLED -> DISABLED;
            case CUSTOM, "ACTIVE_CUSTOM", "DIRECT" -> CUSTOM;
            case FOLLOW_HQ, "HQ" -> ACTIVE;
            default -> ACTIVE;
        };
    }

    /** 가맹 DB 저장 */
    public static String normalizeMerchantStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (FOLLOW_HQ.equals(u) || "HQ".equals(u)) {
            return FOLLOW_HQ;
        }
        return normalizeHq(u);
    }

    public static String resolveEffective(String merchantStored, String hqDefault) {
        String stored = normalizeMerchantStored(merchantStored);
        if (!FOLLOW_HQ.equals(stored)) {
            return stored;
        }
        return normalizeHq(hqDefault);
    }

    public static boolean isCustom(String mode) {
        return CUSTOM.equals(normalizeHq(mode));
    }

    public static boolean isDisabled(String mode) {
        return DISABLED.equals(normalizeHq(mode));
    }

    public static Map<String, String> builtInLeadI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", DEFAULT_LEAD_KO);
        m.put("ENG", "Confirm order and shopper details, then continue to payment.");
        m.put("JPN", "注文・購入者情報を確認してからお支払い画面へ進みます。");
        m.put("CHN", "请确认订单与购买人信息后进入支付页。");
        m.put("THA", "ตรวจคำสั่งซื้อและข้อมูลผู้ซื้อ แล้วไปหน้าชำระเงิน");
        return m;
    }

    public static Map<String, String> builtInHintI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", DEFAULT_HINT_KO);
        m.put("ENG", "Enter your card details on the next screen to complete payment.");
        m.put("JPN", "次の画面でカード情報を入力して決済を完了してください。");
        m.put("CHN", "请在下一屏输入卡信息并完成支付。");
        m.put("THA", "ในหน้าถัดไปกรอกข้อมูลบัตรแล้วชำระเงินให้เสร็จ");
        return m;
    }
}
