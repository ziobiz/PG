package com.pg.merchantdeploy;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 가맹점 운영 PG vs 요청 API 벤더 불일치 경고 — KO/EN/JP/CH/TH (가맹 응답에 운영 PG명 미노출) */
public final class MerchantOperationalPgGuardI18n {

    public static final String KEY_PG_VENDOR_MISMATCH = "PG_VENDOR_MISMATCH";

    private MerchantOperationalPgGuardI18n() {
    }

    public static Map<String, String> allLang(String configuredVendor, String requestedVendor, String operationalPgCd) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String lang : new String[]{"KO", "EN", "JP", "CH", "TH"}) {
            m.put(lang, format(lang, configuredVendor, requestedVendor, operationalPgCd));
        }
        return m;
    }

    public static String format(String lang, String configuredVendor, String requestedVendor, String operationalPgCd) {
        /* 가맹점 응답에는 운영 PG·레거시 API 이름을 넣지 않는다. (내부 로그만 사용) */
        return switch (normalizeLang(lang)) {
            case "EN" -> en(KEY_PG_VENDOR_MISMATCH);
            case "JP" -> jp(KEY_PG_VENDOR_MISMATCH);
            case "CH" -> ch(KEY_PG_VENDOR_MISMATCH);
            case "TH" -> th(KEY_PG_VENDOR_MISMATCH);
            default -> ko(KEY_PG_VENDOR_MISMATCH);
        };
    }

    private static String normalizeLang(String lang) {
        if (lang == null) {
            return "KO";
        }
        String u = lang.trim().toUpperCase(Locale.ROOT);
        if (u.startsWith("EN") || "ENG".equals(u)) {
            return "EN";
        }
        if (u.startsWith("JA") || "JP".equals(u) || "JPN".equals(u)) {
            return "JP";
        }
        if (u.startsWith("ZH") || "CH".equals(u) || "CHN".equals(u)) {
            return "CH";
        }
        if (u.startsWith("TH") || "THA".equals(u)) {
            return "TH";
        }
        return "KO";
    }

    private static String ko(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "요청하신 결제 API가 이 가맹점의 ICOPAY 연동 설정과 맞지 않습니다. "
                    + "통합 Checkout API(POST /api/middleware/v1/merchant/checkout/prepare)와 배포 문서를 사용하거나 운영 담당자에게 문의하세요. 결제 중계가 중지되었습니다.";
            default -> k;
        };
    }

    private static String en(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "The payment API you called does not match this merchant's ICOPAY integration settings. "
                    + "Use the unified Checkout API (POST /api/middleware/v1/merchant/checkout/prepare) and the deployment docs, "
                    + "or contact your operator. Payment relay has been stopped.";
            default -> k;
        };
    }

    private static String jp(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "ご利用の決済APIが、この加盟店のICOPAY連携設定と一致しません。"
                    + "統合 Checkout API(POST /api/middleware/v1/merchant/checkout/prepare)と配布ドキュメントをご利用ください。"
                    + "または運用担当者にお問い合わせください。決済中継を停止しました。";
            default -> k;
        };
    }

    private static String ch(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "您调用的支付 API 与该商户的 ICOPAY 对接设置不一致。"
                    + "请使用统一 Checkout API（POST /api/middleware/v1/merchant/checkout/prepare）及部署文档，或联系运营负责人。已停止支付中转。";
            default -> k;
        };
    }

    private static String th(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "Payment API ที่เรียกไม่ตรงกับการตั้งค่า ICOPAY ของร้านค้านี้ "
                    + "โปรดใช้ Unified Checkout API (POST /api/middleware/v1/merchant/checkout/prepare) และเอกสาร배포 "
                    + "หรือติดต่อผู้ดูแลระบบ การส่งต่อการชำระเงินถูกหยุดแล้ว";
            default -> k;
        };
    }
}
