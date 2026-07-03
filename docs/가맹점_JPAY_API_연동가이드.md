# ICOPAY 가맹점 JPAY API 개발 연동 매뉴얼

| 항목 | 내용 |
|------|------|
| **문서명** | 가맹점 JPAY API 개발 연동 매뉴얼 |
| **배포** | 플랫폼(본사) → 가맹점(또는 용역 개발사) |
| **대상 독자** | 백엔드·결제 연동 담당 |
| **적용 PG** | **JPAY** (ICOPAY 공개 API — `pay_index` 서버 사이드) |
| **문의** | 계약·MID·키·노티 URL·`jpayNotifyIngressStyle` 등은 **플랫폼(본사)** 관리자 채널 |

**목적:** 가맹점이 ICOPAY **JPAY 브로커/레거시 API** 또는 **인라인 iframe URL 결제**로 매출·3DS·통보(웹훅)를 처리하기 위한 개발·검수 기준을 정리합니다.  
**인라인 URL 결제(쇼핑몰 iframe) 배포·검수** → **`JPAY_URL결제_인라인API_배포가이드.md`** (권장)  
**ChillPay만 연동하는 경우** → `가맹점_ChillPay_API_연동가이드.md` 를 사용하세요.  
**전체 목차·배포 안내** → `가맹점_API_연동_매뉴얼_목차.md`

---

## 개정 이력

| 버전 | 일자 | 요약 |
|------|------|------|
| 1.0 | (배포일 기입) | JPAY 전용 분리 매뉴얼 |

---

## 목차

