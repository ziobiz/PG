package com.pg.receipt;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 영수증 샘플 HTML 생성 (JPN 선택 시 영어ㅣ일본어). */
class TransactionReceiptSampleRenderTest {

    @Test
    void writeJapaneseFailedReceiptSample() throws Exception {
        String lang = "JPN";
        var vm = TransactionReceiptHtmlRenderer.TransactionReceiptViewModel.builder()
                .lang(lang)
                .currency("JPY")
                .amount(new BigDecimal("1000"))
                .chargeCurrency("THB")
                .chargeAmount(new BigDecimal("222.00"))
                .acquirer("")
                .paymentSwitcher("Ontheline International Co,. Ltd.\ntk@onthelinem.com")
                .paymentProvider("Ontheline Japan Co., Ltd.\n+81 03-6667-0945 / info@onthelinejp.com")
                .merchant("KOBA KIKAKU Co., Ltd.\n+81 358296016 / rt3216@gmail.com")
                .transactionId("AEF8F09A19009E478099758C74589784")
                .customerEmail("di****@gmail.com")
                .customerTel("09*****1930")
                .serviceItem("LCMWZ1K65J7A83AA")
                .orderNumber("LCMWZ1K65J7A83AA")
                .cardholder("Ki**********a")
                .authorizedDateTime("2026-11-27 21:25:00")
                .approvalCode("168837153022")
                .paymentMethod("CARD")
                .outcomeKind(TransactionReceiptOutcome.FAILED)
                .build();
        String html = TransactionReceiptHtmlRenderer.render(vm);
        String subject = TransactionReceiptEmailI18n.subject(lang, TransactionReceiptOutcome.FAILED);
        assertTrue(html.contains("Payment failedㅣ決済失敗") || html.contains("Payment failed"));
        assertTrue(subject.contains("Payment failedㅣ決済失敗"));
        Path out = Path.of("build", "receipt-sample-jpn-failed.html");
        Files.createDirectories(out.getParent());
        String wrapped = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>"
                + subject + "</title></head><body>"
                + "<p style=\"font-family:sans-serif;color:#666\">Subject: " + subject + "</p>"
                + html + "</body></html>";
        Files.writeString(out, wrapped, StandardCharsets.UTF_8);
        Path mirror = Path.of("..", "tmp", "receipt-sample-jpn-failed.html").normalize();
        Files.createDirectories(mirror.getParent());
        Files.writeString(mirror, wrapped, StandardCharsets.UTF_8);
    }
}
