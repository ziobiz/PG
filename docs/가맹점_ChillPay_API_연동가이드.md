# ICOPAY 가맹점 ChillPay API 개발 연동 매뉴얼

| 항목 | 내용 |
|------|------|
| **문서명** | 가맹점 ChillPay API 개발 연동 매뉴얼 |
| **배포** | 플랫폼(본사) → 가맹점(또는 용역 개발사) |
| **대상 독자** | 백엔드·결제·프론트 연동 담당 |
| **적용 PG** | **ChillPay** (ICOPAY 공개 API — DirectCredit · URL 결제 등) |
| **문의** | 계약·키·URL·권한·CCD 설정은 **플랫폼(본사)** 관리자 채널 |

**목적:** 가맹점이 ICOPAY **ChillPay 브로커/레거시 API**만 사용해 결제를 붙이고, 통보(웹훅)를 처리하기 위한 개발·검수 기준을 정리합니다.  
**JPAY만 연동하는 경우** → `가맹점_JPAY_API_연동가이드.md` 를 사용하세요.  
**전체 목차·배포 안내** → `가맹점_API_연동_매뉴얼_목차.md`

---

## 개정 이력

| 버전 | 일자 | 요약 |
|------|------|------|
| 1.0 | (배포일 기입) | ChillPay 전용 분리 매뉴얼 |

---

## 목차

