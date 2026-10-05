package com.pg.receipt;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * openhtmltopdf PDF 변환용 유니코드 폰트(한글·일어·중국어·태국어·라틴).
 * 폰트 바이트는 classpath에서 1회 로드·캐시한다(요청마다 30MB 읽기 방지).
 */
public final class PdfUnicodeFontSupport {

    public static final String FAMILY = "GoNotoKurrent";

    private static final String REGULAR = "/fonts/GoNotoKurrent-Regular.ttf";
    private static final String BOLD = "/fonts/GoNotoKurrent-Bold.ttf";

    private static final ConcurrentHashMap<String, byte[]> FONT_BYTES = new ConcurrentHashMap<>();

    private PdfUnicodeFontSupport() {
    }

    public static void register(PdfRendererBuilder builder) {
        if (builder == null) {
            return;
        }
        registerOne(builder, REGULAR, 400);
        registerOne(builder, BOLD, 700);
    }

    /**
     * PDF 직전: 유니코드 폰트 + 미리보기와 같은 왼쪽 정렬 + A4 한 장용 압축 스타일.
     * (브라우저 미리보기 HTML은 건드리지 않음)
     */
    public static String prepareHtmlForPdf(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        String style = """
                <style type="text/css">
                @page { size: A4 portrait; margin: 10mm; }
                html, body {
                  margin: 0 !important;
                  padding: 0 !important;
                  background: #ffffff !important;
                }
                body, body td, body div, body span, body pre, body strong, body b {
                  font-family: '%s', sans-serif !important;
                }
                .pg-receipt-outer {
                  width: 100%% !important;
                  padding: 8px 0 !important;
                  background: #ffffff !important;
                }
                .pg-receipt-outer-cell {
                  text-align: center !important;
                }
                .pg-receipt-card {
                  width: 70%% !important;
                  max-width: 364px !important;
                  margin-left: auto !important;
                  margin-right: auto !important;
                  border-radius: 8px !important;
                  text-align: left !important;
                }
                .pg-receipt-section-title,
                .pg-receipt-row-label,
                .pg-receipt-row-value,
                .pg-receipt-pad,
                .pg-receipt-cell {
                  text-align: left !important;
                }
                /* 상단: ICOPAY 왼쪽 · 배지 오른쪽 · 금액 가운데 (미리보기와 동일) */
                .pg-receipt-header-row {
                  width: 100%% !important;
                }
                .pg-receipt-card td.pg-receipt-brand {
                  width: 50%% !important;
                  text-align: left !important;
                  vertical-align: middle !important;
                }
                .pg-receipt-card td.pg-receipt-badge {
                  width: 50%% !important;
                  text-align: right !important;
                  vertical-align: middle !important;
                }
                .pg-receipt-card td.pg-receipt-amount,
                .pg-receipt-card div.pg-receipt-amount-value,
                .pg-receipt-amount,
                .pg-receipt-amount-value {
                  text-align: center !important;
                }
                .pg-receipt-amount-value {
                  font-size: 22px !important;
                  font-weight: 700 !important;
                }
                .pg-receipt-amount {
                  padding-top: 4px !important;
                  padding-bottom: 10px !important;
                }
                .pg-receipt-section-title {
                  font-size: 9px !important;
                  margin-bottom: 6px !important;
                }
                .pg-receipt-row-label {
                  font-size: 8px !important;
                  margin-bottom: 1px !important;
                }
                .pg-receipt-row-value {
                  font-size: 10px !important;
                  line-height: 1.35 !important;
                }
                .pg-receipt-pad {
                  padding: 8px 12px !important;
                }
                .pg-receipt-cell {
                  padding: 6px 10px !important;
                }
                .pg-receipt-footer {
                  font-size: 9px !important;
                  padding: 8px 12px !important;
                  text-align: center !important;
                }
                .pg-receipt-brand {
                  font-size: 11px !important;
                }
                .pg-receipt-badge-pill {
                  font-size: 9px !important;
                  padding: 2px 8px !important;
                  display: inline-block !important;
                }
                </style>
                """.formatted(FAMILY);
        String lower = html.toLowerCase(Locale.ROOT);
        int headClose = lower.indexOf("</head>");
        if (headClose >= 0) {
            return html.substring(0, headClose) + style + html.substring(headClose);
        }
        int bodyOpen = lower.indexOf("<body");
        if (bodyOpen >= 0) {
            int gt = html.indexOf('>', bodyOpen);
            if (gt >= 0) {
                return html.substring(0, gt + 1) + style + html.substring(gt + 1);
            }
        }
        return style + html;
    }

    /** @deprecated use {@link #prepareHtmlForPdf(String)} */
    @Deprecated
    public static String injectFontCss(String html) {
        return prepareHtmlForPdf(html);
    }

    private static void registerOne(PdfRendererBuilder builder, String classpath, int weight) {
        byte[] bytes = loadFontBytes(classpath);
        builder.useFont(
                () -> new ByteArrayInputStream(bytes),
                FAMILY,
                weight,
                PdfRendererBuilder.FontStyle.NORMAL,
                true);
    }

    private static byte[] loadFontBytes(String classpath) {
        return FONT_BYTES.computeIfAbsent(classpath, key -> {
            try (InputStream in = PdfUnicodeFontSupport.class.getResourceAsStream(key)) {
                if (in == null) {
                    throw new IllegalStateException("PDF font missing on classpath: " + key);
                }
                return in.readAllBytes();
            } catch (IOException ex) {
                throw new IllegalStateException("PDF font read failed: " + key, ex);
            }
        });
    }
}
