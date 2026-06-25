package com.pg.service;

import com.pg.util.PayCardBrand;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 결제 카드 정책 메시지 — KO/EN/JP/CH/TH */
public final class PayCardPolicyI18n {

    private PayCardPolicyI18n() {
    }

    public static Map<String, String> allLang(String messageKey, Object... args) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String lang : new String[]{"KO", "EN", "JP", "CH", "TH"}) {
            m.put(lang, format(lang, messageKey, args));
        }
        return m;
    }

    public static String format(String lang, String messageKey, Object... args) {
        String template = switch (normalizeLang(lang)) {
            case "EN" -> en(messageKey);
            case "JP" -> jp(messageKey);
            case "CH" -> ch(messageKey);
            case "TH" -> th(messageKey);
            default -> ko(messageKey);
        };
        if (args == null || args.length == 0) {
            return template;
        }
        String s = template;
        for (int i = 0; i < args.length; i++) {
            s = s.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return s;
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
            case "BLOCKED_PREFIX" -> "이 카드번호는 사용할 수 없습니다. (BIN {0})";
            case "BLACKLIST", "INACTIVE_CARD" -> "해당 카드는 사용중지 된 카드입니다. 관리자에게 문의 바랍니다.";
            case "CARD_COOLDOWN" -> "이 카드는 잠시 사용할 수 없습니다. 약 {0}분 후 다시 시도해 주세요.";
            case "CARD_COOLDOWN_TIER_1" -> "1차 결제 실패 경고입니다. 동일 카드는 약 {0}분 후 다시 시도할 수 있습니다.";
            case "CARD_COOLDOWN_TIER_2" -> "2차 결제 실패 경고입니다. 동일 카드는 약 {0}분 후 다시 시도할 수 있습니다.";
            case "CARD_COOLDOWN_TIER_3" -> "3차 결제 실패 경고입니다. 동일 카드는 약 {0}분 후 다시 시도할 수 있습니다.";
            case "CARD_COOLDOWN_TIER_4" -> "4차 결제 실패 경고입니다. 동일 카드는 약 {0}분 후 다시 시도할 수 있습니다.";
            case "BRAND_NOT_ALLOWED" -> "이 결제수단(PG)에서는 {0} 카드를 사용할 수 없습니다.";
            case "UNION_NOT_62" -> "유니온페이는 62로 시작하는 카드만 사용할 수 있습니다.";
            case "UNION_60_81" -> "60·81로 시작하는 유니온페이 카드는 사용할 수 없습니다.";
            case "AMEX_LEN" -> "아메리칸 익스프레스는 15자리입니다.";
            case "CARD_LEN" -> "카드번호는 {0}자리여야 합니다.";
            case "INVALID_PAN" -> "카드번호를 확인해 주세요.";
            case "SELECT_BRAND" -> "카드 종류를 선택해 주세요.";
            default -> k;
        };
    }

    private static String en(String k) {
        return switch (k) {
            case "BLOCKED_PREFIX" -> "This card number cannot be used. (BIN {0})";
            case "BLACKLIST", "INACTIVE_CARD" -> "This card has been suspended. Please contact the administrator.";
            case "CARD_COOLDOWN" -> "This card is temporarily unavailable. Please try again in about {0} minute(s).";
            case "CARD_COOLDOWN_TIER_1" -> "1st payment failure warning. Please try this card again in about {0} minute(s).";
            case "CARD_COOLDOWN_TIER_2" -> "2nd payment failure warning. Please try this card again in about {0} minute(s).";
            case "CARD_COOLDOWN_TIER_3" -> "3rd payment failure warning. Please try this card again in about {0} minute(s).";
            case "CARD_COOLDOWN_TIER_4" -> "4th payment failure warning. Please try this card again in about {0} minute(s).";
            case "BRAND_NOT_ALLOWED" -> "{0} cards are not accepted for this payment provider.";
            case "UNION_NOT_62" -> "Only UnionPay cards starting with 62 are accepted.";
            case "UNION_60_81" -> "UnionPay cards starting with 60 or 81 cannot be used.";
            case "AMEX_LEN" -> "American Express requires 15 digits.";
            case "CARD_LEN" -> "Card number must be {0} digits.";
            case "INVALID_PAN" -> "Please check the card number.";
            case "SELECT_BRAND" -> "Please select a card brand.";
            default -> k;
        };
    }

    private static String jp(String k) {
        return switch (k) {
            case "BLOCKED_PREFIX" -> "このカード番号はご利用いただけません。(BIN {0})";
            case "BLACKLIST", "INACTIVE_CARD" -> "このカードは利用停止されています。管理者にお問い合わせください。";
            case "CARD_COOLDOWN" -> "このカードは一時的にご利用いただけません。約{0}分後に再度お試しください。";
            case "CARD_COOLDOWN_TIER_1" -> "1回目の決済失敗警告です。同じカードは約{0}分後に再度お試しください。";
            case "CARD_COOLDOWN_TIER_2" -> "2回目の決済失敗警告です。同じカードは約{0}分後に再度お試しください。";
            case "CARD_COOLDOWN_TIER_3" -> "3回目の決済失敗警告です。同じカードは約{0}分後に再度お試しください。";
            case "CARD_COOLDOWN_TIER_4" -> "4回目の決済失敗警告です。同じカードは約{0}分後に再度お試しください。";
            case "BRAND_NOT_ALLOWED" -> "この決済では{0}カードはご利用いただけません。";
            case "UNION_NOT_62" -> "銀聯(UnionPay)は62から始まるカードのみ利用できます。";
            case "UNION_60_81" -> "60・81から始まる銀聯カードはご利用いただけません。";
            case "AMEX_LEN" -> "American Expressは15桁です。";
            case "CARD_LEN" -> "カード番号は{0}桁である必要があります。";
            case "INVALID_PAN" -> "カード番号をご確認ください。";
            case "SELECT_BRAND" -> "カードブランドを選択してください。";
            default -> k;
        };
    }

    private static String ch(String k) {
        return switch (k) {
            case "BLOCKED_PREFIX" -> "无法使用此卡号。(BIN {0})";
            case "BLACKLIST", "INACTIVE_CARD" -> "该卡已停止使用，请联系管理员。";
            case "CARD_COOLDOWN" -> "该卡暂时无法使用，请约 {0} 分钟后再试。";
            case "CARD_COOLDOWN_TIER_1" -> "第1次支付失败警告。相同卡号约 {0} 分钟后可再次尝试。";
            case "CARD_COOLDOWN_TIER_2" -> "第2次支付失败警告。相同卡号约 {0} 分钟后可再次尝试。";
            case "CARD_COOLDOWN_TIER_3" -> "第3次支付失败警告。相同卡号约 {0} 分钟后可再次尝试。";
            case "CARD_COOLDOWN_TIER_4" -> "第4次支付失败警告。相同卡号约 {0} 分钟后可再次尝试。";
            case "BRAND_NOT_ALLOWED" -> "此支付渠道不支持{0}卡。";
            case "UNION_NOT_62" -> "银联卡仅支持以62开头的卡号。";
            case "UNION_60_81" -> "以60或81开头的银联卡无法使用。";
            case "AMEX_LEN" -> "美国运通卡号为15位。";
            case "CARD_LEN" -> "卡号必须为{0}位。";
            case "INVALID_PAN" -> "请检查卡号。";
            case "SELECT_BRAND" -> "请选择卡品牌。";
            default -> k;
        };
    }

    private static String th(String k) {
        return switch (k) {
            case "BLOCKED_PREFIX" -> "ไม่สามารถใช้หมายเลขบัตรนี้ได้ (BIN {0})";
            case "BLACKLIST", "INACTIVE_CARD" -> "บัตรนี้ถูกระงับการใช้งาน กรุณาติดต่อผู้ดูแลระบบ";
            case "CARD_COOLDOWN" -> "บัตรนี้ใช้งานชั่วคราวไม่ได้ กรุณาลองอีกครั้งในอีกประมาณ {0} นาที";
            case "CARD_COOLDOWN_TIER_1" -> "คำเตือนความล้มเหลวครั้งที่ 1 ลองบัตรเดิมอีกครั้งในอีกประมาณ {0} นาที";
            case "CARD_COOLDOWN_TIER_2" -> "คำเตือนความล้มเหลวครั้งที่ 2 ลองบัตรเดิมอีกครั้งในอีกประมาณ {0} นาที";
            case "CARD_COOLDOWN_TIER_3" -> "คำเตือนความล้มเหลวครั้งที่ 3 ลองบัตรเดิมอีกครั้งในอีกประมาณ {0} นาที";
            case "CARD_COOLDOWN_TIER_4" -> "คำเตือนความล้มเหลวครั้งที่ 4 ลองบัตรเดิมอีกครั้งในอีกประมาณ {0} นาที";
            case "BRAND_NOT_ALLOWED" -> "ผู้ให้บริการชำระเงินนี้ไม่รองรับบัตร {0}";
            case "UNION_NOT_62" -> "UnionPay รองรับเฉพาะบัตรที่ขึ้นต้นด้วย 62";
            case "UNION_60_81" -> "บัตร UnionPay ที่ขึ้นต้นด้วย 60 หรือ 81 ใช้ไม่ได้";
            case "AMEX_LEN" -> "American Express ต้องมี 15 หลัก";
            case "CARD_LEN" -> "หมายเลขบัตรต้องมี {0} หลัก";
            case "INVALID_PAN" -> "กรุณาตรวจสอบหมายเลขบัตร";
            case "SELECT_BRAND" -> "กรุณาเลือกแบรนด์บัตร";
            default -> k;
        };
    }

    public static String tierCooldownMessageKey(int failCount) {
        int tier = Math.min(Math.max(failCount, 1), 4);
        return "CARD_COOLDOWN_TIER_" + tier;
    }

    public static String brandLabelKo(PayCardBrand brand) {
        return switch (brand) {
            case VISA -> "비자(Visa)";
            case MASTERCARD -> "마스터(Mastercard)";
            case JCB -> "JCB";
            case UNIONPAY -> "유니온페이(UnionPay)";
            case AMEX -> "아메리칸 익스프레스(AMEX)";
            default -> "알 수 없음";
        };
    }
}
