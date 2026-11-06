package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Map;

/** QR 결제창(/checkout-qr · PayPay) 시스템 기본 문구. */
public final class QrPaymentCopyDefaults {

    /** 로고 아래 브랜드 제목 */
    public static final String BRAND_TITLE_KO = "PayPay QR";

    /** QR 스캔 안내(회색 바) */
    public static final String HEADER_KO =
            "PayPay 앱으로 QR을 스캔해 결제해 주세요.";

    /** 하단 푸터(선택) */
    public static final String FOOTER_KO = "Provided by ICOPAY";

    private QrPaymentCopyDefaults() {}

    public static Map<String, String> brandTitleI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", BRAND_TITLE_KO);
        m.put("ENG", "PayPay QR");
        m.put("JPN", "PayPay QR");
        m.put("CHN", "PayPay QR");
        m.put("THA", "PayPay QR");
        return m;
    }

    public static Map<String, String> headerI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", HEADER_KO);
        m.put("ENG", "Scan this QR with the PayPay app to pay.");
        m.put("JPN", "PayPayアプリでQRを読み取り、お支払いください。");
        m.put("CHN", "请用 PayPay 应用扫描二维码完成支付。");
        m.put("THA", "สแกน QR ด้วยแอป PayPay เพื่อชำระ");
        return m;
    }

    public static Map<String, String> footerI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", FOOTER_KO);
        m.put("ENG", FOOTER_KO);
        m.put("JPN", FOOTER_KO);
        m.put("CHN", FOOTER_KO);
        m.put("THA", FOOTER_KO);
        return m;
    }
}
