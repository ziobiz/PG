ICOPAY Unified Checkout — JSON (REST) Samples
==============================================

Use with the deployment kit JSON key integrationModes.json.

Files
-----
unified-prepare-request.json          — sample POST /merchant/checkout/prepare body
unified-prepare-response.example.json — live prepare success (payUrl / embed)
unified-prepare-sandbox-response.example.json — sandbox prepare (no payUrl; POST complete)

Call
----
POST {publicApiBaseUrl}/api/middleware/v1/merchant/checkout/prepare
Header: Content-Type: application/json
        Accept: application/json
        X-Icopay-Merchant-Broker-Secret: {brokerSecret}  (sandbox secret for sandbox tests)

buyer.email, buyer.phone, and buyer.countryIso2 are required.

Live: data.sessionToken → /v1/embed-checkout/{compId}
Sandbox: data.sandbox===true → POST .../merchant/checkout/complete (never open payUrl)

Status query
------------
GET {publicApiBaseUrl}/api/middleware/v1/merchant/checkout/status?compId=&orderNo=

For PHP integration see ../php/checkout_unified.php.
