<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String orderNo = request.getParameter("orderNo") != null ? request.getParameter("orderNo").trim() : "";
%>
<!DOCTYPE html>
<html lang="ko"><head><meta charset="UTF-8"><title>주문 완료</title></head>
<body>
  <h1>결제 완료 (JSP 예제)</h1>
  <p>주문번호: <%= orderNo %></p>
</body></html>
