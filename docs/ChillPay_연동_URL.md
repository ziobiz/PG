# ChillPay 연동 URL 정리

로컬 개발 시 브라우저·문서 예시는 **localhost** 기준으로 표기합니다.  
실제 운영 배포 URL은 배포 단계에서만 사용합니다.

## 1. 호스티드 결제 (브라우저 → ChillPay 결제 페이지)

HTML Form 또는 Code Template으로 **POST** 하는 대상 URL (Merchant Integration Manual §2.1, 2.2).

| 환경 | URL |
|------|-----|
| Sandbox | `https://sandbox-cdnv3.chillpay.co/Payment/` |
| Production | `https://cdn.chillpay.co/Payment/` |

## 2. DirectCredit (인라인 카드, 서버 JSON API)

현재 PG 앱(`ChillPayService`)이 호출하는 DirectCredit v1 엔드포인트.

| 환경 | Base | 결제 경로 |
|------|------|-----------|
| Sandbox | `https://sandbox-api-directcredit.chillpay.co` | `/api/v1/payment` |
| Production | `https://api-directcredit.chillpay.co` | `/api/v1/payment` |

## 3. Payment API v2 (서버 간, `application/x-www-form-urlencoded`)

Manual §2.3. 리다이렉트와 별도로, 백엔드에서 폼 인코딩 POST로 호출하는 API 베이스.

| 환경 | URL |
|------|-----|
| Sandbox | `https://sandbox-appsrv2.chillpay.co/api/v2/Payment/` |
| Production | `https://appsrv.chillpay.co/api/v2/Payment/` |

## 4. CCD 스크립트 (카드 입력·토큰 발급)

| 환경 | URL |
|------|-----|
| Sandbox | `https://sandbox-bankdemo3.chillpay.co/js/ccdpayment.js` |
| Production | `https://cdn.chill.credit/js/ccdpayment.js` |

## 5. 앱에서의 노출

- `GET http://localhost:8080/api/pay/chillpay/config` 응답에 다음 필드가 포함됩니다.  
  - `ccdScriptUrl`, `directCreditApiUrl`, `redirectPaymentPageUrl`, `paymentAppsrvV2Url`, `merchantCode`, `routeNo`, `sandbox`
- `GET /api/pay/chillpay/checkout-context?compId={업체코드}` — 인라인 결제 페이지용 가맹점명·기본상품·금액(JPY).
- 결제 페이지: `/pay/{compId}` → `pay.html?m=` (인라인 UI, DirectCredit API).

## 6. Transaction Services — Search Payment Transaction

| 환경 | URL |
|------|-----|
| Sandbox | `https://sandbox-api-transaction.chillpay.co/api/v1/payment/search` |
| Production | `https://api-transaction.chillpay.co/api/v1/payment/search` |

- Method: POST, `Content-Type: application/json`, 헤더 `CHILLPAY-MerchantCode`, `CHILLPAY-ApiKey`, 본문 필드 및 Checksum 순서는 `ChillPay-API-Transaction-Services-Document-EN_v1.0.6` Table 1.2~1.3.
- **통합내역** 화면: `TransactionDate` 구간 위주.
- **통합정산** 화면: 동일 API에 `PaymentDateFrom`/`PaymentDateTo` 및 정렬 `Settled` 등을 사용해 칠페이 정책상 정산·Settled·수수료 필드를 조회(ICOPAY 내부 정산 배치와 별개).

## 7. CheckSum

DirectCredit 요청의 연결 문자열 순서는 `ChillPayDirectCreditRequest#toConcatString()` 및 매뉴얼 Table 1.3을 따릅니다.  
**호스티드 결제 / v2 Payment** 는 매뉴얼 해당 표(예: Payment용 필드 순서)와 반드시 일치시켜야 합니다. 구현 시 ChillPay Merchant Integration Manual 최신판을 기준으로 검증하세요.
