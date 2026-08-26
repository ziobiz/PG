package com.pg.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PG 처리사유 자주 쓰이는 문구 — AI 호출 전 정적 번역.
 */
public final class OutcomeReasonPhraseDictionary {

    private static final Map<String, Map<String, String>> EXACT = new LinkedHashMap<>();

    static {
        putExact("交易失败 : 余额不足",
                "KO", "거래 실패 : 잔액 부족",
                "EN", "Transaction failed: insufficient balance",
                "JP", "取引失敗：残高不足",
                "CH", "交易失败：余额不足",
                "TH", "ธุรกรรมล้มเหลว: ยอดเงินไม่เพียงพอ");
        putExact("交易失败",
                "KO", "거래 실패",
                "EN", "Transaction failed",
                "JP", "取引失敗",
                "CH", "交易失败",
                "TH", "ธุรกรรมล้มเหลว");
        putExact("고위험 거래로 인해 거부되었습니다.",
                "KO", "비활성 등록된 카드입니다. 다른 카드를 사용하거나 가맹점에 문의해 주세요.",
                "EN", "This card is on the inactive list. Please use a different card or contact the merchant.",
                "JP", "非アクティブ登録されたカードです。別のカードをご利用いただくか、加盟店にお問い合わせください。",
                "CH", "该卡已登记为非活跃(inactive)卡。请更换其他卡或联系商户。",
                "TH", "บัตรนี้อยู่ในรายการ inactive กรุณาใช้บัตรอื่นหรือติดต่อร้านค้า");
        putExact("This transaction was declined due to high-risk policy.",
                "KO", "비활성 등록된 카드입니다. 다른 카드를 사용하거나 가맹점에 문의해 주세요.",
                "EN", "This card is on the inactive list. Please use a different card or contact the merchant.",
                "JP", "非アクティブ登録されたカードです。別のカードをご利用いただくか、加盟店にお問い合わせください。",
                "CH", "该卡已登记为非活跃(inactive)卡。请更换其他卡或联系商户。",
                "TH", "บัตรนี้อยู่ในรายการ inactive กรุณาใช้บัตรอื่นหรือติดต่อร้านค้า");
        putExact("高リスク取引のため拒否されました。",
                "KO", "비활성 등록된 카드입니다. 다른 카드를 사용하거나 가맹점에 문의해 주세요.",
                "EN", "This card is on the inactive list. Please use a different card or contact the merchant.",
                "JP", "非アクティブ登録されたカードです。別のカードをご利用いただくか、加盟店にお問い合わせください。",
                "CH", "该卡已登记为非活跃(inactive)卡。请更换其他卡或联系商户。",
                "TH", "บัตรนี้อยู่ในรายการ inactive กรุณาใช้บัตรอื่นหรือติดต่อร้านค้า");
        putExact("因高风险交易政策，该笔交易被拒绝。",
                "KO", "비활성 등록된 카드입니다. 다른 카드를 사용하거나 가맹점에 문의해 주세요.",
                "EN", "This card is on the inactive list. Please use a different card or contact the merchant.",
                "JP", "非アクティブ登録されたカードです。別のカードをご利用いただくか、加盟店にお問い合わせください。",
                "CH", "该卡已登记为非活跃(inactive)卡。请更换其他卡或联系商户。",
                "TH", "บัตรนี้อยู่ในรายการ inactive กรุณาใช้บัตรอื่นหรือติดต่อร้านค้า");
        putExact("해당 카드는 사용중지 된 카드입니다. 관리자에게 문의 바랍니다.",
                "KO", "비활성 등록된 카드입니다. 다른 카드를 사용하거나 가맹점에 문의해 주세요.",
                "EN", "This card is on the inactive list. Please use a different card or contact the merchant.",
                "JP", "非アクティブ登録されたカードです。別のカードをご利用いただくか、加盟店にお問い合わせください。",
                "CH", "该卡已登记为非活跃(inactive)卡。请更换其他卡或联系商户。",
                "TH", "บัตรนี้อยู่ในรายการ inactive กรุณาใช้บัตรอื่นหรือติดต่อร้านค้า");
        putExact("余额不足",
                "KO", "잔액 부족",
                "EN", "Insufficient balance",
                "JP", "残高不足",
                "CH", "余额不足",
                "TH", "ยอดเงินไม่เพียงพอ");
        putExact("Insufficient balance",
                "KO", "잔액 부족",
                "EN", "Insufficient balance",
                "JP", "残高不足",
                "CH", "余额不足",
                "TH", "ยอดเงินไม่เพียงพอ");
        putExact("Insufficient of available fund",
                "KO", "결제망 가용잔액 부족",
                "EN", "Insufficient available funds (payment network)",
                "JP", "決済網の利用可能残高不足",
                "CH", "支付网络可用余额不足",
                "TH", "ยอดใช้ได้ของเครือข่ายชำระไม่พอ");
        putExact("Insufficient of available funds",
                "KO", "결제망 가용잔액 부족",
                "EN", "Insufficient available funds (payment network)",
                "JP", "決済網の利用可能残高不足",
                "CH", "支付网络可用余额不足",
                "TH", "ยอดใช้ได้ของเครือข่ายชำระไม่พอ");
        putExact("Transaction failed",
                "KO", "거래 실패",
                "EN", "Transaction failed",
                "JP", "取引失敗",
                "CH", "交易失败",
                "TH", "ธุรกรรมล้มเหลว");
        putExact("Fail Sorry, Your Credit card number or CVV or Expiration data is not vaild",
                "KO", "카드번호·CVV·유효기간이 올바르지 않습니다.",
                "EN", "Sorry, your credit card number, CVV, or expiration date is not valid.",
                "JP", "カード番号・CVV・有効期限が正しくありません。",
                "CH", "卡号、CVV 或有效期不正确。",
                "TH", "หมายเลขบัตร CVV หรือวันหมดอายุไม่ถูกต้อง");
        putExact("Sorry, Your Credit card number or CVV or Expiration data is not vaild",
                "KO", "카드번호·CVV·유효기간이 올바르지 않습니다.",
                "EN", "Sorry, your credit card number, CVV, or expiration date is not valid.",
                "JP", "カード番号・CVV・有効期限が正しくありません。",
                "CH", "卡号、CVV 或有效期不正确。",
                "TH", "หมายเลขบัตร CVV หรือวันหมดอายุไม่ถูกต้อง");
        putExact("Sorry, your credit card number, CVV, or expiration date is not valid.",
                "KO", "카드번호·CVV·유효기간이 올바르지 않습니다.",
                "EN", "Sorry, your credit card number, CVV, or expiration date is not valid.",
                "JP", "カード番号・CVV・有効期限が正しくありません。",
                "CH", "卡号、CVV 或有效期不正确。",
                "TH", "หมายเลขบัตร CVV หรือวันหมดอายุไม่ถูกต้อง");
    }

    private OutcomeReasonPhraseDictionary() {
    }

    private static void putExact(String source, String... localeAndText) {
        Map<String, String> byLocale = new LinkedHashMap<>();
        for (int i = 0; i + 1 < localeAndText.length; i += 2) {
            byLocale.put(localeAndText[i], localeAndText[i + 1]);
        }
        EXACT.put(normalizeKey(source), byLocale);
    }

    private static String normalizeKey(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String lookup(String text, String locale) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String loc = locale != null && !locale.isBlank() ? locale.trim().toUpperCase(Locale.ROOT) : "KO";
        Map<String, String> hit = EXACT.get(normalizeKey(text));
        if (hit == null) {
            return null;
        }
        String translated = hit.get(loc);
        return translated != null && !translated.isBlank() ? translated : null;
    }
}
