package com.pg.receipt;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** 고객 거래명세서 HTML (이메일 클라이언트 호환 인라인 스타일) */
public final class TransactionReceiptHtmlRenderer {

    private static final String AMOUNT_BLUE = "#2563eb";
    private static final String CHARGE_LABEL = "#111827";
    private static final String CHARGE_AMOUNT = "#b91c1c";

    private TransactionReceiptHtmlRenderer() {
    }

    public static String render(TransactionReceiptViewModel vm) {
        if (vm == null) {
            return "";
        }
        String lang = vm.lang();
        Map<String, String> labels = TransactionReceiptEmailI18n.fieldLabels(lang);
        TransactionReceiptOutcome outcome = TransactionReceiptOutcome.parse(vm.outcomeKind());
        String badgeBg = TransactionReceiptEmailI18n.outcomeBadgeBg(outcome);
        String badgeFg = TransactionReceiptEmailI18n.outcomeBadgeFg(outcome);
        String badgeBorder = TransactionReceiptEmailI18n.outcomeBadgeBorder(outcome);
        String amountBlock = amountBlockHtml(lang, vm);
        String eventDateLabel = eventDateLabel(labels, outcome);

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
        appendDetailRow(detailRows, eventDateLabel, vm.authorizedDateTime(), false);
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
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" class="pg-receipt-outer" style="background:#f0f4f8;padding:24px 12px;">
                <tr><td class="pg-receipt-outer-cell" align="center" style="text-align:center;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" class="pg-receipt-card" style="width:100%%;max-width:520px;margin:0 auto;background:#ffffff;border:1px solid #2563eb;border-radius:12px;overflow:hidden;text-align:left;">
                <tr><td class="pg-receipt-pad pg-receipt-header-pad" style="padding:20px 24px 8px 24px;text-align:left;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" class="pg-receipt-header-row" style="width:100%%;">
                    <tr>
                      <td class="pg-receipt-brand" width="50%%" align="left" style="width:50%%;font-size:13px;font-weight:700;color:#1e3a5f;text-align:left;vertical-align:middle;">ICOPAY</td>
                      <td class="pg-receipt-badge" width="50%%" align="right" style="width:50%%;text-align:right;vertical-align:middle;"><span class="pg-receipt-badge-pill" style="display:inline-block;background:%s;color:%s;font-size:11px;font-weight:700;padding:4px 10px;border-radius:10px;border:1px solid %s;">%s</span></td>
                    </tr>
                  </table>
                </td></tr>
                <tr><td class="pg-receipt-amount" align="center" style="padding:8px 24px 20px 24px;border-bottom:2px solid %s;text-align:center;">
                  %s
                </td></tr>
                <tr><td class="pg-receipt-pad" style="padding:20px 24px 8px 24px;text-align:left;">
                  <div class="pg-receipt-section-title" style="font-size:11px;font-weight:700;color:#6b7280;margin-bottom:12px;letter-spacing:0.08em;text-transform:uppercase;text-align:left;">%s</div>
                  %s
                </td></tr>
                <tr><td class="pg-receipt-pad" style="padding:8px 24px 24px 24px;text-align:left;">
                  <div class="pg-receipt-section-title" style="font-size:11px;font-weight:700;color:#6b7280;margin-bottom:10px;letter-spacing:0.08em;text-transform:uppercase;text-align:left;">%s</div>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border-radius:8px;border:1px solid #e5e7eb;">
                    %s
                  </table>
                </td></tr>
                <tr><td class="pg-receipt-footer" style="padding:12px 24px 20px 24px;background:#1f2937;color:#9ca3af;font-size:11px;text-align:center;">
                  ICOPAY Payment Services
                </td></tr>
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(
                badgeBg,
                badgeFg,
                badgeBorder,
                escape(TransactionReceiptEmailI18n.outcomeBadge(lang, outcome)),
                badgeFg,
                amountBlock,
                escape(TransactionReceiptEmailI18n.serviceProvider(lang)),
                providerBlock.toString(),
                escape(TransactionReceiptEmailI18n.paymentDetail(lang)),
                detailRows.toString());
    }

    public static String renderPlainText(TransactionReceiptViewModel vm) {
        Map<String, String> labels = TransactionReceiptEmailI18n.fieldLabels(vm.lang());
        TransactionReceiptOutcome outcome = TransactionReceiptOutcome.parse(vm.outcomeKind());
        StringBuilder sb = new StringBuilder();
        sb.append(TransactionReceiptEmailI18n.outcomeBadge(vm.lang(), outcome)).append('\n');
        sb.append(vm.currency()).append(' ').append(formatAmount(vm.amount())).append('\n');
        if (showsSeparateCharge(vm)) {
            sb.append(TransactionReceiptEmailI18n.chargeCurrencyLabel(vm.lang()))
                    .append(' ')
                    .append(vm.chargeCurrency())
                    .append(' ')
                    .append(formatAmount(vm.chargeAmount()))
                    .append('\n');
        }
        sb.append('\n');
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
        line(sb, eventDateLabel(labels, outcome), vm.authorizedDateTime());
        if (vm.approvalCode() != null && !vm.approvalCode().isBlank()) {
            line(sb, labels.get("approvalCode"), vm.approvalCode());
        }
        if (vm.paymentMethod() != null && !vm.paymentMethod().isBlank()) {
            line(sb, labels.get("paymentMethod"), vm.paymentMethod());
        }
        return sb.toString();
    }

