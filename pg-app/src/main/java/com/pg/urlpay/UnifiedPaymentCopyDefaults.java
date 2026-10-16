package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 통합결제(/checkout-all) 시스템 기본 문구.
 * 상단·푸터메시지는 5개국어, 추가 푸터는 영어 고정.
 */
public final class UnifiedPaymentCopyDefaults {

    public static final String HEADER_KO =
            "카드 또는 암호화폐로 결제할 수 있습니다. 원하시는 결제 수단을 선택해 주세요.";
    public static final String NOTICE_KO =
            "주의: 선택한 수단으로 결제가 진행됩니다. 카드와 암호화폐는 별도 결제창에서 처리되며, 결제 완료 전까지 창을 닫지 마세요.";
    /** 추가 푸터 — 언어와 무관하게 영어 고정 */
    public static final String EXTRA_FOOTER_EN = "ICOPAY, a service operated by ONTHELINE.";

    private UnifiedPaymentCopyDefaults() {}

    public static Map<String, String> headerI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", HEADER_KO);
        m.put("ENG", "You can pay by card or crypto. Please choose a payment method.");
        m.put("JPN", "カードまたは暗号資産でお支払いできます。お支払い方法を選択してください。");
        m.put("CHN", "可使用银行卡或加密货币支付。请选择支付方式。");
        m.put("THA", "ชำระด้วยบัตรหรือคริปโตได้ กรุณาเลือกวิธีชำระเงิน");
        return m;
    }

    public static Map<String, String> noticeI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", NOTICE_KO);
        m.put("ENG", "Note: Payment continues with the method you select. Card and crypto open separate checkout pages. Do not close the window until payment is complete.");
        m.put("JPN", "注意: 選択した方法で決済が続きます。カードと暗号資産は別画面です。完了まで閉じないでください。");
        m.put("CHN", "注意：将按所选方式继续支付。银行卡与加密货币为不同页面。完成前请勿关闭窗口。");
        m.put("THA", "หมายเหตุ: จะชำระตามวิธีที่เลือก บัตรและคริปโตเปิดหน้าต่างคนละหน้า อย่าปิดจนกว่าจะชำระเสร็จ");
        return m;
    }
}
