<%@ page contentType="text/plain;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ICOPAY → merchant server webhook (JSP).
  When signing is ON: header X-Icopay-Signature: v1={HMAC-SHA256 hex of raw body}.
  Set context-param / env ICOPAY_WEBHOOK_SIGN_SECRET. Browser Result URL is separate.
--%>
<%
    java.io.InputStream in = request.getInputStream();
    byte[] buf = in.readAllBytes();
    String raw = new String(buf, java.nio.charset.StandardCharsets.UTF_8);
    String sigHeader = request.getHeader("X-Icopay-Signature");
    if (sigHeader == null) sigHeader = "";
    String secret = System.getenv("ICOPAY_WEBHOOK_SIGN_SECRET");
    if (secret == null) secret = "";
    secret = secret.trim();
    if (!secret.isEmpty()) {
        boolean ok = false;
        if (sigHeader.matches("(?i)^v1=[0-9a-f]+$")) {
            String hex = sigHeader.substring(3).trim();
            try {
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                mac.init(new javax.crypto.spec.SecretKeySpec(
                        secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] dig = mac.doFinal(buf);
                StringBuilder sb = new StringBuilder(dig.length * 2);
                for (byte b : dig) sb.append(String.format("%02x", b));
                ok = sb.toString().equalsIgnoreCase(hex);
            } catch (Exception e) {
                ok = false;
            }
        }
        if (!ok) {
            response.setStatus(401);
%>INVALID_SIGNATURE<%
            return;
        }
    }
    application.log("[ICOPAY notify] sig=" + (!sigHeader.isEmpty()) + " len=" + buf.length);
    // TODO: parse JSON → orderNo + paymentStatus → idempotent DB update
    response.setStatus(200);
%>OK
