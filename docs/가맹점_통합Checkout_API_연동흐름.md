# ICOPAY 가맹점 통합 Checkout API — 연동 흐름 및 엔드포인트

| 항목 | 내용 |
|------|------|
| **문서 ID** | ICOPAY-CHECKOUT-FLOW-001 |
| **버전** | 1.0 |
| **대상** | ICOPAY와 API 연동하는 가맹점(백엔드·프론트 개발자) |
| **연동 방식** | 통합 인라인(Unified Inline) — PG 무관, ChillPay/JPAY 자동 분기 |
| **관련 문서** | [가맹점_통합Checkout_API_연동파라미터_규격.md](./가맹점_통합Checkout_API_연동파라미터_규격.md) · [unified-checkout-api-parameters.html](https://api.icopay.co.kr/merchant-api-samples/docs/unified-checkout-api-parameters.html) |
| **공개 HTML (3개 언어)** | [KO](https://api.icopay.co.kr/merchant-api-samples/docs/unified-checkout-api-flow.ko.html) · [EN](https://api.icopay.co.kr/merchant-api-samples/docs/unified-checkout-api-flow.html) · [JA](https://api.icopay.co.kr/merchant-api-samples/docs/unified-checkout-api-flow.ja.html) |

본 문서는 가맹점 포털·배포 키트에 표시되는 **Prepare · Session · Status · Embed 스크립트** 네 가지 URL의 역할과 호출 주체, 권장 연동 순서를 설명합니다.

**Base URL 예:** `https://api.icopay.co.kr`  
**compId 예:** `6000000028` (가맹점별로 본사 배포 시 할당)

---

## 1. 개요 — “통합”이란?

예전에는 PG별로 prepare 경로가 분리되어 있었습니다.

| PG | 레거시 prepare 경로 |
|----|---------------------|
| ChillPay | `POST /api/middleware/v1/merchant/chillpay/inline-checkout/prepare` |
| JPAY | `POST /api/middleware/v1/merchant/jpay/inline-checkout/prepare` |

**통합 API**는 하나의 prepare만 호출하면 ICOPAY가 해당 가맹점의 **운영 WEB PG 설정**을 조회한 뒤 ChillPay 또는 JPAY로 내부 위임합니다. 가맹 연동 코드는 PG 변경에 덜 민감해집니다.

| 항목 | 값 |
|------|-----|
| integrationMode | `INLINE_UNIFIED` |
| prepare | `POST /api/middleware/v1/merchant/checkout/prepare` |
| embed | `/v1/embed-checkout/{compId}` |

---

## 2. 엔드포인트 요약

| URL | Method | 호출 주체 | 브로커 시크릿 | 역할 |
|-----|--------|-----------|:-------------:|------|
| **Prepare** | POST | 가맹 **서버** | ✅ | 결제 세션 생성, `sessionToken` 발급 |
| **Session** | GET | **브라우저**(embed 위젯) | ❌ | 토큰 검증, 운영 PG(`pgVendor`) 확인 |
| **Status** | GET | 가맹 **서버** | ✅ | 주문별 결제 결과 조회 |
| **Embed 스크립트** | GET (JS) | **브라우저** | ❌ | 결제 iframe 위젯 부트스트랩 |

---

## 3. 전체 연동 흐름

```
[가맹 서버]  주문(PENDING) 저장
     │
     ▼
[가맹 서버]  POST .../checkout/prepare  (buyer 필수, broker secret)
     │
     ▼
[가맹 서버]  응답 data.sessionToken 확보
     │
     ▼
[브라우저]   HTML에 embed 스크립트 + data-session-token 삽입
     │
     ├─► GET /v1/embed-checkout/{compId}  (부트스트랩 JS)
     ├─► GET .../checkout/session?token=...  (위젯이 자동 호출)
     └─► iframe: /pay/{compId} 또는 /jpay-pay/{compId}
     │
     ▼
[브라우저]   postMessage ICOPAY_INLINE_CHECKOUT (결제 완료·실패)
     │
     ▼
[가맹 서버]  웹훅(merchantNotifyUrls) 또는 GET .../checkout/status 로 PAID 확인
     │
     ▼
[가맹 서버]  주문 상태 갱신 (멱등 처리)
```

**권장 3단계 (배포 키트 `integrationModes.json` 기준):**

1. 가맹 서버: `POST .../merchant/checkout/prepare` (`buyer.email` · `phone` · `countryIso2` 필수)
2. 응답 `data.sessionToken` → 브라우저에 `/v1/embed-checkout/{compId}` 스크립트만 전달
3. `GET .../checkout/status` 또는 HQ가 등록한 **웹훅**으로 PAID 확인

---

## 4. Prepare — 결제 세션 생성

```
POST https://api.icopay.co.kr/api/middleware/v1/merchant/checkout/prepare
```

### 4.1 역할

가맹 주문 정보를 ICOPAY에 등록하고, 결제창에 사용할 **일회용 `sessionToken`** 을 발급합니다. ICOPAY는 운영 PG에 따라 ChillPay/JPAY prepare로 내부 위임합니다.

### 4.2 호출 주체·인증

- **호출:** 가맹 백엔드만 (PHP `prepareUnifiedCheckout`, REST, Java 등)
- **헤더:** `Content-Type: application/json`, `Accept: application/json`
- **인증:** `X-Icopay-Merchant-Broker-Secret: {brokerSecret}` (본사 배포 시크릿, 브라우저에 노출 금지)

### 4.3 필수 body

| 필드 | 설명 |
|------|------|
| `compId` | 가맹점 코드 (또는 `merchantId`) |
| `orderNo` | 가맹 주문번호 (가맹 DB와 동일) |
| `amount` | 결제 금액 (&gt; 0) |
| `buyer.email` | 구매자 이메일 (**필수**) |
| `buyer.phone` | 로컬 전화번호, 국가번호 제외 (**필수**) |
| `buyer.countryIso2` | ISO 3166-1 alpha-2, 예: `KR` (**필수**) |

선택: `currency`, `productName`, `lang` (KOR·ENG·JPN·CHN·THA)

상세 파라미터 표는 [연동 파라미터 규격](./가맹점_통합Checkout_API_연동파라미터_규격.md) 참고.

### 4.4 응답 핵심 필드

| 필드 | 설명 |
|------|------|
| `sessionToken` | 브라우저 embed `data-session-token`에 전달 |
| `pgVendor` | 실제 사용 PG (`JPAY`, `CHILLPAY` 등) |
| `embedScriptUrl` | `https://api.icopay.co.kr/v1/embed-checkout/{compId}` |
| `expiresAt` | 세션 만료 시각 |
| `buyerPrefill` | prepare 시 전달한 buyer 정보 |

샘플: `merchant-api-samples/json/unified-prepare-request.json`, `unified-prepare-response.example.json`

---

## 5. Session — 세션 검증 및 PG 분기

```
GET https://api.icopay.co.kr/api/middleware/v1/merchant/checkout/session?token={sessionToken}
```

### 5.1 역할

`sessionToken`이 유효한지 확인하고, **어떤 PG 결제창을 iframe으로 띄울지** 결정합니다.

### 5.2 호출 주체

- **가맹점이 직접 호출할 필요 없음**
- embed 위젯(`icopay-embed-checkout-widget.js`)이 브라우저에서 자동 호출
- 브로커 시크릿 불필요 (토큰 자체가 일회용 자격 증명)

### 5.3 위젯 동작

응답 `data.pgVendor`에 따라 iframe URL 선택:

| pgVendor | iframe 경로 |
|----------|-------------|
| JPAY 계열 | `/jpay-pay/{compId}?entry=merchant_api&embed=1&session=...` |
| ChillPay 계열 | `/pay/{compId}?entry=merchant_api&embed=1&session=...` |

---

## 6. Status — 주문 결제 상태 조회

```
GET https://api.icopay.co.kr/api/middleware/v1/merchant/checkout/status?compId=6000000028&orderNo={orderNo}
```

### 6.1 역할

특정 `orderNo`의 ICOPAY 거래 상태를 조회합니다. postMessage·웹훅 이후 **서버 측 재확인**에 사용합니다.

### 6.2 호출 주체·인증

- **호출:** 가맹 서버
- **헤더:** `Accept: application/json`, `X-Icopay-Merchant-Broker-Secret`

### 6.3 응답 예시 필드

| 필드 | 설명 |
|------|------|
| `found` | 거래 존재 여부 |
| `paymentStatus` | `PAID`, `NOT_FOUND` 등 |
| `approvalNo` | 승인번호 |
| `amount`, `currency` | 승인 금액·통화 |
| `paidAt` | 승인 시각 |

운영 PG에 따라 내부적으로 ChillPay/JPAY status 조회로 위임됩니다.

---

## 7. Embed 스크립트 — 결제창 삽입

```
https://api.icopay.co.kr/v1/embed-checkout/6000000028
```

### 7.1 역할

ICOPAY 통합 인라인 결제 **위젯 부트스트랩 JavaScript**를 제공합니다. 가맹 쇼핑몰 페이지에 iframe 결제창을 삽입합니다.

### 7.2 HTML 예시

```html
<div id="icopay-checkout"></div>
<script src="https://api.icopay.co.kr/v1/embed-checkout/6000000028"
        data-session-token="{prepare 응답 sessionToken}"
        data-target="icopay-checkout"
        data-lang="ENG"
        async defer charset="utf-8"></script>
<script src="https://api.icopay.co.kr/merchant-api-samples/common/icopay-checkout.js"></script>
<script>
  IcopayCheckout.onMessage(function (detail) {
    if (detail.phase === 'finished' && detail.success) {
      location.href = '/order-complete?orderNo=' + encodeURIComponent(detail.orderNo || '');
    }
  }, 'https://api.icopay.co.kr');
</script>
```

### 7.3 위젯이 수행하는 작업

1. `data-session-token` 읽기 (prepare 선행 필수)
2. `GET .../checkout/session?token=...` 호출
3. 운영 PG에 맞는 결제 페이지 iframe 삽입
4. 결제 완료 시 `window.postMessage` — 이벤트 타입 `ICOPAY_INLINE_CHECKOUT`

`data-lang` 생략 시 페이지 `html[lang]`·브라우저 `Accept-Language` 자동 감지.

---

## 8. 보안·운영 체크리스트

| 항목 | 권장 |
|------|------|
| 브로커 시크릿 | 가맹 **서버**에만 보관. 브라우저·모바일 앱·프론트 JS에 넣지 않음 |
| sessionToken | prepare 직후 브라우저에만 전달. 만료(`expiresAt`) 내 사용 |
| 주문 저장 | prepare 전 가맹 DB에 `PENDING` 주문 생성 |
| 완료 처리 | 웹훅 + status 조회, **멱등**(동일 orderNo 중복 갱신 방지) |
| HTTPS | 가맹 웹훅 수신 URL은 HTTPS 권장 |

---

## 9. 샘플·다운로드

| 자료 | 경로 |
|------|------|
| 샘플 인덱스 | `https://api.icopay.co.kr/merchant-api-samples/index.html` |
| 연동 흐름 (KO) | `https://api.icopay.co.kr/merchant-api-samples/docs/unified-checkout-api-flow.ko.html` |
| 연동 흐름 (EN) | `https://api.icopay.co.kr/merchant-api-samples/docs/unified-checkout-api-flow.html` |
| 연동 흐름 (JA) | `https://api.icopay.co.kr/merchant-api-samples/docs/unified-checkout-api-flow.ja.html` |
| 파라미터 표 (영문 HTML) | `https://api.icopay.co.kr/merchant-api-samples/docs/unified-checkout-api-parameters.html` |
| PHP 통합 샘플 | `merchant-api-samples/php/checkout_unified.php` |
| PHP 클라이언트 | `merchant-api-samples/php/IcopayMerchantApi.php` |
| 배포 키트 | 가맹점 API 배포 시 `integrationModes.json`, `merchantUnifiedCheckout` 키 |

---

## 10. FAQ

**Q. Session을 가맹 서버에서 호출해야 하나요?**  
A. 아니요. embed 위젯이 브라우저에서 자동 호출합니다.

**Q. ChillPay 가맹인데 JPAY URL이 나올 수 있나요?**  
A. 통합 API는 **운영 WEB PG** 기준으로 분기합니다. 본사 전산에서 URL 결제 운영 PG가 JPAY로 설정되어 있으면 JPAY 결제창이 열립니다.

**Q. postMessage만으로 충분한가요?**  
A. UX용으로는 충분할 수 있으나, **서버 확정**은 웹훅 또는 status 조회를 권장합니다.

**Q. 레거시 per-PG API를 계속 써도 되나요?**  
A. 가능합니다. 신규 연동은 통합 prepare + `/v1/embed-checkout/{compId}` 를 권장합니다.
