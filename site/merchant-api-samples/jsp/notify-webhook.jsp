<%@ page contentType="text/plain;charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ICOPAY → 가맹점 통보(웹훅) 수신 JSP 스텁. 멱등 처리 후 항상 200 + OK.
--%>
<%
    java.io.InputStream in = request.getInputStream();
    byte[] buf = in.readAllBytes();
    String raw = new String(buf, java.nio.charset.StandardCharsets.UTF_8);
    application.log("[ICOPAY notify] len=" + buf.length + " body=" + raw);
    // TODO: 파싱 → orderNo·승인여부 → 가맹 DB 갱신(멱등)
    response.setStatus(200);
%>OK