1. [전제·용어](#1-전제용어)  
2. [본사로부터 전달받을 것](#2-본사로부터-전달받을-것)  
3. [권장 개발 순서 (ChillPay)](#3-권장-개발-순서-chillpay)  
4. [공통 기술 사양](#4-공통-기술-사양)  
5. [ChillPay REST API 상세](#5-chillpay-rest-api-상세)  
6. [가맹점 서버 통보(웹훅)](#6-가맹점-서버-통보웹훅)  
7. [구현 시 권장 사항](#7-구현-시-권장-사항)  
8. [검수·오픈 전 체크리스트](#8-검수오픈-전-체크리스트)  
9. [부록 — 엔드포인트](#9-부록--엔드포인트)

---

## 1. 전제·용어

| 용어 | 설명 |
|------|------|
| **업체코드 `compId`** | 플랫폼이 부여한 가맹점 코드 |
| **`merchantId`** | 플랫폼 내부 조직 ID(숫자). 키트의 `merchantOrgUnitId` |
| **연동 키트 JSON** | `publicApiBaseUrl`, `pgBrokerBlocks`(ChillPay 블록), 바인딩·통보 URL 등 |
| **브로커 URL** | `/api/middleware/v1/pg/chillpay/...` (**권장**) |
| **레거시 URL** | `/api/pay/chillpay/...` (동일 동작, 시크릿 미검증) |
| **브로커 시크릿** | 헤더 `X-Icopay-Merchant-Broker-Secret` (본사 **강제** 시 필수) |

키트에서 **PG 범위**를 ChillPay(`CHILLPAY` 또는 `ALL`)로 받았는지 확인하세요.

---

## 2. 본사로부터 전달받을 것

| # | 항목 |
|---|------|
| 1 | 업체코드 `compId` |
| 2 | 연동 키트 JSON(ChillPay 바인딩·엔드포인트 포함) |
| 3 | 브로커 시크릿 및 **강제 여부** |
| 4 | 운영/스테이징 `publicApiBaseUrl` (혼용 금지) |
| 5 | 통보(웹훅) URL 등록 요건 |

키트의 `pgBrokerBlocks` 중 **vendorScope가 CHILLPAY 또는 ALL** 인 블록의 `brokerUrl` / `legacyUrl` 을 사용합니다.

---

## 3. 권장 개발 순서 (ChillPay)

1. 키트·ChillPay 바인딩(`merchantPgBindings` 내 `CHILLPAY` 계열) 확인  
2. **통보 URL** 스텁(HTTPS·멱등)  
3. 브로커 시크릿 강제 시 헤더 모듈  
4. API 순서: **`/config`** → **`/checkout-context`** → (표시통화 모드일 때만) **`/display-fx-quote`** → **`/url-result-copy`**(선택) → **`/request`**  
5. 스테이징 검증 후 운영 전환  
6. 본사 검수

---

## 4. 공통 기술 사양

- **HTTPS**, POST 시 `Content-Type: application/json`, `Accept: application/json` 권장  
- **`compId` 또는 `merchantId`** — GET은 쿼리, POST는 JSON  
- **브로커 시크릿 강제 시** 헤더 `X-Icopay-Merchant-Broker-Secret` — 오류 시 HTTP **403**, `errorCode`: `BROKER_AUTH`  
- **응답:** `{ "success": true|false, "data": {...}, "message": "...", "errorCode": "..." }` — HTTP 200이어도 `success: false` 가능  

ChillPay에서 자주 보는 `errorCode` 예: `NOT_FOUND`, `ORG_DISABLED`, `BROKER_AUTH`, `CHILLPAY_ROUTE_NOT_CONFIGURED`, `INVALID_TOKEN`, `INVALID_AMOUNT`, `WEB_PAYMENT_DISABLED`, `URL_PAYMENT_PG_MISSING`, `DISPLAY_FX_*` 등.

---

## 5. ChillPay REST API 상세

**브로커 베이스:** `{BASE}/api/middleware/v1/pg/chillpay`  
**레거시 베이스:** `{BASE}/api/pay/chillpay`  
`{BASE}` = 키트 `publicApiBaseUrl`

| 순서 | 메서드 | 경로 | 설명 |
|------|--------|------|------|
| 1 | GET | `/config` | CCD·DirectCredit·리다이렉트 등 설정 |
| 2 | GET | `/checkout-context` | 체크아웃 컨텍스트(필수에 가깝게 `compId`/`merchantId`) |
| 3 | GET | `/display-fx-quote` | 표시통화(THB 정산) 모드 가맹만 |
| 4 | GET | `/url-result-copy` | 결과 페이지 문구(선택) |
| 5 | POST | `/request` | DirectCredit 승인 |

### POST `/request` 본문(대표)

| 필드 | 설명 |
|------|------|
| `compId` / `merchantId` | 가맹 식별 |
| `directCreditToken` | CCD 토큰 (`PaymentCreditToken` 등 별칭 지원) |
| `orderNo` | 주문번호 |

표시통화·FX는 본사 안내대로 `displayAmount`, `fxQuoteToken`, `urlPayPricingMode` 등 추가.

---

## 6. 가맹점 서버 통보(웹훅)

키트 **`merchantNotifyUrls`** 의 URL로 플랫폼이 통지합니다. **HTTPS**, 타임아웃 내 응답, **멱등** 처리, 서명·IP 규칙은 본사 보안 가이드를 따릅니다.

---

## 7. 구현 시 권장 사항

- 타임아웃 30~60초 이상 권장  
- 로그에 `orderNo`, `compId`, `errorCode` — **카드번호·CVV 저장 금지**  
- 브로커 시크릿 로테이션 시 구 키 즉시 폐기 및 서버 동시 반영  

---

## 8. 검수·오픈 전 체크리스트

- [ ] 운영 `publicApiBaseUrl` 만 사용  
- [ ] 브로커 강제 시 시크릿 헤더  
- [ ] `checkout-context` → `request` 및 토큰·주문번호 검증  
- [ ] 표시통화 모드 시 견적 토큰 만료·재견적 UX  
- [ ] 통보 URL HTTPS·멱등·로그  
- [ ] 주요 `errorCode` 사용자 메시지  

---

## 9. 부록 — 엔드포인트

`{BASE}` = 키트 `publicApiBaseUrl`

**브로커**

```
GET  {BASE}/api/middleware/v1/pg/chillpay/config
GET  {BASE}/api/middleware/v1/pg/chillpay/checkout-context
GET  {BASE}/api/middleware/v1/pg/chillpay/display-fx-quote
GET  {BASE}/api/middleware/v1/pg/chillpay/url-result-copy
POST {BASE}/api/middleware/v1/pg/chillpay/request
```

**레거시:** `/api/middleware/v1/pg/chillpay` → `/api/pay/chillpay` 치환  

---

**JPAY 연동:** `가맹점_JPAY_API_연동가이드.md` 참고.  
본 매뉴얼은 플랫폼 정책에 따라 갱신될 수 있습니다.