    static boolean showsSeparateCharge(TransactionReceiptViewModel vm) {
        if (vm == null || vm.chargeCurrency() == null || vm.chargeCurrency().isBlank() || vm.chargeAmount() == null) {
            return false;
        }
        if (vm.currency() == null || vm.currency().isBlank() || vm.amount() == null) {
            return false;
        }
        return !vm.currency().trim().equalsIgnoreCase(vm.chargeCurrency().trim());
    }

    private static String amountBlockHtml(String lang, TransactionReceiptViewModel vm) {
        String displayLine = escape(vm.currency()) + " " + escape(formatAmount(vm.amount()));
        String display = """
                <div class="pg-receipt-amount-value" align="center" style="font-size:32px;font-weight:700;color:%s;letter-spacing:0.02em;text-align:center;">%s</div>
                """.formatted(AMOUNT_BLUE, displayLine);
        if (!showsSeparateCharge(vm)) {
            return display;
        }
        String chargeLine = escape(vm.chargeCurrency()) + " " + escape(formatAmount(vm.chargeAmount()));
        String chargeLabel = escape(TransactionReceiptEmailI18n.chargeCurrencyLabel(lang));
        return display + """
                <div class="pg-receipt-charge-amount" align="center" style="margin-top:8px;font-size:14px;font-weight:700;line-height:1.4;text-align:center;">
                  <span style="color:%s;">%s</span>
                  <span style="color:%s;"> %s</span>
                </div>
                """.formatted(CHARGE_LABEL, chargeLabel, CHARGE_AMOUNT, chargeLine);
    }

    private static String eventDateLabel(Map<String, String> labels, TransactionReceiptOutcome outcome) {
        TransactionReceiptOutcome o = outcome != null ? outcome : TransactionReceiptOutcome.PAID;
        String key = switch (o) {
            case FAILED -> "failedDateTime";
            case CANCELLED -> "cancelledDateTime";
            case REFUNDED -> "refundedDateTime";
            case VOIDED -> "voidedDateTime";
            default -> "authorizedDateTime";
        };
        String v = labels.get(key);
        return v != null && !v.isBlank() ? v : labels.get("authorizedDateTime");
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
                <tr><td class="pg-receipt-cell" style="padding:10px 14px;border-bottom:1px solid #e5e7eb;text-align:left;">
                  <div class="pg-receipt-row-label" style="font-size:10px;font-weight:700;color:#6b7280;margin-bottom:3px;letter-spacing:0.06em;text-transform:uppercase;text-align:left;">%s</div>
                  <div class="pg-receipt-row-value" style="font-size:13px;color:#111827;line-height:1.45;word-break:break-word;text-align:left;">%s</div>
                </td></tr>
                """.formatted(escape(label), valueHtml));
    }

    private static void appendDetailRow(StringBuilder rows, String label, String value, boolean boldValue) {
        if (value == null || value.isBlank()) {
            return;
        }
        String valueHtml = boldValue ? formatMultilineContactHtml(value, true) : formatMultilineContactHtml(value, false);
        rows.append("""
                <tr><td class="pg-receipt-cell" style="padding:10px 14px;border-bottom:1px solid #e5e7eb;text-align:left;">
                  <div class="pg-receipt-row-label" style="font-size:10px;font-weight:600;color:#6b7280;margin-bottom:3px;letter-spacing:0.04em;text-transform:uppercase;text-align:left;">%s</div>
                  <div class="pg-receipt-row-value" style="font-size:13px;color:#111827;word-break:break-word;text-align:left;">%s</div>
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
            String paymentMethod,
            String outcomeKind,
            String chargeCurrency,
            BigDecimal chargeAmount
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
            public Builder outcomeKind(String v) { data.put("outcomeKind", v); return this; }
            public Builder outcomeKind(TransactionReceiptOutcome v) {
                data.put("outcomeKind", v != null ? v.name() : TransactionReceiptOutcome.PAID.name());
                return this;
            }
            public Builder chargeCurrency(String v) { data.put("chargeCurrency", v); return this; }
            public Builder chargeAmount(BigDecimal v) {
                data.put("chargeAmount", v != null ? v.toPlainString() : null);
                return this;
            }

            public TransactionReceiptViewModel build() {
                BigDecimal amt = null;
                BigDecimal chargeAmt = null;
                try {
                    if (data.get("amount") != null) {
                        amt = new BigDecimal(data.get("amount"));
                    }
                    if (data.get("chargeAmount") != null) {
                        chargeAmt = new BigDecimal(data.get("chargeAmount"));
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
                        data.get("paymentMethod"),
                        data.get("outcomeKind"),
                        data.get("chargeCurrency"),
                        chargeAmt);
            }
        }
    }
}
