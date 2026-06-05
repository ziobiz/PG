ICOPAY Unified Checkout — JSON (REST) Samples
==============================================

Use with the deployment kit JSON key integrationModes.json.

Files
-----
unified-prepare-request.json          — sample POST /merchant/checkout/prepare body
unified-prepare-response.example.json — sample prepare success response (field reference)

Call
----
POST {publicApiBaseUrl}/api/middleware/v1/merchant/checkout/prepare
Header: Content-Type: application/json
        Accept: application/json
        X-Icopay-Merchant-Broker-Secret: {brokerSecret}  (when enforce is on)

buyer.email, buyer.phone, and buyer.countryIso2 are required.

Response data.sessionToken → pass only /v1/embed-checkout/{compId} script to the browser.

Status query
------------
GET {publicApiBaseUrl}/api/middleware/v1/merchant/checkout/status?compId=&orderNo=

For PHP integration see ../php/checkout_unified.php.
