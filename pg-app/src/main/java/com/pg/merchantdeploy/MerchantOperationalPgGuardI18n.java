package com.pg.merchantdeploy;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 가맹점 운영 PG vs 요청 API 벤더 불일치 경고 — KO/EN/JP/CH/TH */
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
        String template = switch (normalizeLang(lang)) {
            case "EN" -> en(KEY_PG_VENDOR_MISMATCH);
            case "JP" -> jp(KEY_PG_VENDOR_MISMATCH);
            case "CH" -> ch(KEY_PG_VENDOR_MISMATCH);
            case "TH" -> th(KEY_PG_VENDOR_MISMATCH);
            default -> ko(KEY_PG_VENDOR_MISMATCH);
        };
        return template
                .replace("{0}", nz(configuredVendor))
                .replace("{1}", nz(requestedVendor))
                .replace("{2}", nz(operationalPgCd));
    }

    private static String nz(String s) {
        return s != null ? s : "";
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
            case KEY_PG_VENDOR_MISMATCH -> "등록된 결제대행사({0})와 요청한 결제 API({1})가 일치하지 않습니다. "
                    + "ICOPAY에 배포된 연동 문서·엔드포인트(운영 PG: {2})를 확인하거나 운영 담당자에게 문의하세요. 결제 중계가 중지되었습니다.";
            default -> k;
        };
    }

    private static String en(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "The registered payment provider ({0}) does not match the requested payment API ({1}). "
                    + "Please verify the integration docs and endpoints deployed in ICOPAY (operational PG: {2}) "
                    + "or contact your operator. Payment relay has been stopped.";
            default -> k;
        };
    }

    private static String jp(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "登録済みの決済代行({0})とリクエストした決済API({1})が一致しません。"
                    + "ICOPAYに配布された連携ドキュメント・エンドポイント(運用PG: {2})を確認するか、運用担当者にお問い合わせください。決済中継を停止しました。";
            default -> k;
        };
    }

    private static String ch(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "已登记的支付服务商({0})与请求的支付 API({1})不一致。"
                    + "请核对 ICOPAY 已部署的对接文档与端点(运营 PG: {2})，或联系运营负责人。已停止支付中转。";
            default -> k;
        };
    }

    private static String th(String k) {
        return switch (k) {
            case KEY_PG_VENDOR_MISMATCH -> "ผู้ให้บริการชำระเงินที่ลงทะเบียน ({0}) ไม่ตรงกับ Payment API ที่ร้องขอ ({1}) "
                    + "โปรดตรวจสอบเอกสารและ endpoint ที่ ICOPAY แจกจ่าย (PG ปฏิบัติการ: {2}) หรือติดต่อผู้ดูแลระบบ "
                    + "การส่งต่อการชำระเงินถูกหยุดแล้ว";
            default -> k;
        };
    }
}
