package com.pg.splitpay;

import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/** 분할결제 안내 메일 — KO/EN/JP/CH/TH */
public final class SplitPayMailI18n {

    private SplitPayMailI18n() {
    }

    public static String subject(String lang, String contractNo, int installmentNo) {
        String l = norm(lang);
        return switch (l) {
            case "ENG" -> "[ICOPAY] Split payment · " + contractNo + " · #" + installmentNo;
            case "JPN" -> "[ICOPAY] 分割払いのお支払い · " + contractNo + " · 第" + installmentNo + "回";
            case "CHN" -> "[ICOPAY] 分期付款 · " + contractNo + " · 第" + installmentNo + "期";
            case "THA" -> "[ICOPAY] การชำระแบบแบ่งงวด · " + contractNo + " · งวดที่ " + installmentNo;
            default -> "[ICOPAY] 분할결제 안내 · " + contractNo + " · " + installmentNo + "회차";
        };
    }

    public static String body(String lang, SplitPayContract c, SplitPayInstallment inst, String payUrl) {
        String l = norm(lang);
        String amt = fmt(inst.getAmount());
        String cur = c.getCurrencyCode() != null ? c.getCurrencyCode() : "";
        String due = inst.getDueDateAdjusted() != null ? inst.getDueDateAdjusted().toString() : "";
        return switch (l) {
            case "ENG" -> """
                    ICOPAY split payment notice

                    Contract: %s
                    Installment: %d / %d
                    Amount: %s %s
                    Due date: %s
                    Order No: %s

                    Please pay at the link below:
                    %s
                    """.formatted(c.getContractNo(), inst.getInstallmentNo(), c.getInstallmentCount(),
                    amt, cur, due, inst.getOrderNo(), payUrl);
            case "JPN" -> """
                    ICOPAY 分割払いのご案内

                    契約番号: %s
                    回数: %d / %d
                    金額: %s %s
                    支払期日: %s
                    注文番号: %s

                    下記リンクよりお支払いください。
                    %s
                    """.formatted(c.getContractNo(), inst.getInstallmentNo(), c.getInstallmentCount(),
                    amt, cur, due, inst.getOrderNo(), payUrl);
            case "CHN" -> """
                    ICOPAY 分期付款通知

                    合同号: %s
                    期数: %d / %d
                    金额: %s %s
                    到期日: %s
                    订单号: %s

                    请点击以下链接完成付款:
                    %s
                    """.formatted(c.getContractNo(), inst.getInstallmentNo(), c.getInstallmentCount(),
                    amt, cur, due, inst.getOrderNo(), payUrl);
            case "THA" -> """
                    แจ้งการชำระแบบแบ่งงวด ICOPAY

                    เลขสัญญา: %s
                    งวด: %d / %d
                    จำนวน: %s %s
                    วันครบกำหนด: %s
                    เลขคำสั่ง: %s

                    ชำระได้ที่ลิงก์:
                    %s
                    """.formatted(c.getContractNo(), inst.getInstallmentNo(), c.getInstallmentCount(),
                    amt, cur, due, inst.getOrderNo(), payUrl);
            default -> """
                    ICOPAY 분할결제 안내

                    계약번호: %s
                    회차: %d / %d
                    금액: %s %s
                    결제예정일: %s
                    주문번호: %s

                    아래 링크에서 결제해 주세요.
                    %s
                    """.formatted(c.getContractNo(), inst.getInstallmentNo(), c.getInstallmentCount(),
                    amt, cur, due, inst.getOrderNo(), payUrl);
        };
    }

    private static String norm(String lang) {
        if (lang == null || lang.isBlank()) {
            return "KOR";
        }
        return lang.trim().toUpperCase(Locale.ROOT);
    }

    private static String fmt(BigDecimal v) {
        return v != null ? v.stripTrailingZeros().toPlainString() : "";
    }
}
