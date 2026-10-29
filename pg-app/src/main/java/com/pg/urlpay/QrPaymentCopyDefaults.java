package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Map;

/** QR 결제창(/checkout-qr) 시스템 기본 안내 문구. */
public final class QrPaymentCopyDefaults {

    public static final String HEADER_KO =
            "QR로 결제합니다. 앱에서 아래 QR을 스캔한 뒤 결제를 완료해 주세요.";

    private QrPaymentCopyDefaults() {}

    public static Map<String, String> headerI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", HEADER_KO);
        m.put("ENG", "Pay with QR. Scan the QR below in your app and complete payment.");
        m.put("JPN", "QRでお支払いください。アプリで下のQRをスキャンし、決済を完了してください。");
        m.put("CHN", "请使用二维码支付。在应用中扫描下方二维码并完成支付。");
        m.put("THA", "ชำระด้วย QR สแกน QR ด้านล่างในแอปแล้วชำระให้เสร็จ");
        return m;
    }
}
