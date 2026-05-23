ICOPAY 가맹점 인라인 결제 API — PHP / JSP 연동 샘플
====================================================

이 폴더의 파일은 가맹점(또는 용역사) 개발 PC로 **복사**해 사용하는 참고 코드입니다.
ICOPAY 서버(JAR) 위에서 PHP/JSP가 실행되는 것이 아닙니다.

연동 흐름 (ChillPay · JPAY 공통)
--------------------------------
1. 가맹점 주문 DB에 주문 저장
2. 가맹점 **서버**(PHP/JSP)에서 ICOPAY prepare API 호출 → sessionToken 수신
3. 가맹점 **결제 페이지** HTML에 embed 스크립트 삽입 (data-session-token)
4. 고객이 ICOPAY iframe 에서 결제 (ChillPay CCD / JPAY 카드·3DS)
5. postMessage 또는 본사 웹훹(notify) + status API 로 결과 확정

설정
----
- php/config.sample.php → config.php 로 복사 후 compId, brokerSecret, apiBaseUrl 입력
- jsp/icopay-config.properties.sample → WEB-INF/classes/icopay-config.properties

샘플 파일
---------
php/
  IcopayMerchantApiClient.php   공통 HTTP 클라이언트
  checkout_chillpay.php         ChillPay 결제 페이지 예
  checkout_jpay.php             JPAY 결제 페이지 예
  pay_status.php                주문 상태 조회 예
  notify_webhook.php            통보 URL 스텁 예

jsp/
  checkout_chillpay.jsp
  checkout_jpay.jsp
  pay_status.jsp
  notify_webhook.jsp

java/
  IcopayMerchantApiClient.java  JSP/WAS 에 클래스로 넣을 때 참고

키트 JSON 의 integrationSamples 경로와 동일합니다.
