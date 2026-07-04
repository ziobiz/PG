package com.icopay.merchant;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ICOPAY 가맹점 인라인 결제 API 클라이언트 (Java 11+ / JSP·Spring 공용, 외부 JSON 라이브러리 불필요).
 * 본 파일을 프로젝트 src 에 복사하고 package 를 맞춘 뒤 컴파일하세요.
 */
public class IcopayMerchantApi {

    public static final String HEADER_BROKER_SECRET = "X-Icopay-Merchant-Broker-Secret";
    public static final String VENDOR_CHILLPAY = "chillpay";
    public static final String VENDOR_JPAY = "jpay";

    private final String apiBase;
    private final String compId;
    private final String brokerSecret;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public IcopayMerchantApi(String apiBase, String compId, String brokerSecret) {
        this.apiBase = trimSlash(apiBase);
        this.compId = compId != null ? compId.trim() : "";
        this.brokerSecret = brokerSecret != null ? brokerSecret.trim() : "";
    }

    public static IcopayMerchantApi fromClasspathProperties() throws IOException {
        Properties p = new Properties();
        try (InputStream in = IcopayMerchantApi.class.getClassLoader().getResourceAsStream("icopay-config.properties")) {
            if (in == null) {
                throw new IOException("icopay-config.properties not found on classpath");
            }
            p.load(in);
        }
        return new IcopayMerchantApi(
                p.getProperty("icopay.apiBaseUrl", ""),
                p.getProperty("icopay.compId", ""),
                p.getProperty("icopay.brokerSecret", "")
        );
    }

    public static String normalizeLang(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String u = raw.trim().toUpperCase();
        if (Set.of("KOR", "ENG", "JPN", "CHN", "THA").contains(u)) {
            return u;
        }
        return switch (u) {
            case "KO", "KR", "KOREAN" -> "KOR";
            case "EN", "ENGLISH" -> "ENG";
            case "JA", "JP", "JPY", "JAPANESE" -> "JPN";
            case "ZH", "CN", "CH", "CHINESE" -> "CHN";
            case "TH", "THAI" -> "THA";
            default -> mapAcceptLanguagePrefix(raw.trim().toLowerCase());
        };
    }

    public static String detectPageLang(String acceptLanguageHeader) {
        return normalizeLang(acceptLanguageHeader);
    }

    private static String mapAcceptLanguagePrefix(String tag) {
        String first = tag.split(",")[0].trim();
        if (first.startsWith("ko")) return "KOR";
        if (first.startsWith("ja")) return "JPN";
        if (first.startsWith("zh")) return "CHN";
        if (first.startsWith("th")) return "THA";
        if (first.startsWith("en")) return "ENG";
        return "";
    }

    public Map<String, Object> prepareInlineCheckout(String vendor, String orderNo, String amount,
                                                     String currency, String productName, String lang)
            throws IOException, InterruptedException {
        String path = "/api/middleware/v1/merchant/checkout/prepare";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("compId", compId);
        body.put("orderNo", orderNo);
        body.put("amount", amount);
        if (currency != null && !currency.isBlank()) {
            body.put("currency", currency.trim().toUpperCase());
        }
        if (productName != null && !productName.isBlank()) {
            body.put("productName", productName.trim());
        }
        String langNorm = normalizeLang(lang != null && !lang.isBlank() ? lang : "");
        if (langNorm.isBlank() && lang == null) {
            langNorm = "";
        }
        if (!langNorm.isBlank()) {
            body.put("lang", langNorm);
        }
        return postJson(path, body);
    }

    public Map<String, Object> prepareInlineCheckout(String vendor, String orderNo, String amount,
                                                     String currency, String productName)
            throws IOException, InterruptedException {
        return prepareInlineCheckout(vendor, orderNo, amount, currency, productName, "");
    }

    public Map<String, Object> getPaymentStatus(String vendor, String orderNo)
            throws IOException, InterruptedException {
        String path = "/api/middleware/v1/merchant/checkout/status";
        String qs = "compId=" + enc(compId) + "&orderNo=" + enc(orderNo);
        return getJson(path + "?" + qs);
    }

    public String buildEmbedHtml(String vendor, String sessionToken, String targetId, String lang) {
        String embedPath = "/v1/embed-checkout/";
        String target = (targetId != null && !targetId.isBlank())
                ? targetId.trim()
                : "icopay-checkout";
        String src = apiBase + embedPath + enc(compId);
        String langNorm = normalizeLang(lang != null ? lang : "");
        String langAttr = langNorm.isBlank() ? "" : " data-lang=\"" + esc(langNorm) + "\"";
        return "<div id=\"" + esc(target) + "\"></div>\n"
                + "<script src=\"" + esc(src) + "\""
                + " data-session-token=\"" + esc(sessionToken) + "\""
                + " data-target=\"" + esc(target) + "\""
                + langAttr
                + " async defer charset=\"utf-8\"></script>";
    }

    public String buildEmbedHtml(String vendor, String sessionToken, String targetId) {
        return buildEmbedHtml(vendor, sessionToken, targetId, "");
    }

    private Map<String, Object> postJson(String path, Map<String, Object> body)
            throws IOException, InterruptedException {
        String json = toJson(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header(HEADER_BROKER_SECRET, brokerSecret)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return parseApiResponse(res.body());
    }

    private Map<String, Object> getJson(String pathWithQuery) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + pathWithQuery))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header(HEADER_BROKER_SECRET, brokerSecret)
                .GET()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return parseApiResponse(res.body());
    }

    private static Map<String, Object> parseApiResponse(String raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            out.put("success", false);
            out.put("message", "Empty response");
            return out;
        }
        boolean ok = raw.matches("(?s).*\"success\"\\s*:\\s*true.*");
        out.put("success", ok);
        if (!ok) {
            out.put("message", jsonStringField(raw, "message"));
            out.put("errorCode", jsonStringField(raw, "errorCode"));
            return out;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionToken", jsonStringField(raw, "sessionToken"));
        data.put("orderNo", jsonStringField(raw, "orderNo"));
        data.put("payUrl", jsonStringField(raw, "payUrl"));
        out.put("data", data);
        return out;
    }

    private static String jsonStringField(String raw, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(raw);
        return m.find() ? m.group(1) : null;
    }

    private static String toJson(Map<String, Object> body) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : body.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escapeJson(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append('"').append(escapeJson(String.valueOf(v))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
