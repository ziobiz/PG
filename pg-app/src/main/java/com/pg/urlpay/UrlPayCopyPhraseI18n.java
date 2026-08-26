package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * URL 결제 폼 카피·탭 제목 — 한국어만 저장된 항목의 ENG/JPN/CHN/THA 폴백.
 */
public final class UrlPayCopyPhraseI18n {

    private UrlPayCopyPhraseI18n() {
    }

    private static final Map<String, Map<String, String>> PHRASES = new LinkedHashMap<>();

    static {
        put("온더라인 간편결제 시스템",
                "OnTheLine Easy Payment System",
                "オンザラインかんたん決済システム",
                "OnTheLine 便捷支付系统",
                "ระบบชำระเงินง่าย OnTheLine");
        put("결제안내",
                "Payment information",
                "決済案内",
                "支付说明",
                "ข้อมูลการชำระเงิน");
        put("감사합니다",
                "Thank you",
                "ありがとうございます",
                "谢谢",
                "ขอบคุณ");
        put("주의 - 최종 결제 통화는 태국 바트입니다.",
                "Note — the final payment currency is Thai Baht.",
                "注意 — 最終決済通貨はタイバーツです。",
                "注意 — 最终支付货币为泰铢。",
                "หมายเหตุ — สกุลเงินที่ชำระจริงคือบาทไทย");
        put("해외결제 가능 카드만 허용",
                "Only cards eligible for overseas payment are accepted",
                "海外決済可能なカードのみご利用いただけます",
                "仅限可用于境外支付的卡",
                "รับเฉพาะบัตรที่ใช้ชำระต่างประเทศได้");
        put("(국내카드 사용불가)",
                "(Domestic cards cannot be used)",
                "(国内カードはご利用いただけません)",
                "(不可使用国内卡)",
                "(ใช้บัตรในประเทศไม่ได้)");
        put("(국내카드 사용불가",
                "(Domestic cards cannot be used)",
                "(国内カードはご利用いただけません)",
                "(不可使用国内卡)",
                "(ใช้บัตรในประเทศไม่ได้)");
        put("해외결제 가능 카드만 허용 (국내카드 사용불가)",
                "Only cards eligible for overseas payment are accepted (domestic cards cannot be used)",
                "海外決済可能なカードのみご利用いただけます（国内カードはご利用いただけません）",
                "仅限可用于境外支付的卡（不可使用国内卡）",
                "รับเฉพาะบัตรที่ใช้ชำระต่างประเทศได้ (ใช้บัตรในประเทศไม่ได้)");
        put("해외결제 가능 카드만 허용 (국내카드 사용불가",
                "Only cards eligible for overseas payment are accepted (domestic cards cannot be used)",
                "海外決済可能なカードのみご利用いただけます（国内カードはご利用いただけません）",
                "仅限可用于境外支付的卡（不可使用国内卡）",
                "รับเฉพาะบัตรที่ใช้ชำระต่างประเทศได้ (ใช้บัตรในประเทศไม่ได้)");
        put("이름 입력은 카드에 표시된 이름형식과 동일하게 입력해야 합니다.",
                "Enter the name in the same format as printed on the card.",
                "カードに記載の氏名と同じ形式で入力してください。",
                "请按卡面记载的姓名格式输入。",
                "กรอกชื่อให้ตรงกับรูปแบบที่พิมพ์บนบัตร");
        put("사용카드: VISA, MASTER, JCB, UNIONPAY",
                "Cards accepted: VISA, MASTER, JCB, UNIONPAY",
                "ご利用カード: VISA, MASTER, JCB, UNIONPAY",
                "可用卡: VISA, MASTER, JCB, UNIONPAY",
                "บัตรที่ใช้ได้: VISA, MASTER, JCB, UNIONPAY");
    }

    private static void put(String ko, String en, String jp, String ch, String th) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("EN", en);
        row.put("JP", jp);
        row.put("CH", ch);
        row.put("TH", th);
        PHRASES.put(ko, row);
        String compact = compact(ko);
        if (!compact.equals(ko)) {
            PHRASES.putIfAbsent(compact, row);
        }
    }

    static String compact(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\r', '\n')
                .replace('\u00a0', ' ')
                .replaceAll("[ \\t]*\\n[ \\t]*", " ")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    static String norm(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\r\n", "\n").replace('\r', '\n').replace('\u00a0', ' ').trim();
    }

    public static String lookup(String korean, String lang) {
        String loc = normalizeLoc(lang);
        if (loc.isEmpty() || "KO".equals(loc)) {
            return norm(korean);
        }
        String exact = lookupExact(korean, loc);
        if (!exact.isEmpty()) {
            return exact;
        }
        String raw = norm(korean);
        if (raw.isEmpty() || !raw.contains("\n")) {
            return "";
        }
        String[] lines = raw.split("\n", -1);
        StringBuilder out = new StringBuilder();
        boolean any = false;
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            String tr = lookupExact(lines[i], loc);
            if (!tr.isEmpty()) {
                out.append(tr);
                any = true;
            } else {
                out.append(lines[i]);
            }
        }
        return any ? out.toString() : "";
    }

    private static String lookupExact(String korean, String loc) {
        String k1 = norm(korean);
        if (k1.isEmpty()) {
            return "";
        }
        Map<String, String> row = PHRASES.get(k1);
        if (row == null) {
            row = PHRASES.get(compact(k1));
        }
        if (row == null && k1.contains("\n")) {
            row = PHRASES.get(k1.replace('\n', ' ').replaceAll(" +", " ").trim());
        }
        if (row == null) {
            return "";
        }
        String v = row.get(loc);
        return v != null ? v : "";
    }

    /**
     * JSON 언어맵에서 비어 있거나 한국어와 동일한 값을 사전으로 채운다.
     */
    public static Map<String, String> fillMissing(Map<String, String> src) {
        Map<String, String> out = new LinkedHashMap<>();
        if (src != null) {
            src.forEach((k, v) -> {
                if (k != null && v != null && !v.isBlank()) {
                    out.put(k.trim().toUpperCase(Locale.ROOT), v.trim());
                }
            });
        }
        String ko = first(out, "KOR", "KO");
        if (ko == null || ko.isBlank()) {
            return out;
        }
        putIfBlankOrSameAsKo(out, "ENG", lookup(ko, "EN"), ko);
        putIfBlankOrSameAsKo(out, "JPN", lookup(ko, "JP"), ko);
        putIfBlankOrSameAsKo(out, "CHN", lookup(ko, "CH"), ko);
        putIfBlankOrSameAsKo(out, "THA", lookup(ko, "TH"), ko);
        return out;
    }

    private static String first(Map<String, String> m, String a, String b) {
        String v = m.get(a);
        if (v != null && !v.isBlank()) {
            return v;
        }
        return m.get(b);
    }

    private static void putIfBlankOrSameAsKo(Map<String, String> out, String code, String translated, String ko) {
        if (translated == null || translated.isBlank()) {
            return;
        }
        String cur = out.get(code);
        if (cur == null || cur.isBlank() || cur.equals(ko)) {
            out.put(code, translated);
        }
    }

    private static String normalizeLoc(String lang) {
        if (lang == null) {
            return "";
        }
        String L = lang.trim().toUpperCase(Locale.ROOT);
        return switch (L) {
            case "KO", "KOR", "KR" -> "KO";
            case "JP", "JPN", "JA" -> "JP";
            case "CH", "CHN", "ZH", "CN" -> "CH";
            case "TH", "THA" -> "TH";
            case "EN", "ENG" -> "EN";
            default -> L;
        };
    }
}
