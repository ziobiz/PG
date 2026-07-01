# ICOPAY 가맹점 통합 Checkout API — 연동 파라미터 규격

| 항목 | 내용 |
|------|------|
| **문서 ID** | ICOPAY-CHECKOUT-PREPARE-001 |
| **버전** | 1.1 |
| **대상** | ICOPAY와 API 연동하는 가맹점(백엔드 개발자) |
| **API** | `POST /api/middleware/v1/merchant/checkout/prepare` (통합, 권장) |
| **연동 방식** | JSON(REST) · PHP(`IcopayMerchantApi.php`) |
| **관련 문서** | [가맹점_통합Checkout_API_연동흐름.md](./가맹점_통합Checkout_API_연동흐름.md) (Prepare·Session·Status·Embed 흐름) |

본 문서는 ChillPay 등 PG사 매뉴얼의 **Request Parameters 표**와 동일한 목적으로, **가맹점이 ICOPAY prepare 호출 시 반드시·선택적으로 넣어야 하는 JSON 필드**를 규정합니다.

---

## 1. 호출 개요

| 항목 | 값 |
|------|-----|
| Method | `POST` |
| Path | `/api/middleware/v1/merchant/checkout/prepare` |
| Content-Type | `application/json` |
| Accept | `application/json` |
| 인증 헤더 | `X-Icopay-Merchant-Broker-Secret` — 브로커 시크릿 **강제** 시 필수 |

**Base URL:** 연동 키트 JSON의 `publicApiBaseUrl` (예: `https://api.icopay.co.kr`)

**식별:** JSON 루트에 `compId`(업체코드) 또는 `merchantId`(숫자) 중 **하나 이상** 필수.

---

## 2. HTTP 헤더

| No. | Header | M/O | 설명 | Remark |
|-----|--------|-----|------|--------|
| 1 | Content-Type | **M** | `application/json` | POST 본문 |
| 2 | Accept | O | `application/json` | 권장 |
| 3 | X-Icopay-Merchant-Broker-Secret | **C** | 브로커 시크릿 | 강제(enforceYn=Y) 시 **M** |

---

## 3. 표 1.1 — Prepare 요청 본문(JSON) 파라미터

| No. | Request Parameter | Data Type | Length | M/O | Description | Remark |
|-----|-------------------|-----------|--------|-----|-------------|--------|
| 1 | compId | String | 64 | **M*** | 가맹 업체코드 | 플랫폼 부여 코드(예: M000123). *merchantId 대체 가능 |
| 2 | merchantId | Number | — | O | 가맹 조직 ID | compId 와 택1 |
| 3 | orderNo | String | 64 | **M** | 주문·거래 참조번호 | **ChillPay 운영:** 영숫자·`-`·`_` 만, **최대 20자**. **JPAY 운영:** 최대 64자. `+` `/` `#` `$` 등 특수문자 불가 |
| 4 | amount | Number | 12 | **M** | 결제 금액 | 0 초과. JPY·KRW 는 정수 금액 권장 |
| 5 | currency | String | 3 | O | ISO 4217 통화 | USD, JPY, KRW, THB 등. 생략 시 가맹·운영 PG 정책 |
| 6 | productName | String | 500 | O | 상품명 | 결제창 표시 |
| 7 | item | String | 500 | O | 상품명 별칭 | productName 없을 때 |
| 8 | lang | String | 5 | O | 결제창 UI 언어 | **KOR · ENG · JPN · CHN · THA** (또는 ko/en/ja/zh/th). langCode·locale 동의어 |
| 9 | buyer | Object | — | **M** | 구매자 정보 | **표 1.2** 참고. 하위 **email·phone·countryIso2 필수**(JPAY·ICOPAY). `buyerPrefill` 키도 동일 |

### 요청 예시

