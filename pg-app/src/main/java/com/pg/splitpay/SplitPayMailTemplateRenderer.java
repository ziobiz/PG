package com.pg.splitpay;

import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;

import java.math.BigDecimal;
import java.util.Locale;

public final class SplitPayMailTemplateRenderer {

    private SplitPayMailTemplateRenderer() {
    }

    public static String render(String template, SplitPayContract c, SplitPayInstallment inst, String payUrl) {
        if (template == null) {
            return "";
        }
        String out = template;
        out = out.replace("{{contractNo}}", nz(c != null ? c.getContractNo() : ""));
        out = out.replace("{{customerName}}", nz(c != null ? c.getCustomerName() : ""));
        out = out.replace("{{customerEmail}}", nz(c != null ? c.getCustomerEmail() : ""));
        out = out.replace("{{installmentNo}}", inst != null && inst.getInstallmentNo() != null
                ? String.valueOf(inst.getInstallmentNo()) : "");
        out = out.replace("{{installmentCount}}", c != null && c.getInstallmentCount() != null
                ? String.valueOf(c.getInstallmentCount()) : "");
        out = out.replace("{{amount}}", fmt(inst != null ? inst.getAmount() : null));
        out = out.replace("{{currency}}", nz(c != null ? c.getCurrencyCode() : ""));
        out = out.replace("{{dueDate}}", inst != null && inst.getDueDateAdjusted() != null
                ? inst.getDueDateAdjusted().toString() : "");
        out = out.replace("{{orderNo}}", nz(inst != null ? inst.getOrderNo() : ""));
        out = out.replace("{{payUrl}}", nz(payUrl));
        return out;
    }

    private static String nz(String v) {
        return v != null ? v : "";
    }

    private static String fmt(BigDecimal v) {
        return v != null ? v.stripTrailingZeros().toPlainString() : "";
    }
}
