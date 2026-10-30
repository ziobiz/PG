package com.pg.controller;

import com.pg.service.WebPayQrBrandViewService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Optional;

/**
 * 웹용 QR 열기 — 상단 가맹명·하단 Provided by ICOPAY HTML (설정에 따라 숨김).
 */
@Controller
public class UrlPayQrViewController {

    private final WebPayQrBrandViewService brandViewService;

    public UrlPayQrViewController(WebPayQrBrandViewService brandViewService) {
        this.brandViewService = brandViewService;
    }

    @GetMapping(value = "/url-pay-qr/{compId}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String view(@PathVariable("compId") String compId) {
        Optional<Map<String, Object>> opt = brandViewService.resolvePublicView(compId);
        if (opt.isEmpty()) {
            return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>QR</title></head>"
                    + "<body style=\"margin:0;background:#111;color:#eee;font-family:sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh\">"
                    + "<p>QR unavailable</p></body></html>";
        }
        Map<String, Object> v = opt.get();
        String code = esc(String.valueOf(v.get("compId")));
        boolean showTitle = Boolean.TRUE.equals(v.get("showTitle"));
        boolean showFooter = Boolean.TRUE.equals(v.get("showFooter"));
        String title = showTitle ? esc(String.valueOf(v.get("titleText"))) : "";
        String footer = showFooter ? esc(String.valueOf(v.get("footerText"))) : "";
        String png = "/api/pub/url-pay/pay-qr?m=" + enc(code) + "&size=280";
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>").append(showTitle ? title : "Payment QR").append("</title>")
                .append("<style>")
                .append("html,body{margin:0;padding:0;min-height:100%;background:#0b0b0b;color:#f5f5f5;")
                .append("font-family:system-ui,-apple-system,'Segoe UI',Roboto,'Noto Sans KR',sans-serif;}")
                .append(".wrap{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px 12px;box-sizing:border-box;}")
                .append(".card{width:280px;max-width:100%;text-align:center;}")
                .append(".title{font-size:15px;font-weight:600;line-height:1.35;margin:0 0 12px;word-break:break-word;}")
                .append(".qr{display:block;width:280px;max-width:100%;height:auto;margin:0 auto;border-radius:8px;background:#fff;}")
                .append(".foot{font-size:12px;opacity:.85;line-height:1.35;margin:12px 0 0;word-break:break-word;}")
                .append("</style></head><body><div class=\"wrap\"><div class=\"card\">");
        if (showTitle) {
            sb.append("<div class=\"title\">").append(title).append("</div>");
        }
        sb.append("<img class=\"qr\" width=\"280\" height=\"280\" alt=\"QR\" src=\"").append(escAttr(png)).append("\">");
        if (showFooter) {
            sb.append("<div class=\"foot\">").append(footer).append("</div>");
        }
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escAttr(String s) {
        return esc(s);
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