```json
{
  "compId": "M000123",
  "orderNo": "ORD-20260501001",
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

샘플 파일: `{BASE}/merchant-api-samples/json/unified-prepare-request.json`

---

## 4. 표 1.2 — buyer 객체 파라미터

표 1.1의 `buyer`는 **객체**이며, 이메일·전화·국가코드는 루트가 아니라 **아래 하위 필드**로 전달합니다. JPAY·ICOPAY 모두 **필수(M)** 입니다.

ICOPAY는 **email · phone · countryIso2** 를 모든 가맹 prepare 에서 **필수**로 수집·검증합니다. JPAY 서버 직접 `sale` 호출 시에는 동일 정보를 `payEmailAddress` · `payTelephone` · `payCountryIsoCode2` 로 보냅니다.

| No. | Parameter | Data Type | Length | M/O | Description | Remark |
|-----|-----------|-----------|--------|-----|-------------|--------|
| 1 | email | String | 254 | **M** | 구매자 이메일 | JPAY·ICOPAY 필수. sale: `payEmailAddress` |
| 2 | phone | String | 32 | **M** | 구매자 전화(로컬) | JPAY·ICOPAY 필수. 국가번호 `+82` 등 **제거**, 로컬 번호만. sale: `payTelephone` |
| 3 | countryIso2 | String | 2 | **M** | 국가 ISO2 | JPAY·ICOPAY 필수. KR, US, TH 등 **대문자 2자**. sale: `payCountryIsoCode2` |
| 4 | address | String | 200 | O | 배송 주소 1행 | shipping prefill (선택) |
| 5 | address2 | String | 200 | O | 배송 주소 2행 | |
| 6 | city | String | 100 | O | 도시 | |
| 7 | state | String | 100 | O | 주·도 | |
| 8 | postcode | String | 20 | O | 우편번호 | zip 별칭 |
| 9 | shippingAddress | String | 200 | O | 별도 배송지 | address 와 다를 때 |
| 10 | shippingPhone | String | 32 | O | 별도 배송지 전화 | |

---

## 5. Prepare 성공 응답(data) 주요 필드

| No. | Field | Type | M/O | Remark |
|-----|-------|------|-----|--------|
| 1 | sessionToken | String | **M** | embed `data-session-token` |
| 2 | pgVendor | String | **M** | CHILLPAY 또는 JPAY |
| 3 | operationalPgCd | String | **M** | 운영 WEB PG 코드 |
| 4 | embedScriptUrl | String | **M** | `/v1/embed-checkout/{compId}` |
| 5 | payUrl | String | O | iframe src 대안 |
| 6 | buyerPrefill | Object | O | 정규화된 buyer |
| 7 | expiresAt | String | **M** | 세션 만료(ISO-8601) |

---

## 6. 결제 상태 조회 (GET status)

| No. | Query Parameter | Type | M/O | Remark |
|-----|-----------------|------|-----|--------|
| 1 | compId | String | **M*** | *merchantId 대체 |
| 2 | orderNo | String | **M** | prepare orderNo |
| 3 | merchantId | Number | O | compId 대체 |

`GET {BASE}/api/middleware/v1/merchant/checkout/status?compId=&orderNo=`

---

## 7. 주요 errorCode

| errorCode | 의미 |
|-----------|------|
| BUYER_REQUIRED | buyer.email·phone·countryIso2 누락 |
| BROKER_AUTH | 브로커 시크릿 오류(403) |
| INVALID_ORDER_NO | orderNo 형식·길이 |
| INVALID_AMOUNT | amount 누락 또는 ≤ 0 |
| NOT_FOUND | compId 미등록 |
| URL_PAYMENT_PG_MISSING | 운영 WEB PG 미설정 |

---

## 8. 연동 방식별 참고

| 방식 | 참고 |
|------|------|
| **JSON** | 본 문서 + 키트 `integrationModes.json` + curl 예시 |
| **PHP** | `IcopayMerchantApi::prepareUnifiedCheckout()` + `checkout_unified.php` |

HTML 표: `{BASE}/merchant-api-samples/docs/unified-checkout-api-parameters.html`

---

## 9. 배포·키트

본사 관리 화면 **가맹점 API 생성** → 연동 키트 JSON 키:

- `merchantCheckoutApiParameterSpec` — 본 규격(기계 판독용 표)
- `merchantUnifiedCheckout` — URL·샘플
- `integrationModes.json` / `integrationModes.php`

---

## 개정 이력

| 버전 | 일자 | 요약 |
|------|------|------|
| 1.0 | 2026-06-01 | 통합 checkout prepare 파라미터 규격 최초 작성 |
