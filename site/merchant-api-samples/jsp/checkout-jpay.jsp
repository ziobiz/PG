<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.icopay.merchant.IcopayMerchantApi" %>
<%@ page import="java.util.*" %>
<%
    String error = "";
    String embedHtml = "";
    String orderNo = "";
    String apiBase = "";
    IcopayMerchantApi api;
    try {
        api = IcopayMerchantApi.fromClasspathProperties();
        Properties p = new Properties();
        try (java.io.InputStream in = application.getResourceAsStream("/WEB-INF/classes/icopay-config.properties")) {
            if (in != null) {
                p.load(in);
                apiBase = p.getProperty("icopay.apiBaseUrl", "");
            }
        }
    } catch (Exception ex) {
        error = "Config error: " + ex.getMessage();
        api = null;
    }

    if (api != null && "POST".equalsIgnoreCase(request.getMethod())) {
        orderNo = trim(request.getParameter("orderNo"));
        String amount = trim(request.getParameter("amount"));
        String currency = trimOrDefault(request.getParameter("currency"), "USD");
        String productName = trimOrDefault(request.getParameter("productName"), "Product");
        if (orderNo.isEmpty() || amount.isEmpty()) {
            error = "orderNo and amount are required.";
        } else {
            try {
                String pageLang = IcopayMerchantApi.detectPageLang(request.getHeader("Accept-Language"));
                Map<String, Object> prep = api.prepareInlineCheckout(IcopayMerchantApi.VENDOR_JPAY, orderNo, amount, currency, productName, pageLang);
                if (!Boolean.TRUE.equals(prep.get("success"))) {
                    error = String.valueOf(prep.getOrDefault("message", "prepare failed"));
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) prep.get("data");
                    String token = data != null ? String.valueOf(data.get("sessionToken")) : "";
                    if (token == null || token.isBlank() || "null".equals(token)) {
                        error = "sessionToken missing";
                    } else {
                        embedHtml = api.buildEmbedHtml(IcopayMerchantApi.VENDOR_JPAY, token, null, pageLang);
                    }
                }
            } catch (Exception ex) {
                error = ex.getMessage();
            }
        }
    }
    String apiBaseJs = apiBase != null ? apiBase.replace("\\", "\\\\").replace("'", "\\'") : "";
%>
<%!
    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static String trimOrDefault(String s, String def) { String t = trim(s); return t.isEmpty() ? def : t; }
    private static String html(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>ICOPAY JPAY Checkout (JSP sample)</title>
  <style>body{font-family:system-ui,sans-serif;max-width:640px;margin:2rem auto;padding:0 1rem}.err{color:#b02a37}</style>
</head>
<body>
  <h1>JPAY 결제 (JSP 예제)</h1>
  <% if (!error.isEmpty()) { %><p class="err"><%= html(error) %></p><% } %>
  <% if (embedHtml.isEmpty()) { %>
  <form method="post">
    <p><label>주문번호 <input name="orderNo" required maxlength="64" value="JORD<%= System.currentTimeMillis() %></label></p>
    <p><label>금액 <input name="amount" required value="100"></label></p>
    <p><label>통화 <input name="currency" value="USD"></label></p>
    <p><label>상품명 <input name="productName" value="Test product"></label></p>
    <button type="submit">결제하기</button>
  </form>
  <% } else { %>
  <p>주문번호: <strong><%= html(orderNo) %></strong></p>
  <%= embedHtml %>
  <script src="<%= html(apiBase) %>/merchant-api-samples/common/icopay-checkout.js"></script>
  <script>
    IcopayCheckout.onMessage(function (detail) {
      if (detail.phase === 'finished' && detail.success) {
        location.href = 'order-complete.jsp?orderNo=' + encodeURIComponent(detail.orderNo || '');
      }
    }, '<%= apiBaseJs %>');
  </script>
  <% } %>
</body>
</html>