1. [전제·용어](#1-전제용어)  
2. [본사로부터 전달받을 것](#2-본사로부터-전달받을-것)  
3. [권장 개발 순서 (JPAY)](#3-권장-개발-순서-jpay)  
3-A. [인라인 URL 결제 (iframe)](#3-a-인라인-url-결제-iframe)  
4. [공통 기술 사양](#4-공통-기술-사양)  
5. [JPAY REST API 상세](#5-jpay-rest-api-상세)  
6. [가맹점 서버 통보(웹훅)](#6-가맹점-서버-통보웹훅)  
7. [구현 시 권장 사항](#7-구현-시-권장-사항)  
8. [검수·오픈 전 체크리스트](#8-검수오픈-전-체크리스트)  
9. [부록 — 엔드포인트](#9-부록--엔드포인트)

---

## 1. 전제·용어

| 용어 | 설명 |
|------|------|
| **업체코드 `compId`** | 플랫폼이 부여한 가맹점 코드 |
| **`merchantId`** | 플랫폼 내부 조직 ID. 키트의 `merchantOrgUnitId` |
| **연동 키트 JSON** | `publicApiBaseUrl`, `pgBrokerBlocks`(JPAY 블록), 바인딩·통보 URL 등 |
| **브로커 URL** | `/api/middleware/v1/pg/jpay/...` (**권장**) |
| **레거시 URL** | `/api/pay/jpay/...` |
| **브로커 시크릿** | `X-Icopay-Merchant-Broker-Secret` (본사 **강제** 시 필수) |
| **`pay_index`** | JPAY 측 결제 엔드포인트 — 플랫폼이 서버에서 대리 호출 |

키트 **PG 범위**를 JPAY(`JPAY` 또는 `ALL`)로 확인하세요.  
플랫폼이 JPAY에 넘기는 노티 URL은 기본 **미들웨어 수신 경로**를 사용합니다(본사 `tb_pg_agency` 의 `jpayNotifyIngressStyle=OPEN` 시 레거시 open 경로 — 본사 안내).

---

## 2. 본사로부터 전달받을 것

| # | 항목 |
|---|------|
| 1 | 업체코드 `compId` |
| 2 | 연동 키트 JSON(JPAY 바인딩·`pay_index` URL·노티 타깃 코드 등) |
| 3 | 브로커 시크릿 및 강제 여부 |
| 4 | 운영/스테이징 `publicApiBaseUrl` |
| 5 | 통보(웹훅) URL 요건 |

`pgBrokerBlocks` 중 **JPAY** 블록의 `brokerUrl` / `legacyUrl` 사용.

---

## 3. 권장 개발 순서 (JPAY)

**연동 방식을 먼저 선택하세요.**

| 방식 | 용도 | 문서 |
|------|------|------|
| **인라인 URL (권장 — 쇼핑몰)** | 가맹 서버 `prepare` → iframe `jpay-pay.html` | **`JPAY_URL결제_인라인API_배포가이드.md`** · 아래 §3-A |
| **서버 직접 sale** | 가맹 백엔드가 카드 필드를 받아 `POST .../jpay/sale` | 본 문서 §5 |

### 3.1 서버 직접 sale 순서

1. 키트·JPAY 바인딩(`JPAY` 계열 `pg_cd`, WEB·운영 Y) 확인  
2. **통보 URL** 스텁  
3. 브로커 시크릿 강제 시 헤더  
4. **`POST .../jpay/sale`** 연동 — 응답의 `status`, `redirectUrl` 처리  
5. JPAY 매뉴얼과 병행해 3DS·비동기 노티 시나리오 검증  
6. 본사 검수

---

## 3-A. 인라인 URL 결제 (iframe)

ChillPay URL 인라인과 **동일한 중계 패턴**입니다. 차이는 JPAY 전용 경로·결제창·본사 **API 중계형 INLINE** 설정입니다.

### 흐름

1. 가맹 서버: `POST {BASE}/api/middleware/v1/merchant/jpay/inline-checkout/prepare` (+ 브로커 시크릿)  
2. 응답 `sessionToken` → embed 스크립트 `data-session-token` 또는 `payUrl` iframe  
3. 고객: `jpay-pay.html` 에서 카드 입력 → ICOPAY가 `pay_index` 호출  
4. 완료: `postMessage` 이벤트 **`ICOPAY_INLINE_CHECKOUT`** + 가맹 웹훅(멱등)

### 키트 JSON

`merchantInlineCheckoutJpay` 블록에 `prepareUrl`, `embedScriptUrl`, `embedScriptExample`, `statusUrl` 이 포함됩니다.

### PHP 최소 예

```php
$prep = $api->prepareInlineCheckout(
    IcopayMerchantApi::VENDOR_JPAY, $orderNo, $amount, 'USD', $productName
);
echo $api->buildEmbedHtml(IcopayMerchantApi::VENDOR_JPAY, $prep['data']['sessionToken']);
```

전체 예: `merchant-api-samples/php/checkout_jpay.php`  
배포·본사 설정·체크리스트: **`JPAY_URL결제_인라인API_배포가이드.md`**

> **ChillPay·노티 전용** 가맹점 바인딩만으로는 JPAY 인라인을 사용할 수 없습니다. JPAY `pg_cd` 를 별도 등록해야 합니다.

---

## 4. 공통 기술 사양

- **HTTPS**, `Content-Type: application/json`, `Accept: application/json`  
- **`compId` 또는 `merchantId`** (GET 쿼리 / POST JSON)  
- 브로커 시크릿 강제 시 **403** / `BROKER_AUTH`  
- 응답 래퍼: `success`, `data`, `message`, `errorCode`  

JPAY 관련 `errorCode` 예: `NOT_FOUND`, `ORG_DISABLED`, `BROKER_AUTH`, `ORDER_DUP`, `ORDER_ALREADY_ATTEMPTED`, `ORDER_PENDING`, `BELOW_MIN_AMOUNT`, `INACTIVE_CARD`, `CARD_COOLDOWN*`, `JPAY_ERROR` 등.

**ICOPAY vs JPAY 거절:** `INACTIVE_CARD`·`CARD_COOLDOWN*` 는 JPAY `pay_index` **호출 전** ICOPAY 결제창에서 차단됩니다(승인번호 없음). JPAY 고위험(이메일/전화 불일치 등)은 JPAY 응답 원문으로 구분합니다. §4.7 참고.

**결제 실패 후 재시도:** 동일 `orderNo` 로 prepare·결제를 반복하면 `ORDER_DUP` 이 발생합니다. **새 orderNo** 를 발급한 뒤 prepare → embed 를 다시 호출하세요. 상세는 `JPAY_URL결제_인라인API_배포가이드.md` §4.6.

---

## 5. JPAY REST API 상세

**브로커 베이스:** `{BASE}/api/middleware/v1/pg/jpay`  
**레거시 베이스:** `{BASE}/api/pay/jpay`  
`{BASE}` = 키트 `publicApiBaseUrl`

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/sale` | JPAY `pay_index` 호출(본문 필드를 JPAY 폼 필드로 매핑) |

### POST `/sale` 본문(필수·주요)

| 필드 | 필수 | 설명 |
|------|------|------|
| `compId` / `merchantId` | 예 | 가맹 식별 |
| `orderNo` | 예 | 최대 64자(초과 시 잘림) |
| `amount` | 예 | 0 초과 |
| `payEmailAddress` | **예** | 구매자 이메일. JPAY 필수 |
| `payTelephone` | **예** | 구매자 전화(**로컬 번호**). 국가번호 `+82` 등은 제거. JPAY 필수 |
| `payCountryIsoCode2` | **예** | 구매자 국가 ISO 3166-1 alpha-2 (예: `KR`, `JP`, `TH` 대문자). JPAY 필수 |
| `currency` | 아니오 | ChillPay URL 일반형과 동일: 가맹 → 총판 → 본사 **기준통화** 우선, 없으면 본문 값, 최종 **`JPY`** |
| `payUrl` | 아니오 | 비우면 플랫폼 공개 베이스 |

> **인라인 prepare** (`.../jpay/inline-checkout/prepare`) 사용 시에는 루트 `buyer` 또는 `buyerPrefill` 객체 안에 `email` · `phone` · `countryIso2` 를 넣습니다. 통합 prepare (`.../checkout/prepare`)도 동일합니다. 자세한 표는 `가맹점_통합Checkout_API_연동파라미터_규격.md` **표 1.2** 참고.

**J-Pay Sale 매핑** ([공식 Sale API](https://docs.j-pay.net/docs/api/sale)): ICOPAY JSON → `pay_index` form.

| ICOPAY JSON | J-Pay 필드 |
|-------------|-----------|
| `payCardno` … `payCardcvv` | `pay_cardno` … |
| `payFirstname`, `payLastname`, `payEmailAddress` | `pay_firstname` … |
| `payTelephone`, `payCountryIsoCode2`, `payStreetAddress1`, `payCity`, `payPostcode` | 동명 snake_case |
| `payLanguage` / `langCode` | `pay_language` (`en`, `ko`, `zh` …) |
| (생략) | `system` 기본 `icopay`, 배송 주소는 청구지 복제 |

**응답 `status`:** `0` 성공 · `1` 3DS(`redirectUrl`) · `2` 실패 — J-Pay 문서와 동일.

카드·청구지 등: JSON camelCase → JPAY `pay_*` snake_case (플랫폼 매핑).

### 응답 `data` (참고)

| 키 | 설명 |
|----|------|
| `success` | 플랫폼 처리 성공 여부 |
| `status` | JPAY status(3DS 등은 JPAY 문서와 대조) |
| `redirectUrl` | 브라우저 리다이렉트 필요 시 |
| `orderNo` | 주문번호 |
| `rawResponse` | 원시 JSON |

최종 승인·노티는 **비동기**일 수 있으므로 **§6 통보 URL** 필수.

---

## 6. 가맹점 서버 통보(웹훅)

`merchantNotifyUrls` 에 등록한 HTTPS URL 준비, 멱등·로그·본사 보안 가이드 준수.

---

## 7. 구현 시 권장 사항

- JPAY·네트워크 지연을 고려한 타임아웃  
- `orderNo`·`status`·`rawResponse` 로그(민감정보 저장 금지)  
- 동일 주문 재시도 정책은 본사와 합의  

---

## 8. 검수·오픈 전 체크리스트

**인라인 URL:** `JPAY_URL결제_인라인API_배포가이드.md` §8 체크리스트 우선.

**서버 직접 sale:**

- [ ] 운영 베이스 URL만 사용  
- [ ] 브로커 시크릿(강제 시)  
- [ ] `sale` 후 `status` / `redirectUrl` / 고객 복귀 처리  
- [ ] 샌드박스·운영 MID 혼선 없음  
- [ ] 통보 URL·멱등  
- [ ] JPAY 공식 매뉴얼과 필드·서명 일치 재확인  

---

## 9. 부록 — 엔드포인트

**인라인 prepare (가맹 서버):**

```
POST {BASE}/api/middleware/v1/merchant/jpay/inline-checkout/prepare
GET  {BASE}/api/middleware/v1/merchant/jpay/inline-checkout/status?compId=&orderNo=
```

**서버 직접 sale:**

```
POST {BASE}/api/middleware/v1/pg/jpay/sale
```

**레거시:** `POST {BASE}/api/pay/jpay/sale`

---

**ChillPay 연동:** `가맹점_ChillPay_API_연동가이드.md` 참고.  
본 매뉴얼은 플랫폼 정책에 따라 갱신될 수 있습니다.
