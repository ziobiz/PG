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
    void displayFxShowsChargeCurrencyBesideDisplayAmount() {
        var vm = TransactionReceiptHtmlRenderer.TransactionReceiptViewModel.builder()
                .lang("KOR")
                .currency("JPY")
                .amount(new BigDecimal("90"))
                .chargeCurrency("THB")
                .chargeAmount(new BigDecimal("20.00000000"))
                .merchant("Sample Merchant")
                .orderNumber("ORD-1")
                .outcomeKind(TransactionReceiptOutcome.PAID)
                .build();
        String html = TransactionReceiptHtmlRenderer.render(vm);
        assertTrue(html.contains("JPY 90"));
        assertTrue(html.contains("#2563eb"));
        assertTrue(html.contains("결제통화"));
        assertTrue(html.contains("#111827"));
        assertTrue(html.contains("THB 20"));
        assertTrue(html.contains("#b91c1c"));
        String plain = TransactionReceiptHtmlRenderer.renderPlainText(vm);
        assertTrue(plain.contains("JPY 90"));
        assertTrue(plain.contains("결제통화 THB 20"));
    }

    @Test
    void sameCurrencyKeepsSingleAmountLine() {
        var vm = TransactionReceiptHtmlRenderer.TransactionReceiptViewModel.builder()
                .lang("KOR")
                .currency("THB")
                .amount(new BigDecimal("20"))
                .chargeCurrency("THB")
                .chargeAmount(new BigDecimal("20"))
                .outcomeKind(TransactionReceiptOutcome.PAID)
                .build();
        String html = TransactionReceiptHtmlRenderer.render(vm);
        assertTrue(html.contains("THB 20"));
        assertTrue(!html.contains("결제통화"));
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
