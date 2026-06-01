ICOPAY 가맹점 연동 샘플
======================

다운로드 베이스: {publicApiBaseUrl}/merchant-api-samples/

연동 방식 (API 배포 키트 integrationModes)
------------------------------------------
① JSON — REST 직접 호출 (Java·Node·Python 등)
   docs/unified-checkout-api-parameters.html  — Prepare API 파라미터 표 (표 1.1·1.2)
   json/README.txt
   json/unified-prepare-request.json
   json/unified-prepare-response.example.json

② PHP — IcopayMerchantApi.php + checkout 페이지
   php/icopay_config.example.php -> icopay_config.php
   php/IcopayMerchantApi.php
   php/checkout_unified.php (통합 checkout, 권장)
   php/checkout_chillpay.php | checkout_jpay.php (레거시)
   php/notify_webhook.php
   common/icopay-checkout.js

JSP (Java 11+)
--------------
jsp/icopay-config.example.properties
jsp/IcopayMerchantApi.sample.java
jsp/checkout-chillpay.jsp | checkout-jpay.jsp
jsp/notify-webhook.jsp

연동 순서
---------
주문 생성 -> 서버 prepare (JSON 또는 PHP) -> sessionToken -> embed -> postMessage/웹훅

키트 JSON: integrationModes.json / integrationModes.php / merchantUnifiedCheckout
배포·검수 문서:
  docs/가맹점_PG_API_연동가이드.md
  ChillPay — docs/ChillPay_URL결제_인라인API_배포가이드.md
  JPAY     — docs/JPAY_URL결제_인라인API_배포가이드.md

WooCommerce
-----------
WordPress/WooCommerce: woocommerce/icopay-woocommerce/

인코딩: UTF-8
