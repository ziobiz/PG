package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 간편결제 QR URL(/pay-simple) 결제 안내 모드.
 * REDIRECT(RE) 확인창({@link RedirectCheckoutCopyModeUtil})과 분리 — 간편결제 ON 업체에만 적용.
 */
public final class SimplePayCheckoutCopyModeUtil {

    public static final String FOLLOW_HQ = RedirectCheckoutCopyModeUtil.FOLLOW_HQ;
    public static final String ACTIVE = RedirectCheckoutCopyModeUtil.ACTIVE;
    public static final String CUSTOM = RedirectCheckoutCopyModeUtil.CUSTOM;
    public static final String DISABLED = RedirectCheckoutCopyModeUtil.DISABLED;

    public static final String DEFAULT_LEAD_KO =
            "주문·구매자 정보를 확인한 뒤 결제하세요. 카드는 다음 결제창에서 입력합니다.";
    public static final String DEFAULT_HINT_KO =
            "휴대폰 카메라로 스캔 후 결제 진행을 합니다.";
    public static final String DEFAULT_AUTO_KO =
            "결제후 자동 완료 처리 됩니다.";

    private SimplePayCheckoutCopyModeUtil() {
    }

    public static String normalizeHq(String raw) {
        return RedirectCheckoutCopyModeUtil.normalizeHq(raw);
    }

    public static String normalizeMerchantStored(String raw) {
        return RedirectCheckoutCopyModeUtil.normalizeMerchantStored(raw);
    }

    public static String resolveEffective(String merchantStored, String hqDefault) {
        return RedirectCheckoutCopyModeUtil.resolveEffective(merchantStored, hqDefault);
    }

    public static boolean isCustom(String mode) {
        return RedirectCheckoutCopyModeUtil.isCustom(mode);
    }

    public static boolean isDisabled(String mode) {
        return RedirectCheckoutCopyModeUtil.isDisabled(mode);
    }

    public static Map<String, String> builtInLeadI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", DEFAULT_LEAD_KO);
        m.put("ENG", "Confirm order and shopper details, then pay. Enter the card on the next payment screen.");
        m.put("JPN", "注文・購入者情報を確認してからお支払いください。カードは次の画面で入力します。");
        m.put("CHN", "请确认订单与购买人信息后支付。卡信息在下一支付页输入。");
        m.put("THA", "ตรวจคำสั่งซื้อและข้อมูลผู้ซื้อแล้วชำระ ใส่บัตรในหน้าถัดไป");
        return m;
    }

    public static Map<String, String> builtInHintI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", DEFAULT_HINT_KO);
        m.put("ENG", "Scan with your phone camera to continue payment.");
        m.put("JPN", "スマートフォンのカメラでスキャンして決済を進めてください。");
        m.put("CHN", "请用手机相机扫描后继续支付。");
        m.put("THA", "สแกนด้วยกล้องมือถือแล้วชำระต่อ");
        return m;
    }

    public static Map<String, String> builtInAutoI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", DEFAULT_AUTO_KO);
        m.put("ENG", "Payment will complete automatically after pay.");
        m.put("JPN", "支払い後、自動で完了処理されます。");
        m.put("CHN", "支付后将自动完成处理。");
        m.put("THA", "หลังชำระระบบจะปิดงานอัตโนมัติ");
        return m;
    }
}
