package com.pg.service;

import com.pg.util.PayCardBrand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
        m.put("KOR", m.get("KO"));
        m.put("ENG", m.get("EN"));
        m.put("JPN", m.get("JP"));
        m.put("CHN", m.get("CH"));
        m.put("THA", m.get("TH"));
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
            case "INACTIVE_CARD" -> "비활성 등록된 카드입니다. 다른 카드를 사용하거나 가맹점에 문의해 주세요.";
            case "BLACKLIST" -> "관리자에 의해 사용이 제한된 카드입니다. 다른 카드를 사용하거나 가맹점에 문의해 주세요.";
            case "CARD_COOLDOWN" -> "이 카드는 잠시 사용할 수 없습니다. 약 {0}분 후 다시 시도해 주세요.";
            case "CARD_COOLDOWN_TIER_1" -> "1차 결제 실패 경고입니다. 동일 카드는 약 {0}분 후 다시 시도할 수 있습니다.";
            case "CARD_COOLDOWN_TIER_2" -> "2차 결제 실패 경고입니다. 동일 카드는 약 {0}분 후 다시 시도할 수 있습니다.";
            case "CARD_COOLDOWN_TIER_3" -> "3차 결제 실패 경고입니다. 동일 카드는 약 {0}분 후 다시 시도할 수 있습니다.";
            case "CARD_COOLDOWN_TIER_4" -> "4차 결제 실패 경고입니다. 동일 카드는 약 {0}분 후 다시 시도할 수 있습니다.";
            case "BRAND_NOT_ALLOWED" -> "{0} 결제 가능 / {1} 사용 일시 중지";
            case "BRAND_SCOPE_ALLOWED" -> "{0} 결제 가능";
            case "BRAND_SCOPE_PAUSED" -> "{0} 사용 일시 중지";
            case "UNION_NOT_62" -> "유니온페이는 62로 시작하는 카드만 사용할 수 있습니다.";
            case "UNION_60_81" -> "60·81로 시작하는 유니온페이 카드는 사용할 수 없습니다.";
            case "AMEX_LEN" -> "아메리칸 익스프레스는 15자리입니다.";
            case "CARD_LEN" -> "카드번호는 {0}자리여야 합니다.";
            case "INVALID_PAN" -> "카드번호를 확인해 주세요.";
            case "LUHN_FAIL" -> "카드번호를 다시 확인해 주세요.";
            case "SELECT_BRAND" -> "카드 종류를 선택해 주세요.";
            default -> k;
        };
    }

    private static String en(String k) {
        return switch (k) {
            case "BLOCKED_PREFIX" -> "This card number cannot be used. (BIN {0})";
            case "INACTIVE_CARD" -> "This card is on the inactive list. Please use a different card or contact the merchant.";
            case "BLACKLIST" -> "This card is restricted by the administrator. Please use a different card or contact the merchant.";
            case "CARD_COOLDOWN" -> "This card is temporarily unavailable. Please try again in about {0} minute(s).";
            case "CARD_COOLDOWN_TIER_1" -> "1st payment failure warning. Please try this card again in about {0} minute(s).";
            case "CARD_COOLDOWN_TIER_2" -> "2nd payment failure warning. Please try this card again in about {0} minute(s).";
            case "CARD_COOLDOWN_TIER_3" -> "3rd payment failure warning. Please try this card again in about {0} minute(s).";
            case "CARD_COOLDOWN_TIER_4" -> "4th payment failure warning. Please try this card again in about {0} minute(s).";
            case "BRAND_NOT_ALLOWED" -> "{0} accepted / {1} temporarily suspended";
            case "BRAND_SCOPE_ALLOWED" -> "{0} accepted";
            case "BRAND_SCOPE_PAUSED" -> "{0} temporarily suspended";
            case "UNION_NOT_62" -> "Only UnionPay cards starting with 62 are accepted.";
            case "UNION_60_81" -> "UnionPay cards starting with 60 or 81 cannot be used.";
            case "AMEX_LEN" -> "American Express requires 15 digits.";
            case "CARD_LEN" -> "Card number must be {0} digits.";
            case "INVALID_PAN" -> "Please check the card number.";
            case "LUHN_FAIL" -> "Please check the card number again.";
            case "SELECT_BRAND" -> "Please select a card brand.";
            default -> k;
        };
    }

    private static String jp(String k) {
        return switch (k) {
            case "BLOCKED_PREFIX" -> "このカード番号はご利用いただけません。(BIN {0})";
            case "INACTIVE_CARD" -> "非アクティブ登録されたカードです。別のカードをご利用いただくか、加盟店にお問い合わせください。";
            case "BLACKLIST" -> "管理者により使用が制限されたカードです。別のカードをご利用いただくか、加盟店にお問い合わせください。";
            case "CARD_COOLDOWN" -> "このカードは一時的にご利用いただけません。約{0}分後に再度お試しください。";
            case "CARD_COOLDOWN_TIER_1" -> "1回目の決済失敗警告です。同じカードは約{0}分後に再度お試しください。";
            case "CARD_COOLDOWN_TIER_2" -> "2回目の決済失敗警告です。同じカードは約{0}分後に再度お試しください。";
            case "CARD_COOLDOWN_TIER_3" -> "3回目の決済失敗警告です。同じカードは約{0}分後に再度お試しください。";
            case "CARD_COOLDOWN_TIER_4" -> "4回目の決済失敗警告です。同じカードは約{0}分後に再度お試しください。";
            case "BRAND_NOT_ALLOWED" -> "{0} ご利用可能 / {1} 一時停止";
            case "BRAND_SCOPE_ALLOWED" -> "{0} ご利用可能";
            case "BRAND_SCOPE_PAUSED" -> "{0} 一時停止";
            case "UNION_NOT_62" -> "銀聯(UnionPay)は62から始まるカードのみ利用できます。";
            case "UNION_60_81" -> "60・81から始まる銀聯カードはご利用いただけません。";
            case "AMEX_LEN" -> "American Expressは15桁です。";
            case "CARD_LEN" -> "カード番号は{0}桁である必要があります。";
            case "INVALID_PAN" -> "カード番号をご確認ください。";
            case "LUHN_FAIL" -> "カード番号を再度ご確認ください。";
            case "SELECT_BRAND" -> "カードブランドを選択してください。";
            default -> k;
        };
    }

    private static String ch(String k) {
        return switch (k) {
            case "BLOCKED_PREFIX" -> "无法使用此卡号。(BIN {0})";
            case "INACTIVE_CARD" -> "该卡已登记为非活跃(inactive)卡。请更换其他卡或联系商户。";
            case "BLACKLIST" -> "该卡已被管理员限制使用。请更换其他卡或联系商户。";
            case "CARD_COOLDOWN" -> "该卡暂时无法使用，请约 {0} 分钟后再试。";
            case "CARD_COOLDOWN_TIER_1" -> "第1次支付失败警告。相同卡号约 {0} 分钟后可再次尝试。";
            case "CARD_COOLDOWN_TIER_2" -> "第2次支付失败警告。相同卡号约 {0} 分钟后可再次尝试。";
            case "CARD_COOLDOWN_TIER_3" -> "第3次支付失败警告。相同卡号约 {0} 分钟后可再次尝试。";
            case "CARD_COOLDOWN_TIER_4" -> "第4次支付失败警告。相同卡号约 {0} 分钟后可再次尝试。";
            case "BRAND_NOT_ALLOWED" -> "{0} 可支付 / {1} 暂时停用";
            case "BRAND_SCOPE_ALLOWED" -> "{0} 可支付";
            case "BRAND_SCOPE_PAUSED" -> "{0} 暂时停用";
            case "UNION_NOT_62" -> "银联卡仅支持以62开头的卡号。";
            case "UNION_60_81" -> "以60或81开头的银联卡无法使用。";
            case "AMEX_LEN" -> "美国运通卡号为15位。";
            case "CARD_LEN" -> "卡号必须为{0}位。";
            case "INVALID_PAN" -> "请检查卡号。";
            case "LUHN_FAIL" -> "请再次确认卡号。";
            case "SELECT_BRAND" -> "请选择卡品牌。";
            default -> k;
        };
    }

    private static String th(String k) {
        return switch (k) {
            case "BLOCKED_PREFIX" -> "ไม่สามารถใช้หมายเลขบัตรนี้ได้ (BIN {0})";
            case "INACTIVE_CARD" -> "บัตรนี้อยู่ในรายการ inactive กรุณาใช้บัตรอื่นหรือติดต่อร้านค้า";
            case "BLACKLIST" -> "บัตรนี้ถูกจำกัดโดยผู้ดูแลระบบ กรุณาใช้บัตรอื่นหรือติดต่อร้านค้า";
            case "CARD_COOLDOWN" -> "บัตรนี้ใช้งานชั่วคราวไม่ได้ กรุณาลองอีกครั้งในอีกประมาณ {0} นาที";
            case "CARD_COOLDOWN_TIER_1" -> "คำเตือนความล้มเหลวครั้งที่ 1 ลองบัตรเดิมอีกครั้งในอีกประมาณ {0} นาที";
            case "CARD_COOLDOWN_TIER_2" -> "คำเตือนความล้มเหลวครั้งที่ 2 ลองบัตรเดิมอีกครั้งในอีกประมาณ {0} นาที";
            case "CARD_COOLDOWN_TIER_3" -> "คำเตือนความล้มเหลวครั้งที่ 3 ลองบัตรเดิมอีกครั้งในอีกประมาณ {0} นาที";
            case "CARD_COOLDOWN_TIER_4" -> "คำเตือนความล้มเหลวครั้งที่ 4 ลองบัตรเดิมอีกครั้งในอีกประมาณ {0} นาที";
            case "BRAND_NOT_ALLOWED" -> "{0} ชำระได้ / {1} ระงับชั่วคราว";
            case "BRAND_SCOPE_ALLOWED" -> "{0} ชำระได้";
            case "BRAND_SCOPE_PAUSED" -> "{0} ระงับชั่วคราว";
            case "UNION_NOT_62" -> "UnionPay รองรับเฉพาะบัตรที่ขึ้นต้นด้วย 62";
            case "UNION_60_81" -> "บัตร UnionPay ที่ขึ้นต้นด้วย 60 หรือ 81 ใช้ไม่ได้";
            case "AMEX_LEN" -> "American Express ต้องมี 15 หลัก";
            case "CARD_LEN" -> "หมายเลขบัตรต้องมี {0} หลัก";
            case "INVALID_PAN" -> "กรุณาตรวจสอบหมายเลขบัตร";
            case "LUHN_FAIL" -> "กรุณาตรวจสอบหมายเลขบัตรอีกครั้ง";
            case "SELECT_BRAND" -> "กรุณาเลือกแบรนด์บัตร";
            default -> k;
        };
    }

    public static String tierCooldownMessageKey(int failCount) {
        int tier = Math.min(Math.max(failCount, 1), 4);
        return "CARD_COOLDOWN_TIER_" + tier;
    }

    public static String brandLabelKo(PayCardBrand brand) {
        return brandLabel("KO", brand);
    }

    public static String brandLabel(String lang, PayCardBrand brand) {
        PayCardBrand b = brand != null ? brand : PayCardBrand.UNKNOWN;
        return switch (normalizeLang(lang)) {
            case "EN" -> switch (b) {
                case VISA -> "Visa";
                case MASTERCARD -> "Mastercard";
                case JCB -> "JCB";
                case UNIONPAY -> "UnionPay";
                case AMEX -> "American Express";
                default -> "Unknown";
            };
            case "JP" -> switch (b) {
                case VISA -> "Visa";
                case MASTERCARD -> "Mastercard";
                case JCB -> "JCB";
                case UNIONPAY -> "銀聯";
                case AMEX -> "American Express";
                default -> "不明";
            };
            case "CH" -> switch (b) {
                case VISA -> "Visa";
                case MASTERCARD -> "Mastercard";
                case JCB -> "JCB";
                case UNIONPAY -> "银联";
                case AMEX -> "美国运通";
                default -> "未知";
            };
            case "TH" -> switch (b) {
                case VISA -> "Visa";
                case MASTERCARD -> "Mastercard";
                case JCB -> "JCB";
                case UNIONPAY -> "UnionPay";
                case AMEX -> "American Express";
                default -> "ไม่ทราบ";
            };
            default -> switch (b) {
                case VISA -> "비자";
                case MASTERCARD -> "마스터";
                case JCB -> "JCB";
                case UNIONPAY -> "유니온페이";
                case AMEX -> "아메리칸 익스프레스";
                default -> "알 수 없음";
            };
        };
    }

    /** 안내문용 짧은 브랜드명 — VISA, Master, JCB, UNION, AMX */
    public static String brandShort(PayCardBrand brand) {
        if (brand == null || brand == PayCardBrand.UNKNOWN) {
            return "";
        }
        return switch (brand) {
            case VISA -> "VISA";
            case MASTERCARD -> "Master";
            case JCB -> "JCB";
            case UNIONPAY -> "UNION";
            case AMEX -> "AMX";
            default -> "";
        };
    }

    public static String joinBrandShort(Iterable<PayCardBrand> brands, String sep) {
        String delimiter = sep != null ? sep : ", ";
        StringBuilder sb = new StringBuilder();
        if (brands != null) {
            for (PayCardBrand b : brands) {
                String label = brandShort(b);
                if (label.isEmpty()) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(delimiter);
                }
                sb.append(label);
            }
        }
        return sb.toString();
    }

    public static List<PayCardBrand> pausedBrands(Collection<PayCardBrand> allowed,
                                                 Collection<PayCardBrand> pgSupported) {
        List<PayCardBrand> out = new ArrayList<>();
        if (pgSupported == null || pgSupported.isEmpty()) {
            return out;
        }
        for (PayCardBrand b : PayCardBrand.values()) {
            if (b == PayCardBrand.UNKNOWN || !pgSupported.contains(b)) {
                continue;
            }
            if (allowed != null && allowed.contains(b)) {
                continue;
            }
            out.add(b);
        }
        return out;
    }

    public static String joinBrandLabels(String lang, Iterable<PayCardBrand> brands) {
        String sep = switch (normalizeLang(lang)) {
            case "JP", "CH" -> "、";
            default -> ", ";
        };
        StringBuilder sb = new StringBuilder();
        if (brands != null) {
            for (PayCardBrand b : brands) {
                if (b == null || b == PayCardBrand.UNKNOWN) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(sep);
                }
                sb.append(brandLabel(lang, b));
            }
        }
        return sb.length() > 0 ? sb.toString() : brandLabel(lang, PayCardBrand.UNKNOWN);
    }

    public static String formatBrandNotAllowed(String lang,
                                               Iterable<PayCardBrand> allowed,
                                               Iterable<PayCardBrand> paused) {
        String allowedTxt = joinBrandShort(allowed, ", ");
        String pausedTxt = joinBrandShort(paused, " & ");
        if (pausedTxt.isEmpty() && !allowedTxt.isEmpty()) {
            return format(lang, "BRAND_SCOPE_ALLOWED", allowedTxt);
        }
        if (allowedTxt.isEmpty() && !pausedTxt.isEmpty()) {
            return format(lang, "BRAND_SCOPE_PAUSED", pausedTxt);
        }
        return format(lang, "BRAND_NOT_ALLOWED", allowedTxt, pausedTxt);
    }

    public static Map<String, String> allLangBrandNotAllowed(Iterable<PayCardBrand> allowed,
                                                             Iterable<PayCardBrand> paused) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String lang : new String[]{"KO", "EN", "JP", "CH", "TH"}) {
            m.put(lang, formatBrandNotAllowed(lang, allowed, paused));
        }
        m.put("KOR", m.get("KO"));
        m.put("ENG", m.get("EN"));
        m.put("JPN", m.get("JP"));
        m.put("CHN", m.get("CH"));
        m.put("THA", m.get("TH"));
        return m;
    }
}
