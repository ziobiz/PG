# JPAY 연동 — 키트 발급 · 샌드박스 검수 절차

| 항목 | 내용 |
|------|------|
| **대상** | ICOPAY 본사 운영자 · 가맹 연동 담당 |
| **전제** | JPAY : ICOPAY **1:1**, ICOPAY : 가맹 **1:N** (가맹은 ICOPAY API만 사용) |
| **J-Pay 규격** | [Sale API](https://docs.j-pay.net/docs/api/sale) · `POST …/pay_index` |
| **관련 문서** | `JPAY_URL결제_인라인API_배포가이드.md` · `가맹점_JPAY_API_연동가이드.md` |

---

## 0. 준비 — 검수용 compId 정하기

예: `6000000028` (실제 운영·스테이징 업체코드로 교체)

체크리스트에 **compId를 한 줄로 기록**해 두고 아래 단계마다 동일 코드를 사용합니다.

---

## 1. 본사(HQ) 선행 설정

관리자: `https://icopay.co.kr`

### 1.1 결제로직설정

| 항목 | 값 |
|------|-----|
| API 중계형 INLINE 제공 | **Y** |
| API 중계형 기본 방식 | **INLINE** |

### 1.2 API연동설정 — JPAY 행

| 항목 | 값 |
|------|-----|
| PG 코드 | JPAY 계열 (예: `JPAY`, `JPAY URL`) |
| **연동용도 URL결제** | **Y** |
| pay_index | 샌드박스: `https://sandbox.j-pay.net/pay_index` |
| ApiKey | J-Pay 샌드박스 키 (본사 보관) |
| 샌드박스 | **Y** |
| 사용·운영 | 테스트 시 **Y** (바인딩과 함께) |

J-Pay 문서 샌드박스 Sale 테스트 계정(참고):

| 항목 | 값 |
|------|-----|
| Merchant ID | `10151` |
| ApiKey | `3p17o32a83gge5tho3vpv0m61nvhtye9` |

> 운영 MID는 J-Pay Production configuration 후 본사가 `tb_pg_agency`·가맹 바인딩에 반영합니다.

### 1.3 가맹점 → 결제대행사 (compId)

| 항목 | 값 |
|------|-----|
| PG | JPAY 계열 |
| 결제수단 | WEB |
| MID | 샌드박스 `10151` (또는 J-Pay 부여 MID) |
| 운영 | **Y** |
| 활성 | **Y** |

ChillPay·노티 전용 바인딩만 있으면 **JPAY 행을 추가**합니다.

### 1.4 가맹점 → 업체정보

| 항목 | 값 |
|------|-----|
| 웹결제 | **Y** |
| **JPAY 수신통보 URL** | Notify → `pay_notifyurl`, Callback → `pay_callbackurl` (노티미들웨어 가맹 URL) |

### 1.5 배포설정 → publicApiBaseUrl

| 항목 | 예 |
|------|-----|
| publicApiBaseUrl | `https://api.icopay.co.kr` |

노티·callback·embed·`jpay-pay.html` 이 이 도메인 기준으로 동작해야 합니다.

### 1.6 J-Pay 측

- [ ] ICOPAY 서버(VPS) **출발 IP** J-Pay 화이트리스트 등록
- [ ] 샌드박스 Merchant `10151` 사용 가능 확인

---

## 2. 가맹점 API 키트 발급

**메뉴:** 배포설정 → **가맹점 API 생성** (`/hq/merchantApiGenerate`)

| 단계 | 작업 |
|------|------|
| 1 | 업체코드 `{compId}` 입력 또는 목록에서 **선택** |
| 2 | PG 범위 **JPAY** (또는 ALL) |
| 3 | **연동 키트 JSON 불러오기** |
| 4 | (권장) **브로커 시크릿 재발급** → `brokerSecretPlain` **1회** 복사 → 안전 저장 |
| 5 | (권장) **브로커 시크릿 강제** ON |
| 6 | JSON 전체를 가맹·개발 담당에게 전달 |

### 키트에서 확인할 블록

```json
"merchantInlineCheckoutJpay": {
  "prepareUrl": "…/api/middleware/v1/merchant/jpay/inline-checkout/prepare",
  "embedScriptUrl": "…/v1/embed-jpay-pay/{compId}",
  "statusUrl": "…/inline-checkout/status?compId=…&orderNo=…"
}
```

가맹 PHP 샘플 베이스: `{publicApiBaseUrl}/merchant-api-samples/`

---

## 3. 가맹(또는 본사 대리) 샌드박스 — 인라인 1건

### 3.1 PHP 샘플 (가장 빠름)

1. `merchant-api-samples/php/icopay_config.example.php` → `icopay_config.php`
2. 설정:

```php
return [
    'api_base_url'  => 'https://api.icopay.co.kr',
    'comp_id'       => '{compId}',
    'broker_secret' => '{발급한 시크릿}',
    'default_vendor'=> 'jpay',
];
```

3. 서버에 `checkout_jpay.php` 배치 (document root)
4. 브라우저에서 주문번호·금액 입력 → **결제하기**

### 3.2 prepare 수동 호출 (curl)

```bash
curl -sS -X POST "https://api.icopay.co.kr/api/middleware/v1/merchant/jpay/inline-checkout/prepare" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "X-Icopay-Merchant-Broker-Secret: {broker_secret}" \
  -d '{
    "compId": "{compId}",
    "orderNo": "JTEST001",
    "amount": 0.01,
    "currency": "USD",
    "productName": "Sandbox test",
    "lang": "ENG"
  }'
```

성공 시 `data.sessionToken`, `data.payUrl`, `data.embedScriptUrl` 확인.

### 3.3 결제창 · 테스트 카드

`payUrl` 또는 embed로 `jpay-pay.html` 로드 후:

| 카드 | MM/YYYY | CVV | 기대 |
|------|---------|-----|------|
| 4242424242424242 | 12 / 2027 | 123 | status **0** 즉시 성공 |
| 4000000000001018 | 12 / 2027 | 123 | status **2** 실패 |
| 4141414141414141 | 12 / 2027 | 123 | status **1** → 3DS URL |

청구 필드(전화·국가 ISO2·주소·도시·우편번호)를 입력합니다.

### 3.4 완료 확인 (3중)

| # | 확인 |
|---|------|
| 1 | 브라우저 `postMessage` `ICOPAY_INLINE_CHECKOUT` · `phase=finished` |
| 2 | ICOPAY 관리 — **결제내역** JPAY 건 |
| 3 | (선택) `GET …/inline-checkout/status?compId={compId}&orderNo=JTEST001` + 브로커 시크릿 → `paymentStatus=PAID` |

```bash
curl -sS "https://api.icopay.co.kr/api/middleware/v1/merchant/jpay/inline-checkout/status?compId={compId}&orderNo=JTEST001" \
  -H "X-Icopay-Merchant-Broker-Secret: {broker_secret}"
```

---

## 4. 자주 나는 오류 (샌드박스)

| 증상 / errorCode | 조치 |
|------------------|------|
| `URL_PAYMENT_PG_MISSING` | 가맹 JPAY WEB 운영 바인딩 |
| `WEB_PAYMENT_DISABLED` | 업체정보 웹결제 Y |
| `INLINE_NOT_ENABLED` | 결제로직 API 중계 INLINE Y |
| `BROKER_AUTH` | 시크릿 헤더·강제 여부·compId |
| J-Pay HTTP 오류 / 거절 | ApiKey·MID·IP 화이트리스트·Sale 필수 필드 |
| 3DS 후 복귀 없음 | `pay_callbackurl`(rsJpay ingress) · JAR 최신 |

---

## 5. 검수 통과 후 — 운영 전환

| # | 작업 |
|---|------|
| 1 | `tb_pg_agency` JPAY 행: 샌드박스 **N**, 운영 `pay_index` `https://www.j-pay.net/pay_index` |
| 2 | 가맹 바인딩 MID → **운영 MID** 로 변경 |
| 3 | J-Pay 운영 ApiKey·IP 등록 |
| 4 | 소액 실거래 1건 → 노티·가맹 웹훅 멱등 확인 |
| 5 | 가맹에 운영 `publicApiBaseUrl`·키트·`JPAY_URL결제_인라인API_배포가이드.md` PDF 재전달 |

---

## 6. 가맹 전달 패키지 (체크)

- [ ] 연동 키트 JSON (JPAY, `merchantInlineCheckoutJpay`)
- [ ] 브로커 시크릿 (서버만, 1회 안전 전달)
- [ ] `JPAY_URL결제_인라인API_배포가이드.md` / PDF
- [ ] 샘플 URL: `{publicApiBaseUrl}/merchant-api-samples/php/checkout_jpay.php`
- [ ] 가맹 웹훅 URL 등록 (`merchantNotifyUrls`)

---

## 개정 이력

| 버전 | 일자 | 요약 |
|------|------|------|
| 1.0 | 2026-05-23 | JPAY 키트 발급·샌드박스 검수 절차 최초 작성 |
