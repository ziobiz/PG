package com.pg.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 사전 리스크 필터 사용자 메시지 — KO/EN/JP/CH/TH.
 */
public final class PayPresaleRiskFilterI18n {

    private PayPresaleRiskFilterI18n() {
    }

    public static Map<String, String> allLang(String filterCode, Map<String, String> params) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("KOR", message("KOR", filterCode, params));
        out.put("ENG", message("ENG", filterCode, params));
        out.put("JP", message("JP", filterCode, params));
        out.put("CHN", message("CHN", filterCode, params));
        out.put("THA", message("THA", filterCode, params));
        return out;
    }

    public static String message(String lang, String filterCode, Map<String, String> params) {
        String l = lang != null ? lang.trim().toUpperCase() : "KOR";
        if (l.startsWith("KO")) {
            l = "KOR";
        }
        if (l.startsWith("EN")) {
            l = "ENG";
        }
        if (l.startsWith("JA") || "JPN".equals(l)) {
            l = "JP";
        }
        if (l.startsWith("ZH") || "CHN".equals(l) || "CH".equals(l)) {
            l = "CHN";
        }
        return switch (l) {
            case "ENG" -> eng(filterCode, params);
            case "JP" -> jp(filterCode, params);
            case "CHN" -> ch(filterCode, params);
            case "THA" -> th(filterCode, params);
            default -> ko(filterCode, params);
        };
    }

    private static String ko(String code, Map<String, String> p) {
        return switch (code) {
            case PayPresaleRiskFilterCodes.BUYER_EMAIL_MISMATCH ->
                    "이전 결제 이력의 이메일(" + p.getOrDefault("prevEmail", "") + ")과 다릅니다. 동일 정보로 다시 시도해 주세요.";
            case PayPresaleRiskFilterCodes.BUYER_PHONE_MISMATCH ->
                    "이전 결제 이력의 전화번호(" + p.getOrDefault("prevPhone", "") + ")와 다릅니다. 동일 정보로 다시 시도해 주세요.";
            case PayPresaleRiskFilterCodes.BUYER_NAME_MISMATCH ->
                    "이전 결제 이력의 성명(" + p.getOrDefault("prevName", "") + ")과 다릅니다. 동일 정보로 다시 시도해 주세요.";
            case PayPresaleRiskFilterCodes.HOLDER_NAME_SUSPICIOUS ->
                    "입력하신 카드 명의가 결제 정책상 허용되지 않습니다. 실명 카드로 다시 시도해 주세요.";
            case PayPresaleRiskFilterCodes.VELOCITY_CARD ->
                    "동일 카드로 짧은 시간에 너무 많은 시도가 있었습니다. 잠시 후 다시 시도해 주세요.";
            case PayPresaleRiskFilterCodes.VELOCITY_EMAIL ->
                    "동일 이메일로 짧은 시간에 너무 많은 시도가 있었습니다. 잠시 후 다시 시도해 주세요.";
            case PayPresaleRiskFilterCodes.VELOCITY_IP ->
                    "동일 접속에서 짧은 시간에 너무 많은 시도가 있었습니다. 잠시 후 다시 시도해 주세요.";
            case PayPresaleRiskFilterCodes.PHONE_INVALID ->
                    "입력하신 전화번호 형식이 올바르지 않습니다(예: 0000000000). 실제 연락 가능한 번호를 입력해 주세요.";
            case PayPresaleRiskFilterCodes.EMAIL_INVALID ->
                    "입력하신 이메일 형식이 올바르지 않습니다. 유효한 이메일을 입력해 주세요.";
            default -> "결제 정책에 따라 요청이 취소되었습니다.";
        };
    }

    private static String eng(String code, Map<String, String> p) {
        return switch (code) {
            case PayPresaleRiskFilterCodes.BUYER_EMAIL_MISMATCH ->
                    "Email differs from the previous payment (" + p.getOrDefault("prevEmail", "") + "). Please use the same details.";
            case PayPresaleRiskFilterCodes.BUYER_PHONE_MISMATCH ->
                    "Phone differs from the previous payment (" + p.getOrDefault("prevPhone", "") + "). Please use the same details.";
            case PayPresaleRiskFilterCodes.BUYER_NAME_MISMATCH ->
                    "Name differs from the previous payment (" + p.getOrDefault("prevName", "") + "). Please use the same details.";
            case PayPresaleRiskFilterCodes.HOLDER_NAME_SUSPICIOUS ->
                    "The cardholder name is not allowed by payment policy. Please use a valid card.";
            case PayPresaleRiskFilterCodes.VELOCITY_CARD ->
                    "Too many attempts with the same card. Please try again later.";
            case PayPresaleRiskFilterCodes.VELOCITY_EMAIL ->
                    "Too many attempts with the same email. Please try again later.";
            case PayPresaleRiskFilterCodes.VELOCITY_IP ->
                    "Too many attempts from this connection. Please try again later.";
            case PayPresaleRiskFilterCodes.PHONE_INVALID ->
                    "The phone number format is invalid (e.g. 0000000000). Please enter a valid contact number.";
            case PayPresaleRiskFilterCodes.EMAIL_INVALID ->
                    "The email format is invalid. Please enter a valid email address.";
            default -> "This payment was cancelled by risk policy.";
        };
    }

    private static String jp(String code, Map<String, String> p) {
        return switch (code) {
            case PayPresaleRiskFilterCodes.BUYER_EMAIL_MISMATCH ->
                    "前回の決済メール(" + p.getOrDefault("prevEmail", "") + ")と異なります。同じ情報で再試行してください。";
            case PayPresaleRiskFilterCodes.BUYER_PHONE_MISMATCH ->
                    "前回の決済電話(" + p.getOrDefault("prevPhone", "") + ")と異なります。同じ情報で再試行してください。";
            case PayPresaleRiskFilterCodes.BUYER_NAME_MISMATCH ->
                    "前回の決済氏名(" + p.getOrDefault("prevName", "") + ")と異なります。同じ情報で再試行してください。";
            case PayPresaleRiskFilterCodes.HOLDER_NAME_SUSPICIOUS ->
                    "カード名義が決済ポリシー上許可されていません。実名カードで再試行してください。";
            case PayPresaleRiskFilterCodes.VELOCITY_CARD ->
                    "同一カードでの短時間試行が多すぎます。しばらくしてから再試行してください。";
            case PayPresaleRiskFilterCodes.VELOCITY_EMAIL ->
                    "同一メールでの短時間試行が多すぎます。しばらくしてから再試行してください。";
            case PayPresaleRiskFilterCodes.VELOCITY_IP ->
                    "同一接続からの短時間試行が多すぎます。しばらくしてから再試行してください。";
            case PayPresaleRiskFilterCodes.PHONE_INVALID ->
                    "電話番号の形式が正しくありません(例: 0000000000)。有効な番号を入力してください。";
            case PayPresaleRiskFilterCodes.EMAIL_INVALID ->
                    "メール形式が正しくありません。有効なメールを入力してください。";
            default -> "リスクポリシーにより決済がキャンセルされました。";
        };
    }

    private static String ch(String code, Map<String, String> p) {
        return switch (code) {
            case PayPresaleRiskFilterCodes.BUYER_EMAIL_MISMATCH ->
                    "邮箱与上次支付(" + p.getOrDefault("prevEmail", "") + ")不一致，请使用相同信息重试。";
            case PayPresaleRiskFilterCodes.BUYER_PHONE_MISMATCH ->
                    "电话与上次支付(" + p.getOrDefault("prevPhone", "") + ")不一致，请使用相同信息重试。";
            case PayPresaleRiskFilterCodes.BUYER_NAME_MISMATCH ->
                    "姓名与上次支付(" + p.getOrDefault("prevName", "") + ")不一致，请使用相同信息重试。";
            case PayPresaleRiskFilterCodes.HOLDER_NAME_SUSPICIOUS ->
                    "持卡人姓名不符合支付政策，请使用实名卡重试。";
            case PayPresaleRiskFilterCodes.VELOCITY_CARD ->
                    "同一卡短时间尝试过多，请稍后再试。";
            case PayPresaleRiskFilterCodes.VELOCITY_EMAIL ->
                    "同一邮箱短时间尝试过多，请稍后再试。";
            case PayPresaleRiskFilterCodes.VELOCITY_IP ->
                    "同一连接短时间尝试过多，请稍后再试。";
            case PayPresaleRiskFilterCodes.PHONE_INVALID ->
                    "电话号码格式无效(如 0000000000)，请输入有效号码。";
            case PayPresaleRiskFilterCodes.EMAIL_INVALID ->
                    "邮箱格式无效，请输入有效邮箱。";
            default -> "因风险策略，本次支付已取消。";
        };
    }

    private static String th(String code, Map<String, String> p) {
        return switch (code) {
            case PayPresaleRiskFilterCodes.BUYER_EMAIL_MISMATCH ->
                    "อีเมลไม่ตรงกับครั้งก่อน (" + p.getOrDefault("prevEmail", "") + ") กรุณาใช้ข้อมูลเดิม";
            case PayPresaleRiskFilterCodes.BUYER_PHONE_MISMATCH ->
                    "โทรศัพท์ไม่ตรงกับครั้งก่อน (" + p.getOrDefault("prevPhone", "") + ") กรุณาใช้ข้อมูลเดิม";
            case PayPresaleRiskFilterCodes.BUYER_NAME_MISMATCH ->
                    "ชื่อไม่ตรงกับครั้งก่อน (" + p.getOrDefault("prevName", "") + ") กรุณาใช้ข้อมูลเดิม";
            case PayPresaleRiskFilterCodes.HOLDER_NAME_SUSPICIOUS ->
                    "ชื่อผู้ถือบัตรไม่ได้รับอนุญาตตามนโยบาย กรุณาใช้บัตรจริง";
            case PayPresaleRiskFilterCodes.VELOCITY_CARD ->
                    "ลองบัตรเดิมบ่อยเกินไป กรุณารอสักครู่";
            case PayPresaleRiskFilterCodes.VELOCITY_EMAIL ->
                    "ลองอีเมลเดิมบ่อยเกินไป กรุณารอสักครู่";
            case PayPresaleRiskFilterCodes.VELOCITY_IP ->
                    "ลองจากการเชื่อมต่อเดิมบ่อยเกินไป กรุณารอสักครู่";
            case PayPresaleRiskFilterCodes.PHONE_INVALID ->
                    "รูปแบบหมายเลขโทรศัพท์ไม่ถูกต้อง (เช่น 0000000000) กรุณากรอกหมายเลขที่ใช้ได้จริง";
            case PayPresaleRiskFilterCodes.EMAIL_INVALID ->
                    "รูปแบบอีเมลไม่ถูกต้อง กรุณากรอกอีเมลที่ใช้ได้";
            default -> "การชำระเงินถูกยกเลิกตามนโยบายความเสี่ยง";
        };
    }

    public static String filterLabelKo(String code) {
        return switch (code) {
            case PayPresaleRiskFilterCodes.BUYER_EMAIL_MISMATCH -> "구매자 이메일 불일치";
            case PayPresaleRiskFilterCodes.BUYER_PHONE_MISMATCH -> "구매자 전화 불일치";
            case PayPresaleRiskFilterCodes.BUYER_NAME_MISMATCH -> "구매자 성명 불일치";
            case PayPresaleRiskFilterCodes.HOLDER_NAME_SUSPICIOUS -> "의심 holder명";
            case PayPresaleRiskFilterCodes.VELOCITY_CARD -> "카드 속도 제한";
            case PayPresaleRiskFilterCodes.VELOCITY_EMAIL -> "이메일 속도 제한";
            case PayPresaleRiskFilterCodes.VELOCITY_IP -> "IP 속도 제한";
            case PayPresaleRiskFilterCodes.PHONE_INVALID -> "비정상 전화번호";
            case PayPresaleRiskFilterCodes.EMAIL_INVALID -> "비정상 이메일";
            case JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_HIGH_RISK -> "JPAY 사후 고위험";
            case JpayPostSaleRiskOutcomeUtil.POSTSALE_JPAY_PY0124 -> "JPAY PY0124";
            default -> code;
        };
    }
}
