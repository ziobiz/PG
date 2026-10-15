ICOPAY Merchant Integration Samples
===================================

Download base: {publicApiBaseUrl}/merchant-api-samples/

Brand: ICOPAY only. Payment-agency / operational PG names are never shown to merchants.

Recommended path (3 steps)
--------------------------
1) Merchant server: POST /api/middleware/v1/merchant/checkout/prepare
2) Live: browser /v1/embed-checkout/{compId} (or iframe data.payUrl)
   Sandbox secret: data.sandbox=true → POST .../checkout/complete (no payUrl/card UI)
3) Merchant server: GET .../checkout/status and/or merchant webhook
   Sandbox rows: Payments → Sandbox history (not the live payment list)

Quick start (Korean): docs/../../docs/가맹점_ICOPAY_간단연동_빠른시작.md
  (repo path) also mirrored in merchant kit docs portal.

① JSON — direct REST
---------------------
   docs/unified-checkout-api-flow.html (+ .ko .ja .ch .th)
   docs/unified-checkout-api-parameters.html (+ langs)
   docs/crypto-checkout-api.html (+ .ko .ja .ch .th)  ← payMeans=CRYPTO, Callback reuse
   json/unified-prepare-request.json
   json/crypto-prepare-request.json            ← crypto prepare sample
   json/unified-prepare-response.example.json   ← live: pgVendor=ICOPAY, payUrl=/checkout/...
   json/unified-prepare-sandbox-response.example.json  ← sandbox=true, call complete

Crypto: same prepare URL with "payMeans":"CRYPTO". Callback = existing card NOTI callbackUrl (Comp-Id).

② PHP — recommended
-------------------
   php/icopay_config.example.php → icopay_config.php
   php/IcopayMerchantApi.php
   php/checkout_unified.php          ← USE THIS
   php/notify_webhook.php
   common/icopay-checkout.js

Do not use for new integrations
-------------------------------
   php/checkout_*.php legacy vendor-specific samples (compat only)
   jsp/checkout-*.jsp legacy samples (compat only)

Encoding: UTF-8
