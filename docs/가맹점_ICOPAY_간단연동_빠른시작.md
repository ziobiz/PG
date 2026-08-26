# ICOPAY 가맹점 간단 연동 — 빠른 시작

| 항목 | 내용 |
|------|------|
| **대상** | 가맹점 백엔드·프론트 개발자 |
| **브랜드** | **ICOPAY만** 사용합니다. 결제대행사(운영 PG) 이름은 API·문서·화면·URL에 노출되지 않습니다. |
| **Base URL** | 배포 키트의 `publicApiBaseUrl` (예: `https://api.icopay.co.kr`) |
| **공식 전달** | 가맹점 **로그인 → 업체관리 → 가맹점API**. 별도 메일·파일 배포 불필요 |

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

**필수 (빼면 prepare가 ICOPAY `BUYER_*` 로 실패합니다. 빈 문자열도 누락입니다):**
- `compId`(또는 `merchantId`)
- `orderNo`
- `amount`
- `buyer.email`
- `buyer.phone` — 로컬 번호만. 국가번호 `+82` 등은 **제거**
- `buyer.countryIso2` — 대문자 2자 (예: `KR`, `TH`, `JP`)

필드별 오류 코드: `BUYER_EMAIL_REQUIRED` · `BUYER_PHONE_REQUIRED` · `BUYER_COUNTRY_REQUIRED`  
응답 `messages` 는 KOR/ENG/JPN/CHN/THA 입니다. 결제대행사 영문 오류가 아닙니다.

**응답에서 쓸 값:** `data.sessionToken`, `data.payUrl`, `data.embedScriptUrl`  
**응답 `pgVendor`:** 항상 `ICOPAY` (그 외 벤더명·운영 PG 코드는 오지 않음)

---

## 2. 결제창 표시 (브라우저)

### 권장 A — Embed 스크립트 (iframe)

가맹 서버에서 `sessionToken`만 브라우저에 전달한 뒤:

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

`payUrl`은 항상 **`/checkout/{compId}`** 형태입니다. 다른 HTML 경로를 직접 열지 마세요.

카드 입력·3DS·언어·상단 안내 문구는 ICOPAY 결제창이 처리합니다.

---

## 3. 결제 결과 확인 (가맹 서버)

```http
GET {BASE}/api/middleware/v1/merchant/checkout/status?compId={compId}&orderNo={orderNo}
X-Icopay-Merchant-Broker-Secret: {브로커시크릿}
```

또는 본사에 등록한 **가맹 Webhook URL**로 PAID 통보를 수신합니다.

브라우저 `postMessage` 이벤트: `ICOPAY_INLINE_CHECKOUT` (참고용). **최종 확정은 Status API 또는 Webhook**으로 하세요.

---

## 4. 취소·환불

가맹 Checkout API에는 **취소·환불 요청 엔드포인트가 없습니다.** 승인 건의 취소·환불은 **ICOPAY 관리자 결제내역**에서 자동환불·강제환불로 요청합니다. (당일 무효 API는 해당 결제 방식에 없습니다.)

| 경로 | 동작 |
|------|------|
| ICOPAY 결제내역 → 자동환불·강제환불 | ICOPAY가 결제망에 환불을 요청하고 결과를 반영 |
| 결제망 캐비닛에서 환불 | 노티가 ICOPAY로 오면 거래 상태를 환불로 맞춤 |
| 가맹 확인 | 등록한 **Webhook** 통보 및 `GET …/checkout/status` 의 `paymentStatus` (`PAID` · `REFUNDED` · `CHARGEBACK` 등) |

부분 환불이 필요하면 본사 운영 정책에 따릅니다.

---

## PHP 샘플 (권장)

| 파일 | 용도 |
|------|------|
| `merchant-api-samples/php/icopay_config.example.php` | `compId`·브로커 시크릿·Base URL |
| `merchant-api-samples/php/IcopayMerchantApi.php` | 클라이언트 |
| `merchant-api-samples/php/checkout_unified.php` | prepare → embed 예제 |

```php
$prep = $api->prepareUnifiedCheckout($orderNo, $amount, $buyer, $currency, $productName, $lang);
$token = $prep['data']['sessionToken'];
echo $api->buildUnifiedEmbedHtml($token, 'icopay-checkout', $lang);
```

---

## 하지 말 것

| 금지 | 이유 |
|------|------|
| PG사·결제대행사 이름을 문서·UI·소스에 표기 | 계약·보안 — 가맹에는 ICOPAY만 |
| `/jpay-pay.html`, `/pay/` 등 비중립 URL을 직접 호출 | 설정·문구가 누락될 수 있음. `payUrl`·embed만 사용 |
| prepare body에 가맹 사이트 return URL을 넣음 | 브라우저 복귀는 ICOPAY NOTI → 가맹 Result |
| 브로커 시크릿을 브라우저에 노출 | 시크릿은 **가맹 서버만** |

---

## 리다이렉트·구독 (선택)

동일 브랜드·규칙입니다.

- 리다이렉트: `POST …/merchant/checkout/redirect/prepare`
- 구독: `POST …/merchant/checkout/subscription/prepare`

상세 표는 배포 키트 `merchantCheckoutApiParameterSpec` 및  
`merchant-api-samples/docs/unified-checkout-api-parameters*.html` 을 참고하세요.

---

## 문의

브로커 시크릿·Webhook 등록·API 배포는 **본사(ICOPAY)** 에 요청하세요.  
배포 후에는 관리자 **업체관리 → 가맹점API**에서 키·파라미터 표·샘플을 확인하세요.
