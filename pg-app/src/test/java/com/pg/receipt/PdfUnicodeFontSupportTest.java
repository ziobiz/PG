package com.pg.receipt;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PDF 다국어 폰트 임베드 스모크 — 일본어·한글이 # 로 깨지지 않는지 확인 */
class PdfUnicodeFontSupportTest {

    @Test
    void embedsCjkGlyphs() throws Exception {
                String style = "<style type=\"text/css\">"
                        + "@page{size:A4 portrait;margin:8mm;}"
                        + "body,body *{font-family:'GoNotoKurrent',sans-serif !important;text-align:left !important;}"
                        + ".pg-receipt-amount{text-align:center !important;}"
                        + "</style>";
                String html = """
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><meta charset="UTF-8"/>%s</head>
                <body>
                  <div class="pg-receipt-amount">JPY 1000</div>
                  <div>お支払い完了</div>
                  <div>アクワイアラ ElementPay Inc</div>
                  <div>決済スイッチャー</div>
                  <div>결제대행사</div>
                  <div>收单行</div>
                </body>
                </html>
                """.formatted(style);
        byte[] pdf;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            PdfUnicodeFontSupport.register(b);
            b.withHtmlContent(html, null);
            b.toStream(os);
            b.run();
            pdf = os.toByteArray();
        }
        assertTrue(pdf.length > 1000);
        try (PDDocument doc = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("ElementPay"), text);
            assertTrue(text.contains("お支払い") || text.contains("完了"), "JP missing: " + text);
            assertFalse(text.contains("######"), text);
        }
    }
}
