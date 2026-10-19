package com.pg.receipt;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionReceiptHtmlRendererTest {

    @Test
    void refundHtmlUsesRefundBadgeAndIcopayOnly() {
        var vm = sample(TransactionReceiptOutcome.REFUNDED, "KOR");
        String html = TransactionReceiptHtmlRenderer.render(vm);
        assertTrue(html.contains("환불 완료"));
        assertTrue(html.contains("#dbeafe"));
        assertTrue(html.contains("ICOPAY"));
        assertTrue(!html.toLowerCase().contains("elementpay"));
        assertTrue(!html.toLowerCase().contains("jpay"));
        String plain = TransactionReceiptHtmlRenderer.renderPlainText(vm);
        assertTrue(plain.contains("환불 완료"));
        assertTrue(plain.contains("환불일시"));
    }

    @Test
    void failedHtmlUsesFailedBadgeNotPaid() {
        var vm = sample(TransactionReceiptOutcome.FAILED, "KOR");
        String html = TransactionReceiptHtmlRenderer.render(vm);
        assertTrue(html.contains("결제실패"));
        assertTrue(html.contains("#fee2e2"));
        assertTrue(!html.contains("결제 완료") && !html.contains("결제완료"));
        assertTrue(html.contains("실패일시"));
        String plain = TransactionReceiptHtmlRenderer.renderPlainText(vm);
        assertTrue(plain.contains("결제실패"));
        assertTrue(!plain.contains("결제 완료") && !plain.contains("결제완료"));
    }

    @Test
    void voidHtmlUsesVoidBadgeInEnglish() {
        var vm = sample(TransactionReceiptOutcome.VOIDED, "ENG");
        String html = TransactionReceiptHtmlRenderer.render(vm);
        assertTrue(html.contains("VOID COMPLETED"));
        assertTrue(html.contains("#ffedd5"));
        String plain = TransactionReceiptHtmlRenderer.renderPlainText(vm);
        assertTrue(plain.contains("Voided Date/Time"));
    }

    private static TransactionReceiptHtmlRenderer.TransactionReceiptViewModel sample(
            TransactionReceiptOutcome outcome, String lang) {
        return TransactionReceiptHtmlRenderer.TransactionReceiptViewModel.builder()
                .lang(lang)
                .currency("THB")
                .amount(new BigDecimal("100.00"))
                .merchant("Sample Merchant")
                .transactionId("TXN-1")
                .customerEmail("buyer@example.com")
                .customerTel("+66812345678")
                .serviceItem("Item")
                .orderNumber("ORD-1")
                .cardholder("Hong Gildong")
                .authorizedDateTime("2026-08-25 12:00:00")
                .approvalCode("OK")
                .paymentMethod("CARD")
                .outcomeKind(outcome)
                .build();
    }
}
