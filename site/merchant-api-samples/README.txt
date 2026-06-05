ICOPAY Merchant Integration Samples
===================================

Download base: {publicApiBaseUrl}/merchant-api-samples/

Integration modes (API deployment kit integrationModes)
-----------------------------------------------------
① JSON — direct REST (Java, Node, Python, etc.)
   docs/unified-checkout-api-flow.html       — Integration flow (EN)
   docs/unified-checkout-api-flow.ko.html   — 연동 흐름 (KO)
   docs/unified-checkout-api-flow.ja.html    — 連携フロー (JA)
   docs/unified-checkout-api-flow.ch.html    — 对接流程 (CH)
   docs/unified-checkout-api-flow.th.html    — ขั้นตอนการเชื่อมต่อ (TH)
   docs/unified-checkout-api-parameters.html  — Prepare API parameter tables (EN)
   docs/unified-checkout-api-parameters.ko.html — 파라미터 표 (KO)
   docs/unified-checkout-api-parameters.ja.html — パラメータ表 (JA)
   docs/unified-checkout-api-parameters.ch.html — 参数表 (CH)
   docs/unified-checkout-api-parameters.th.html — ตารางพารามิเตอร์ (TH)
   json/README.txt
   json/unified-prepare-request.json
   json/unified-prepare-response.example.json

② PHP — IcopayMerchantApi.php + checkout pages
   php/icopay_config.example.php -> icopay_config.php
   php/IcopayMerchantApi.php
   php/checkout_unified.php (unified checkout, recommended)
   php/checkout_chillpay.php | checkout_jpay.php (legacy)
   php/notify_webhook.php
   common/icopay-checkout.js

JSP (Java 11+)
--------------
jsp/icopay-config.example.properties
jsp/IcopayMerchantApi.sample.java
jsp/checkout-chillpay.jsp | checkout-jpay.jsp
jsp/notify-webhook.jsp

Integration flow
----------------
Create order -> server prepare (JSON or PHP) -> sessionToken -> embed -> postMessage/webhook

Kit JSON: integrationModes.json / integrationModes.php / merchantUnifiedCheckout
Additional HQ deployment guides (internal repo docs/, not served here):
  Merchant PG API integration guide
  ChillPay URL / inline API deployment guide
  JPAY URL / inline API deployment guide

WooCommerce
-----------
WordPress/WooCommerce: woocommerce/icopay-woocommerce/

Encoding: UTF-8
