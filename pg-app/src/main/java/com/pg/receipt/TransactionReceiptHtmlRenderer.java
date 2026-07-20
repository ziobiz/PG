package com.pg.receipt;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** 고객 거래명세서 HTML (이메일 클라이언트 호환 인라인 스타일) */
public final class TransactionReceiptHtmlRenderer {

    private static final String AMOUNT_BLUE = "#2563eb";

    private TransactionReceiptHtmlRenderer() {
    }

    public static String render(TransactionReceiptViewModel vm) {
        if (vm == null) {
            return "";
        }
        String lang = vm.lang();
        Map<String, String> labels = TransactionReceiptEmailI18n.fieldLabels(lang);
        String amountLine = escape(vm.currency()) + " " + escape(formatAmount(vm.amount()));

        StringBuilder providerBlock = new StringBuilder();
        appendProviderSection(providerBlock, labels, vm);

        StringBuilder detailRows = new StringBuilder();
        appendDetailRow(detailRows, labels.get("merchant"), vm.merchant(), true);
        appendDetailRow(detailRows, labels.get("transactionId"), vm.transactionId(), false);
        appendDetailRow(detailRows, labels.get("email"), maskEmail(vm.customerEmail()), false);
        appendDetailRow(detailRows, labels.get("contactNo"), maskPhone(vm.customerTel()), false);
        appendDetailRow(detailRows, labels.get("serviceItem"), vm.serviceItem(), false);
        appendDetailRow(detailRows, labels.get("orderNumber"), vm.orderNumber(), false);
        appendDetailRow(detailRows, labels.get("cardholder"), maskName(vm.cardholder()), false);
        appendDetailRow(detailRows, labels.get("authorizedDateTime"), vm.authorizedDateTime(), false);
        if (vm.approvalCode() != null && !vm.approvalCode().isBlank()) {
            appendDetailRow(detailRows, labels.get("approvalCode"), vm.approvalCode(), false);
        }
        if (vm.paymentMethod() != null && !vm.paymentMethod().isBlank()) {
            appendDetailRow(detailRows, labels.get("paymentMethod"), vm.paymentMethod(), false);
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f0f4f8;font-family:Segoe UI,Helvetica,Arial,sans-serif;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f0f4f8;padding:24px 12px;">
                <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border:1px solid #2563eb;border-radius:12px;overflow:hidden;">
                <tr><td style="padding:20px 24px 8px 24px;">
                  <table role="presentation" width="100%%"><tr>
                    <td style="font-size:13px;font-weight:700;color:#1e3a5f;">ICOPAY</td>
                    <td align="right"><span style="display:inline-block;background:#22c55e;color:#fff;font-size:11px;font-weight:700;padding:4px 10px;border-radius:4px;">%s</span></td>
                  </tr></table>
                </td></tr>
                <tr><td style="padding:8px 24px 20px 24px;border-bottom:2px solid #22c55e;text-align:center;">
                  <div style="font-size:32px;font-weight:700;color:%s;letter-spacing:0.02em;">%s</div>
                </td></tr>
                <tr><td style="padding:20px 24px 8px 24px;">
                  <div style="font-size:11px;font-weight:700;color:#6b7280;margin-bottom:12px;letter-spacing:0.08em;text-transform:uppercase;">%s</div>
                  %s
                </td></tr>
                <tr><td style="padding:8px 24px 24px 24px;">
                  <div style="font-size:11px;font-weight:700;color:#6b7280;margin-bottom:10px;letter-spacing:0.08em;text-transform:uppercase;">%s</div>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border-radius:8px;border:1px solid #e5e7eb;">
                    %s
                  </table>
                </td></tr>
                <tr><td style="padding:12px 24px 20px 24px;background:#1f2937;color:#9ca3af;font-size:11px;text-align:center;">
                  ICOPAY Payment Services
                </td></tr>
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(
                escape(TransactionReceiptEmailI18n.paymentSuccessful(lang)),
                AMOUNT_BLUE,
                amountLine,
                escape(TransactionReceiptEmailI18n.serviceProvider(lang)),
                providerBlock.toString(),
                escape(TransactionReceiptEmailI18n.paymentDetail(lang)),
                detailRows.toString());
    }

    public static String renderPlainText(TransactionReceiptViewModel vm) {
        Map<String, String> labels = TransactionReceiptEmailI18n.fieldLabels(vm.lang());
        StringBuilder sb = new StringBuilder();
        sb.append(TransactionReceiptEmailI18n.paymentSuccessful(vm.lang())).append('\n');
        sb.append(vm.currency()).append(' ').append(formatAmount(vm.amount())).append("\n\n");
        sb.append(TransactionReceiptEmailI18n.serviceProvider(vm.lang())).append("\n");
        line(sb, labels.get("acquirer"), vm.acquirer());
        line(sb, labels.get("paymentSwitcher"), vm.paymentSwitcher());
        line(sb, labels.get("paymentProvider"), vm.paymentProvider());
        sb.append('\n');
        sb.append(TransactionReceiptEmailI18n.paymentDetail(vm.lang())).append("\n");
        line(sb, labels.get("merchant"), vm.merchant());
        line(sb, labels.get("transactionId"), vm.transactionId());
        line(sb, labels.get("email"), maskEmail(vm.customerEmail()));
        line(sb, labels.get("contactNo"), maskPhone(vm.customerTel()));
        line(sb, labels.get("serviceItem"), vm.serviceItem());
        line(sb, labels.get("orderNumber"), vm.orderNumber());
        line(sb, labels.get("cardholder"), maskName(vm.cardholder()));
        line(sb, labels.get("authorizedDateTime"), vm.authorizedDateTime());
        if (vm.approvalCode() != null && !vm.approvalCode().isBlank()) {
            line(sb, labels.get("approvalCode"), vm.approvalCode());
        }
        if (vm.paymentMethod() != null && !vm.paymentMethod().isBlank()) {
            line(sb, labels.get("paymentMethod"), vm.paymentMethod());
        }
        return sb.toString();
    }

    /** SERVICE PROVIDER — 단일 박스에 Acquirer / Payment Switcher / Payment Provider 나열 */
    private static void appendProviderSection(StringBuilder out, Map<String, String> labels, TransactionReceiptViewModel vm) {
        StringBuilder rows = new StringBuilder();
        appendListedRow(rows, labels.get("acquirer"), vm.acquirer(), false);
        appendListedRow(rows, labels.get("paymentSwitcher"), vm.paymentSwitcher(), false);
        appendListedRow(rows, labels.get("paymentProvider"), vm.paymentProvider(), true);
        if (rows.isEmpty()) {
            return;
        }
        out.append("""
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #dbeafe;border-radius:8px;background:#f8fafc;">
                  %s
                </table>
                """.formatted(rows));
    }

    private static void appendListedRow(StringBuilder rows, String label, String value, boolean boldValue) {
        if (value == null || value.isBlank()) {
            return;
        }
        String valueHtml = boldValue ? formatMultilineContactHtml(value, true) : formatMultilineContactHtml(value, false);
        rows.append("""
                <tr><td style="padding:10px 14px;border-bottom:1px solid #e5e7eb;">
                  <div style="font-size:10px;font-weight:700;color:#6b7280;margin-bottom:3px;letter-spacing:0.06em;text-transform:uppercase;">%s</div>
                  <div style="font-size:13px;color:#111827;line-height:1.45;word-break:break-word;">%s</div>
                </td></tr>
                """.formatted(escape(label), valueHtml));
    }

    private static void appendDetailRow(StringBuilder rows, String label, String value, boolean boldValue) {
        if (value == null || value.isBlank()) {
            return;
        }
        String valueHtml = boldValue ? formatMultilineContactHtml(value, true) : formatMultilineContactHtml(value, false);
        rows.append("""
                <tr><td style="padding:10px 14px;border-bottom:1px solid #e5e7eb;">
                  <div style="font-size:10px;font-weight:600;color:#6b7280;margin-bottom:3px;letter-spacing:0.04em;text-transform:uppercase;">%s</div>
                  <div style="font-size:13px;color:#111827;word-break:break-word;">%s</div>
                </td></tr>
                """.formatted(escape(label), valueHtml));
    }

    /**
     * 연락처 블록 — 줄바꿈·「 · 」·「 / 」 구분. 첫 줄(이름)만 굵게 가능.
     */
    private static String formatMultilineContactHtml(String value, boolean boldFirstLine) {
        String v = value.trim();
        String[] lines;
        if (v.contains("\n")) {
            lines = v.split("\\R");
        } else if (v.contains(" · ")) {
            lines = v.split(" · ");
        } else if (v.contains(" / ")) {
            lines = v.split(" / ", 2);
        } else {
            lines = new String[]{v};
        }
        StringBuilder html = new StringBuilder();
        boolean firstWritten = false;
        for (String line : lines) {
            String part = line != null ? line.trim() : "";
            if (part.isEmpty()) {
                continue;
            }
            if (firstWritten) {
                html.append("<br>");
            }
            if (boldFirstLine && !firstWritten) {
                html.append("<strong style=\"font-weight:700;color:#111827;\">").append(escape(part)).append("</strong>");
            } else {
                html.append(escape(part));
            }
            firstWritten = true;
        }
        return html.toString();
    }

    private static void line(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append(label).append(": ").append(value.replace('\n', ' ')).append('\n');
    }

    private static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return nz(email);
        }
        String[] p = email.trim().split("@", 2);
        String local = p[0];
        String masked = local.length() <= 2 ? local.charAt(0) + "***" : local.substring(0, 2) + "****";
        return masked + "@" + p[1];
    }

    static String maskPhone(String tel) {
        if (tel == null || tel.isBlank()) {
            return "";
        }
        String t = tel.trim();
        if (t.length() <= 4) {
            return "****";
        }
        return t.substring(0, 2) + "*****" + t.substring(t.length() - 4);
    }

    static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String t = name.trim();
        if (t.length() <= 2) {
            return t.charAt(0) + "*";
        }
        return t.substring(0, 2) + "**********" + t.charAt(t.length() - 1);
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public record TransactionReceiptViewModel(
            String lang,
            String currency,
            BigDecimal amount,
            String acquirer,
            String paymentSwitcher,
            String paymentProvider,
            String merchant,
            String transactionId,
            String customerEmail,
            String customerTel,
            String serviceItem,
            String orderNumber,
            String cardholder,
            String authorizedDateTime,
            String approvalCode,
            String paymentMethod
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final Map<String, String> data = new LinkedHashMap<>();

            public Builder lang(String v) { data.put("lang", v); return this; }
            public Builder currency(String v) { data.put("currency", v); return this; }
            public Builder amount(BigDecimal v) { data.put("amount", v != null ? v.toPlainString() : null); return this; }
            public Builder acquirer(String v) { data.put("acquirer", v); return this; }
            public Builder paymentSwitcher(String v) { data.put("paymentSwitcher", v); return this; }
            public Builder paymentProvider(String v) { data.put("paymentProvider", v); return this; }
            public Builder merchant(String v) { data.put("merchant", v); return this; }
            public Builder transactionId(String v) { data.put("transactionId", v); return this; }
            public Builder customerEmail(String v) { data.put("customerEmail", v); return this; }
            public Builder customerTel(String v) { data.put("customerTel", v); return this; }
            public Builder serviceItem(String v) { data.put("serviceItem", v); return this; }
            public Builder orderNumber(String v) { data.put("orderNumber", v); return this; }
            public Builder cardholder(String v) { data.put("cardholder", v); return this; }
            public Builder authorizedDateTime(String v) { data.put("authorizedDateTime", v); return this; }
            public Builder approvalCode(String v) { data.put("approvalCode", v); return this; }
            public Builder paymentMethod(String v) { data.put("paymentMethod", v); return this; }

            public TransactionReceiptViewModel build() {
                BigDecimal amt = null;
                try {
                    if (data.get("amount") != null) {
                        amt = new BigDecimal(data.get("amount"));
                    }
                } catch (Exception ignored) {
                }
                return new TransactionReceiptViewModel(
                        data.get("lang"),
                        data.get("currency"),
                        amt,
                        data.get("acquirer"),
                        data.get("paymentSwitcher"),
                        data.get("paymentProvider"),
                        data.get("merchant"),
                        data.get("transactionId"),
                        data.get("customerEmail"),
                        data.get("customerTel"),
                        data.get("serviceItem"),
                        data.get("orderNumber"),
                        data.get("cardholder"),
                        data.get("authorizedDateTime"),
                        data.get("approvalCode"),
                        data.get("paymentMethod"));
            }
        }
    }
}
