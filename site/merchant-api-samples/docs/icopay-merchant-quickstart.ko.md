# ICOPAY 가맹점 간단 연동 — 빠른 시작

| 항목 | 내용 |
|------|------|
| **대상** | 가맹점 백엔드·프론트 개발자 |
| **브랜드** | **ICOPAY만** 사용합니다. 결제대행사(운영 PG) 이름은 API·문서·화면·URL에 노출되지 않습니다. |
| **Base URL** | 배포 키트의 `publicApiBaseUrl` (예: `https://api.icopay.co.kr`) |

**공식 전달 경로:** 본사가 API 배포를 완료하면, 가맹점은 관리자에 로그인 후 **업체관리 → 가맹점API**에서 키·엔드포인트·파라미터 표·샘플을 확인합니다. 별도 메일·파일 첨부 배포는 필요하지 않습니다.

가맹점은 **아래 3단계만** 구현하면 됩니다. 다른 경로(레거시·PG별 URL)는 사용하지 마세요.

---

## 1. Prepare (가맹 서버 → ICOPAY)

```http
POST {BASE}/api/middleware/v1/merchant/checkout/prepare
Content-Type: application/json
X-Icopay-Merchant-Broker-Secret: {브로커시크릿}
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

**필수:** `compId`(또는 merchantId), `orderNo`, `amount`, `buyer.email`, `buyer.phone`, `buyer.countryIso2`

**응답에서 쓸 값:** `data.sessionToken`, `data.payUrl`, `data.embedScriptUrl`  
**응답 `pgVendor`:** 항상 `ICOPAY`

---

## 2. 결제창 표시 (브라우저)

### 권장 A — Embed 스크립트 (iframe)

```html
<div id="icopay-checkout"></div>
<script src="{BASE}/v1/embed-checkout/{compId}"
  data-session-token="{sessionToken}"
  data-target="icopay-checkout"
  data-lang="ENG"
  async defer charset="utf-8"></script>
```

### 권장 B — payUrl iframe

```html
<iframe src="{data.payUrl}"
  title="ICOPAY checkout"
  style="width:100%;min-height:640px;border:0;"
  allow="payment *"></iframe>
```

`payUrl`은 항상 **`/checkout/{compId}`** 형태입니다.

---

## 3. 결제 결과 확인 (가맹 서버)

```http
GET {BASE}/api/middleware/v1/merchant/checkout/status?compId={compId}&orderNo={orderNo}
X-Icopay-Merchant-Broker-Secret: {브로커시크릿}
```

또는 본사에 등록한 **가맹 Webhook URL**로 PAID 통보를 수신합니다.  
**최종 확정은 Status API 또는 Webhook**으로 하세요.

---

## PHP 샘플 (권장)

| 파일 | 용도 |
|------|------|
| `merchant-api-samples/php/icopay_config.example.php` | `compId`·브로커 시크릿·Base URL |
| `merchant-api-samples/php/IcopayMerchantApi.php` | 클라이언트 |
| `merchant-api-samples/php/checkout_unified.php` | prepare → embed 예제 |

---

## 하지 말 것

| 금지 | 이유 |
|------|------|
| PG사·결제대행사 이름을 문서·UI·소스에 표기 | 가맹에는 ICOPAY만 |
| `/jpay-pay.html`, `/pay/` 등 비중립 URL을 직접 호출 | `payUrl`·embed만 사용 |
| prepare body에 가맹 사이트 return URL을 넣음 | 브라우저 복귀는 ICOPAY NOTI → 가맹 Result |
| 브로커 시크릿을 브라우저에 노출 | 시크릿은 **가맹 서버만** |

---

## 리다이렉트·구독 (선택)

- 리다이렉트: `POST …/merchant/checkout/redirect/prepare`
- 구독: `POST …/merchant/checkout/subscription/prepare`

상세 표: **가맹점API** 화면 또는 `merchant-api-samples/docs/unified-checkout-api-parameters*.html`

---

## 문의

브로커 시크릿·Webhook 등록·API 배포는 **본사(ICOPAY)** 에 요청하세요. 배포 후에는 관리자 **가맹점API** 메뉴를 사용하세요.
