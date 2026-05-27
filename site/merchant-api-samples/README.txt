ICOPAY 가맹점 연동 샘플 (PHP / JSP)
====================================

다운로드 베이스: {publicApiBaseUrl}/merchant-api-samples/

PHP
---
1. php/icopay_config.example.php -> icopay_config.php (document root 밖 권장)
2. php/IcopayMerchantApi.php - API 클라이언트
3. php/checkout_chillpay.php | checkout_jpay.php - 결제 페이지 예제
4. php/notify_webhook.php - 웹훅 스텁
5. common/icopay-checkout.js - postMessage 수신

JSP (Java 11+)
--------------
1. jsp/icopay-config.example.properties -> WEB-INF/classes/icopay-config.properties
2. jsp/IcopayMerchantApi.sample.java -> com.icopay.merchant.IcopayMerchantApi 컴파일
3. jsp/checkout-chillpay.jsp | checkout-jpay.jsp
4. jsp/notify-webhook.jsp

연동 순서
---------
주문 생성 -> 서버 prepare(브로커 시크릿) -> sessionToken -> embed 스크립트 -> postMessage/웹훅

키트 JSON 의 merchantIntegrationSamples, merchantInlineCheckoutChillPay / merchantInlineCheckoutJpay 참고.
배포·검수 문서:
  ChillPay — docs/ChillPay_URL결제_인라인API_배포가이드.md
  JPAY     — docs/JPAY_URL결제_인라인API_배포가이드.md

WooCommerce
-----------
WordPress/WooCommerce 가맹: 저장소 woocommerce/icopay-woocommerce/ 플러그인 ZIP 배포.
(compId·broker secret·API URL 설정 → HQ 웹훅 URL 등록)

인코딩: UTF-8. 브라우저에서 한글이 깨지면 새로고침(Ctrl+F5) 또는 파일을 저장 후 메모장에서 UTF-8로 열어 주세요.
