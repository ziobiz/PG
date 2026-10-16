package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 크립토 결제창(/crypto-pay) 상단 안내 — 시스템 기본 문구(5개국어).
 */
public final class CryptoPaymentCopyDefaults {

    public static final String HEADER_KO =
            "암호화폐로 결제합니다. 자산·네트워크·지갑을 선택한 뒤 안내 주소로 입금해 주세요.";

    private CryptoPaymentCopyDefaults() {}

    public static Map<String, String> headerI18n() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("KOR", HEADER_KO);
        m.put("ENG", "Pay with cryptocurrency. Choose asset, network and wallet, then deposit to the address shown.");
        m.put("JPN", "暗号資産でお支払いください。資産・ネットワーク・ウォレットを選び、表示の住所へ入金してください。");
        m.put("CHN", "请使用加密货币支付。选择资产、网络与钱包后，转账至所示地址。");
        m.put("THA", "ชำระด้วยคริปโต เลือกสินทรัพย์ เครือข่าย และกระเป๋า แล้วโอนไปยังที่อยู่ที่แสดง");
        return m;
    }
}
