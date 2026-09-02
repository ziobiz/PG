# ICOPAY 가맹점 API 개발 연동 매뉴얼

| 항목 | 내용 |
|------|------|
| **문서명** | 가맹점 API 개발 연동 매뉴얼 |
| **배포** | 플랫폼(본사) → 가맹점(또는 가맹점 용역 개발사) |
| **대상 독자** | 백엔드·결제 연동 담당 개발자, 기술 PM |
| **적용 PG** | ChillPay · JPAY (플랫폼이 개방한 공개 API 기준) |
| **문의** | 계약·키·URL·권한 변경은 **플랫폼(본사)** 관리자 채널로 요청 |

**본 문서의 목적:** 가맹점이 **자사 쇼핑몰·앱·POS 백엔드**에서 ICOPAY가 제공하는 **공개 REST API**를 호출해 결제를 연동하고, **통보(웹훅)** 를 수신·처리하기까지의 **개발·검수 기준**을 한곳에 정리합니다.

---

## 개정 이력

| 버전 | 일자 | 요약 |
|------|------|------|
| 1.0 | (배포일 기입) | 최초 배포 — ChillPay·JPAY 브로커 경로 기준 |

---

## 목차

1. [연동 전제와 용어](#1-연동-전제와-용어)  
2. [본사로부터 전달받을 것(인수인계)](#2-본사로부터-전달받을-것인수인계)  
3. [권장 개발 순서](#3-권장-개발-순서)  
4. [공통 기술 사양](#4-공통-기술-사양)  
5. [ChillPay 연동](#5-chillpay-연동)  
6. [JPAY 연동](#6-jpay-연동)  
7. [가맹점 서버 통보(웹훅)](#7-가맹점-서버-통보웹훅)  
8. [구현 시 권장 사항](#8-구현-시-권장-사항)  
9. [검수·오픈 전 체크리스트](#9-검수오픈-전-체크리스트)  
10. [부록 — 엔드포인트 일람](#10-부록--엔드포인트-일람)

---

## 1. 연동 전제와 용어

### 1.1 역할 구분

| 역할 | 설명 |
|------|------|
| **PG사** | ChillPay, JPAY 등 실제 결제 승인·취소를 처리하는 기관 |
| **플랫폼(본사)** | PG와 1:1 연동을 보유하고, 가맹점에는 **동일한 ICOPAY API**로 1:N 서비스를 제공하는 주체 |
| **가맹점** | 본 매뉴얼에 따라 ICOPAY API를 호출하고, 통보 URL로 결과를 받는 주체 |

가맹점은 **PG사 대시보드에 직접 가입하는 방식이 아닌 경우가 많으며**, MID·키·노티 URL은 **본사가 계약·설정한 범위** 안에서 키트로 안내됩니다.

### 1.2 용어

| 용어 | 설명 |
|------|------|
| **업체코드 `compId`** | 플랫폼이 가맹점에 부여한 문자열 코드(예: `M000123`) |
| **`merchantId`** | 플랫폼 내부 조직 ID(숫자). 키트의 `merchantOrgUnitId`와 동일할 수 있음 |
| **연동 키트 JSON** | 엔드포인트·베이스 URL·바인딩 요약·노티 수신 URL 등이 담긴 JSON 묶음 |
| **브로커 URL** | `/api/middleware/v1/pg/...` — **권장** 연동 경로 |
| **레거시 URL** | `/api/pay/...` — 동일 기능, 이행·구 시스템용 |
| **브로커 시크릿** | 브로커 URL 호출 시 선택적으로 요구되는 HTTP 헤더 비밀값 |

---

## 2. 본사로부터 전달받을 것(인수인계)

개발 착수 전에 아래를 **서면·메일·보안 채널** 등으로 수령했는지 확인하세요.

| # | 항목 | 비고 |
|---|------|------|
| 1 | **업체코드 `compId`** | 주문·로그에 함께 남길 것 |
| 2 | **연동 키트 JSON** | 본사 관리 화면 「가맹점 API 생성」에서 발급 또는 본사가 직접 전달 |
| 3 | **브로커 시크릿** (해당 시) | 재발급 시 **이전 값 즉시 무효**. 1회만 표시되는 평문은 안전하게 보관 |
| 4 | **브로커 시크릿 강제 여부** | 강제 시 모든 브로커 호출에 헤더 필수 |
| 5 | **운영 / 스테이징 구분** | `publicApiBaseUrl` 이 환경별로 다름 — 혼용 금지 |
| 6 | **통보 URL 등록 요건** | 가맹점이 열어 둘 HTTPS URL·방화벽·인증 방식(본사 별도 안내) |

키트 JSON의 주요 키:

| 키 | 용도 |
|----|------|
| `publicApiBaseUrl` | 모든 API의 베이스(끝에 `/` 없이 사용) |
| `pgBrokerBlocks` | PG별 `brokerUrl` / `legacyUrl` 목록 |
| `notifyIngressUrlMiddleware` | PG·NOTI가 플랫폼을 호출할 때 쓰는 **수신 URL**(가맹점 구현 대상 아님, 본사·PG 등록용) |
| `merchantPgBindings` | 허용된 PG코드·MID 등 요약 |
| `merchantNotifyUrls` | **가맹점이 구현할 통보(웹훅) URL** 목록 |
| `integrationChecklist` | 본사가 제시하는 배포 전 확인 문구 |

실제 호출 URL은 **항상 키트의 `brokerUrl`(또는 합의된 `legacyUrl`)을 기준**으로 하세요. 아래 경로 표는 키트와 동일한 목록입니다.

---

## 3. 권장 개발 순서

1. **키트 확인** — `publicApiBaseUrl`, PG 블록, 통보 URL 타입 확인  
2. **통보 URL 스텁 구현** — HTTPS, 200 응답, 본문 파싱·로그·멱등 설계  
3. **브로커 시크릿** — 강제면 클라이언트에 헤더 삽입 모듈부터 적용  
4. **ChillPay 또는 JPAY** 중 계약된 PG부터  
   - **권장(인라인 iframe):** PHP/JSP 샘플(`merchant-api-samples/`) — 서버 `prepare` → `sessionToken` → embed → postMessage/웹훅  
   - ChillPay(고급): `config` → `checkout-context` → (필요 시) `display-fx-quote` → `request`  
   - JPAY(고급): `sale` 호출 및 `redirectUrl`·`status` 처리  
5. **스테이징(제공 시) 검증** 후 운영 URL로 전환  
6. **본사 검수 요청** — 아래 체크리스트 제출

### 3.1 PHP / JSP 연동 (인라인 결제 — 권장)

가맹 쇼핑몰이 **PHP** 또는 **JSP** 인 경우, 본사 키트 JSON의 `merchantIntegrationSamples` 와 아래 정적 샘플을 사용합니다.

| 항목 | URL ( `{BASE}` = `publicApiBaseUrl` ) |
|------|----------------------------------------|
| 샘플 목록 | `{BASE}/merchant-api-samples/README.txt` |
| PHP 클라이언트 | `{BASE}/merchant-api-samples/php/IcopayMerchantApi.php` |
| PHP ChillPay 예제 | `{BASE}/merchant-api-samples/php/checkout_chillpay.php` |
| PHP JPAY 예제 | `{BASE}/merchant-api-samples/php/checkout_jpay.php` |
| JSP Java 클라이언트 | `{BASE}/merchant-api-samples/jsp/IcopayMerchantApi.sample.java` |
| JSP ChillPay 예제 | `{BASE}/merchant-api-samples/jsp/checkout-chillpay.jsp` |
| postMessage JS | `{BASE}/merchant-api-samples/common/icopay-checkout.js` |

**흐름**

1. 가맹 서버(PHP/JSP): 주문 저장 후 `POST .../inline-checkout/prepare` (헤더 `X-Icopay-Merchant-Broker-Secret`)  
2. 응답 `data.sessionToken` 으로 embed 스크립트 HTML 생성 (`IcopayMerchantApi.buildEmbedHtml`)  
3. 브라우저: ICOPAY 결제 iframe 표시 → `ICOPAY_INLINE_CHECKOUT` postMessage 또는 `merchantNotifyUrls` 웹훅으로 완료  
4. (선택) `GET .../inline-checkout/status?compId=&orderNo=` 폴링  

**보안:** 브로커 시크릿은 **가맹 서버에만** 저장. JavaScript·모바일 앱에 넣지 마세요.

---

## 4. 공통 기술 사양

### 4.1 전송

- **TLS(HTTPS)** 필수(운영)  
- **POST** 시 `Content-Type: application/json`  
- **`Accept: application/json`** 권장  

### 4.2 가맹점 식별

각 API는 **`compId` 또는 `merchantId` 중 하나 이상** 필요합니다.

- **GET:** 쿼리 스트링  
- **POST:** JSON 루트 필드  

### 4.3 브로커 시크릿 헤더 (강제 시)

| 헤더 | 값 |
|------|-----|
| `X-Icopay-Merchant-Broker-Secret` | 본사가 발급한 시크릿 문자열 |

- 누락·불일치 시 **HTTP 403**, `errorCode`: `BROKER_AUTH`  
- **레거시 URL**(`/api/pay/...`)은 시크릿 검증 없음(본사 정책에 따름)

### 4.4 응답 포맷

모든 API는 공통 래퍼를 사용합니다.

**성공 예:**

```json
{
  "success": true,
  "data": { }
}
```

**실패 예:**

```json
{
  "success": false,
  "message": "사람이 읽을 수 있는 설명",
  "errorCode": "기계 판별용 코드"
}
```

자주 쓰이는 `errorCode` 예: `NOT_FOUND`, `ORG_DISABLED`, `BROKER_AUTH`, `JPAY_ERROR`, `CHILLPAY_ROUTE_NOT_CONFIGURED`, `INVALID_TOKEN`, `INVALID_AMOUNT`, `WEB_PAYMENT_DISABLED`, `URL_PAYMENT_PG_MISSING` 등.  
**HTTP 200이어도 `success: false`일 수 있으므로** 반드시 `success`와 `errorCode`를 판별하세요.

---

## 5. ChillPay 연동

**브로커 베이스:** `{BASE}/api/middleware/v1/pg/chillpay`  
**레거시 베이스:** `{BASE}/api/pay/chillpay`  
`{BASE}` = 키트의 `publicApiBaseUrl`

### 5.1 API 목록

| 순서 | 메서드 | 경로 | 설명 |
|------|--------|------|------|
| 1 | GET | `/config` | 결제·CCD 연동에 필요한 설정 |
| 2 | GET | `/checkout-context` | 체크아웃 화면용 컨텍스트(가맹 표시명, 기본 금액, 통화 등) |
| 3 | GET | `/display-fx-quote` | **표시통화 DP 모드** 가맹점만 — 견적·토큰 발급 (실결제는 본사 URL결제설정 PG `settlementCurrency`, THB 한정 아님) |
| 4 | GET | `/url-result-copy` | 결과 페이지용 문구(다국어, 없을 수 있음) |
| 5 | POST | `/request` | DirectCredit 승인 요청 |

### 5.2 GET `/config`

- **쿼리:** `compId` 또는 `merchantId` (연동 정책에 따라 생략 가능 여부는 본사 확인)  
- **응답 `data`:** 프론트·백엔드가 ChillPay 스크립트·API 호출에 사용할 설정 맵  

### 5.3 GET `/checkout-context`

- **쿼리:** `compId` 또는 `merchantId` (**권장: 필수 수준으로 사용**)  
- 실패 시: 가맹 미존재, 웹결제 OFF, URL PG 미설정 등 — `message` / `errorCode` 참고  

### 5.4 GET `/display-fx-quote`

- **쿼리:** `compId` (**필수**), `displayCurrency` (선택)  
- 본사에서 해당 가맹에 표시통화 모드를 켜지 않았으면 오류 반환  

### 5.5 GET `/url-result-copy`

- **쿼리:** `compId` 또는 `merchantId`  
- **응답 `data`:** 결과 문구 맵(키는 플랫폼 정의). 없으면 `{}` 에 가깝게 올 수 있음  

### 5.6 POST `/request` (DirectCredit)

**본문(JSON) 필수·대표:**

| 필드 | 설명 |
|------|------|
| `compId` 또는 `merchantId` | 가맹 식별 |
| `directCreditToken` | CCD 등에서 발급된 토큰. `PaymentCreditToken` / `paymentCreditToken` 등 **별칭 키** 일부 지원 |
| `orderNo` | 가맹 주문번호 |

표시통화·FX 모드일 때는 본사 안내대로 `displayAmount`, `fxQuoteToken`, `urlPayPricingMode` 등을 추가합니다.  
**성공 시 `data`:** ChillPay 응답 형식에 맞는 객체.

---

## 6. JPAY 연동

**브로커 베이스:** `{BASE}/api/middleware/v1/pg/jpay`  
**레거시 베이스:** `{BASE}/api/pay/jpay`

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/sale` | JPAY `pay_index` 서버 사이드 호출에 대응 |

### 6.1 POST `/sale` 본문

| 필드 | 필수 | 설명 |
|------|------|------|
| `compId` 또는 `merchantId` | 예 | 가맹 식별 |
| `orderNo` | 예 | 최대 64자(초과 시 잘림) |
| `amount` | 예 | 0 초과 |
| `currency` | 아니오 | 기본 `USD` |
| `payUrl` | 아니오 | 비우면 플랫폼 공개 베이스 사용 |

카드·청구지 등 JPAY 스펙 필드는 JSON 키로 전달 가능(예: `payCardno` 등 — 플랫폼이 JPAY 폼 필드명으로 매핑).

### 6.2 응답 `data` (참고)

| 키 | 설명 |
|----|------|
| `success` | 플랫폼 처리 성공 여부 — `false` 이면 `message` 확인 |
| `status` | JPAY 응답 코드(예: 3DS 시 리다이렉트 등 — JPAY 매뉴얼 병행) |
| `redirectUrl` | 고객 브라우저 이동이 필요할 때 |
| `orderNo` | 주문번호 |
| `rawResponse` | 원시 JSON(디버깅) |

최종 매입·노티는 **비동기**일 수 있으므로 **§7 통보 URL** 처리를 반드시 구현하세요.

---

## 7. 가맹점 서버 통보(웹훅)

플랫폼은 거래 상태 변경 시 키트의 **`merchantNotifyUrls`** 에 등록된 URL로 HTTP를 호출할 수 있습니다.

가맹점은 다음을 준수합니다.

- **HTTPS** 엔드포인트 제공  
- **타임아웃 내 200** 응답(본사가 별도 규격을 주면 그에 따름)  
- **동일 주문에 대한 중복 통지** 가능성을 고려한 **멱등 처리**  
- 요청 **서명·IP 제한**이 있는 경우 본사 **보안 가이드** 문서 준수  

---

## 8. 구현 시 권장 사항

- **타임아웃:** 결제 API는 PG 응답 지연을 고려해 30~60초 이상 권장(내부 정책에 맞게 조정)  
- **로깅:** `orderNo`, `compId`, `errorCode`, 요청 ID(있을 경우)를 남기되 **카드번호·CVV는 저장 금지**  
- **재시도:** `success: false` 일 때 동일 주문번호로 무분별 재전송하지 말고, 본사에 확인할 것  
- **키 로테이션:** 브로커 시크릿 재발급 시 **구 시크릿 즉시 폐기** — 애플리케이션·환경변수 동시 반영  

---

## 9. 검수·오픈 전 체크리스트

가맹점 → 본사 제출용으로 활용 가능합니다.

- [ ] 운영 `publicApiBaseUrl` 으로만 호출(테스트 혼선 없음)  
- [ ] 브로커 사용 시 강제 여부와 `X-Icopay-Merchant-Broker-Secret` 일치  
- [ ] ChillPay: `checkout-context` → `request` 흐름 및 토큰·주문번호 검증  
- [ ] JPAY: `sale` 후 `status` / `redirectUrl` / 고객 복귀 URL 처리  
- [ ] 통보 URL: HTTPS, 멱등, 로그, 장애 시 알림  
- [ ] 주요 `errorCode` 에 대한 사용자 메시지 매핑  
- [ ] 본사 요청 시 **샘플 주문 로그·재현 절차** 제공 가능  

---

## 10. 부록 — 엔드포인트 일람

`{BASE}` = 키트 `publicApiBaseUrl` (끝 슬래시 없음)

**ChillPay — 브로커**

```
GET  {BASE}/api/middleware/v1/pg/chillpay/config
GET  {BASE}/api/middleware/v1/pg/chillpay/checkout-context
GET  {BASE}/api/middleware/v1/pg/chillpay/display-fx-quote
GET  {BASE}/api/middleware/v1/pg/chillpay/url-result-copy
POST {BASE}/api/middleware/v1/pg/chillpay/request
```

**ChillPay — 레거시:** 위 경로에서 `/api/middleware/v1/pg/chillpay` → `/api/pay/chillpay` 로 치환  

**JPAY — 브로커**

```
POST {BASE}/api/middleware/v1/pg/jpay/sale
```

**JPAY — 레거시**

```
POST {BASE}/api/pay/jpay/sale
```

---

**끝.** 본 매뉴얼은 플랫폼 배포 정책에 따라 갱신될 수 있습니다. 최신본은 본사에 문의하세요.
