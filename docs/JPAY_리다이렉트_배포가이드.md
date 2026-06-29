# JPAY 리다이렉트 배포 가이드

| 항목 | 내용 |
|------|------|
| **문서명** | JPAY URL 결제 — Redirect(API 중계형) 배포 가이드 |
| **연동 방식** | API 중계형 **REDIRECT** — 고객 브라우저가 ICOPAY 결제 페이지로 이동 |
| **API 경로** | `/api/middleware/v1/merchant/jpay/redirect-checkout/*` |
| **WordPress** | `icopay-jpay` · `icopay-woocommerce` v1.1+ (`flow_mode=redirect`) |

> Inline(iframe) 배포는 `JPAY_URL결제_인라인API_배포가이드.md` 를 참고하세요.

---

## 1. Inline vs Redirect

| 구분 | Inline | Redirect |
|------|--------|----------|
| 고객 UX | 가맹 사이트 내 iframe | ICOPAY pay 페이지 전체 화면 |
| prepare API | `…/inline-checkout/prepare` | `…/redirect-checkout/prepare` |
| prepare 응답 핵심 | `sessionToken`, `embedScriptUrl` | `payUrl` |
| 완료 감지 | postMessage + status + 웹훅 | **NOTI Result(브라우저)** + status + 웹훅 |
| prepare body | buyer 등 | **returnUrl/cancelUrl 넣지 않음** |
| 본사 스위치 | API 중계형 INLINE Y | API 중계형 REDIRECT Y |

**기본값:** 모든 WordPress 플러그인은 `flow_mode=inline` — 기존 v1.0 WooCommerce 동작과 동일합니다.

---

## 2. 전체 흐름

```mermaid
sequenceDiagram
  participant Shop as 가맹 사이트
  participant MSrv as 가맹 서버(WP/PHP)
  participant ICOPAY as ICOPAY API
  participant Pay as jpay-pay (full page)
  participant JPAY as JPAY pay_index
  participant NOTI as NOTI MW

  Shop->>MSrv: 결제 시작(주문/숏코드)
  MSrv->>ICOPAY: POST redirect-checkout/prepare<br/>(returnUrl 없음)
  ICOPAY-->>MSrv: payUrl
  MSrv-->>Shop: 302 payUrl
  Shop->>Pay: ICOPAY 결제 페이지
  Pay->>JPAY: sale (서버 중계, pay_callbackurl=NOTI)
  JPAY-->>NOTI: 브라우저 3DS 복귀
  NOTI-->>Shop: 가맹 Result URL (NOTI 설정)
  MSrv->>ICOPAY: GET redirect-checkout/status
  ICOPAY-->>MSrv: paymentStatus PAID
  NOTI-->>MSrv: 가맹 Callback (서버 webhook)
```

**가맹 도메인은 PG(JPAY) pay_index 전문에 노출되지 않습니다.** ICOPAY 업체관리 JPAY 수신통보 URL에는 NOTI 주소만 등록합니다.

---

## 3. 본사(HQ) 설정

관리자: `https://icopay.co.kr` → **결제로직설정**

| 항목 | Redirect 사용 시 |
|------|------------------|
| API 중계형 REDIRECT 제공 | **Y** (`apiBrokerRedirectEnabledYn`) |
| API 중계형 기본 방식 | REDIRECT 또는 가맹 플러그인에서 redirect 선택 |
| 가맹 JPAY WEB PG 바인딩 | 운영 MID 등록 |
| 가맹 웹결제 | Y |
| 가맹 JPAY 수신통보 URL | NOTI callback/result **만** (가맹 URL 아님) |

ChillPay Redirect: `…/chillpay/redirect-checkout/prepare` — WooCommerce vendor=chillpay + flow_mode=redirect

---

## 4. API 요약

### Prepare

```
POST {API}/api/middleware/v1/merchant/jpay/redirect-checkout/prepare
Header: X-Icopay-Merchant-Broker-Secret
```

```json
{
  "compId": "M000123",
  "orderNo": "ORD-001",
  "amount": "100.00",
  "currency": "USD",
  "productName": "Sample",
  "lang": "ENG"
}
```

- **`returnUrl` / `cancelUrl` 을 body에 넣으면 `MERCHANT_RETURN_URL_NOT_ALLOWED` 로 거부됩니다.**
- 브라우저 복귀: **NOTI** 가맹 설정 → Result URL
- 서버 확정: **NOTI → 가맹 Callback** + **status API**

성공 응답 `data.payUrl` — 브라우저를 이 URL로 리다이렉트합니다.

### Status

```
GET {API}/api/middleware/v1/merchant/jpay/redirect-checkout/status?compId=&orderNo=
Header: X-Icopay-Merchant-Broker-Secret
```

`paymentStatus=PAID` 이면 주문 완료 처리(멱등).

---

## 5. WordPress 연동

### icopay-jpay

- **설정 → ICOPAY JPAY → Checkout flow: Redirect**
- prepare API에 returnUrl을 보내지 않음
- 브라우저 복귀: NOTI Result → 가맹 Result 페이지(플러그인/NOTI 설정)

### icopay-woocommerce v1.1+

- **WooCommerce → 설정 → 결제 → ICOPAY → Checkout flow: Redirect**
- prepare API에 returnUrl을 보내지 않음
- 인라인 결제 페이지의 `returnUrl`은 WooCommerce 주문 완료 UX용(플러그인 로컬)
- 웹훅: `/wp-json/icopay/v1/webhook` (v1.0과 동일)

---

## 6. 브라우저 복귀 (NOTI)

| URL | 등록 위치 |
|-----|-----------|
| Callback (서버) | **NOTI** → 가맹 webhook |
| Result (브라우저) | **NOTI** → 가맹 Result 페이지 |
| JPAY pay_notifyurl / pay_callbackurl | **ICOPAY 업체관리** → NOTI 주소만 |

복귀 후 **status API** 또는 **웹훅**으로 PAID 확인 (URL 파라미터만 신뢰하지 않음).

---

## 7. 오류 코드

| errorCode | 조치 |
|-----------|------|
| `MERCHANT_RETURN_URL_NOT_ALLOWED` | prepare body에서 returnUrl/cancelUrl 제거 |
| `REDIRECT_NOT_ENABLED` | HQ API 중계형 REDIRECT Y |
| `BROKER_AUTH` | broker secret 확인 |
| `URL_PAYMENT_PG_MISSING` | JPAY WEB 바인딩 |
| `INVALID_ORDER_NO` | orderNo 형식·길이 |

---

## 8. ZIP 빌드·배포

```powershell
.\tools\build-wp-plugin-zips.ps1
```

- WooCommerce: `woocommerce/icopay-woocommerce-1.1.0.zip`
- 일반 WP: `wordpress/icopay-jpay-1.0.0.zip`

WordPress `/wp-content/plugins/` 업로드 후 활성화, Permalink 저장.

---

## 9. 관련 문서

- `WordPress_JPAY_플러그인_배포가이드.md`
- `JPAY_URL결제_인라인API_배포가이드.md`
- `JPAY_연동_변경이력.md`
