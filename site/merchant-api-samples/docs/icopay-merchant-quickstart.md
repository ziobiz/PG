# ICOPAY Merchant Quick Start

| Item | Detail |
|------|--------|
| **Audience** | Merchant backend / frontend developers |
| **Brand** | **ICOPAY only.** Operational PG names are never exposed in API, docs, UI, or URLs. |
| **Base URL** | Kit `publicApiBaseUrl` (e.g. `https://api.icopay.co.kr`) |

**Official delivery:** After ICOPAY HQ deploys API for your merchant, log in → **Company management → Merchant API**. That screen is the live integration kit (keys, endpoints, parameter tables, samples). Separate email attachments are not required.

Implement **these 3 steps only**. Do not use legacy or PG-specific URLs.

---

## 1. Prepare (merchant server → ICOPAY)

```http
POST {BASE}/api/middleware/v1/merchant/checkout/prepare
Content-Type: application/json
X-Icopay-Merchant-Broker-Secret: {brokerSecret}
```

```json
{
  "compId": "M000123",
  "orderNo": "ORD-20260715001",
  "amount": 10000,
  "currency": "USD",
  "productName": "Sample product",
  "lang": "ENG",
  "buyer": {
    "email": "buyer@example.com",
    "phone": "1012345678",
    "countryIso2": "KR"
  }
}
```

**Required:** `compId` (or merchantId), `orderNo`, `amount`, `buyer.email`, `buyer.phone`, `buyer.countryIso2`

**Use from response:** `data.sessionToken`, `data.payUrl`, `data.embedScriptUrl`  
**Response `pgVendor`:** always `ICOPAY`

---

## 2. Show checkout (browser)

### A — Embed script (recommended)

Pass only `sessionToken` to the browser:

```html
<div id="icopay-checkout"></div>
<script src="{BASE}/v1/embed-checkout/{compId}"
  data-session-token="{sessionToken}"
  data-target="icopay-checkout"
  data-lang="ENG"
  async defer charset="utf-8"></script>
```

### B — payUrl iframe

```html
<iframe src="{data.payUrl}"
  title="ICOPAY checkout"
  style="width:100%;min-height:640px;border:0;"
  allow="payment *"></iframe>
```

`payUrl` is always `/checkout/{compId}`. Do not open other HTML paths directly.

---

## 3. Confirm payment (merchant server)

```http
GET {BASE}/api/middleware/v1/merchant/checkout/status?compId={compId}&orderNo={orderNo}
X-Icopay-Merchant-Broker-Secret: {brokerSecret}
```

Or receive PAID via the merchant Webhook URL registered at HQ.

Final confirmation must be **Status API or Webhook** on the server.

---

## PHP samples

| File | Use |
|------|-----|
| `merchant-api-samples/php/icopay_config.example.php` | compId, broker secret, Base URL |
| `merchant-api-samples/php/IcopayMerchantApi.php` | Client |
| `merchant-api-samples/php/checkout_unified.php` | prepare → embed example |

---

## Do not

| Forbidden | Why |
|-----------|-----|
| Put operational PG names in docs/UI/source | Merchants see ICOPAY only |
| Call `/jpay-pay.html`, `/pay/`, etc. directly | Use `payUrl` / embed only |
| Put browser return URL in prepare body | Browser return is ICOPAY NOTI → merchant Result |
| Expose broker secret in the browser | Secret stays on merchant server only |

---

## Optional: redirect / subscription

- Redirect: `POST …/merchant/checkout/redirect/prepare`
- Subscription: `POST …/merchant/checkout/subscription/prepare`

Parameter tables: Merchant API portal after login, or `merchant-api-samples/docs/unified-checkout-api-parameters*.html`.

---

## Support

Broker secret, Webhook registration, and API deployment: **ICOPAY HQ**. After deploy, use **Merchant API** in the admin UI.
