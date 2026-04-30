# ICOPAY 가맹점 JPAY API 개발 연동 매뉴얼

| 항목 | 내용 |
|------|------|
| **문서명** | 가맹점 JPAY API 개발 연동 매뉴얼 |
| **배포** | 플랫폼(본사) → 가맹점(또는 용역 개발사) |
| **대상 독자** | 백엔드·결제 연동 담당 |
| **적용 PG** | **JPAY** (ICOPAY 공개 API — `pay_index` 서버 사이드) |
| **문의** | 계약·MID·키·노티 URL·`jpayNotifyIngressStyle` 등은 **플랫폼(본사)** 관리자 채널 |

**목적:** 가맹점이 ICOPAY **JPAY 브로커/레거시 API**만 사용해 매출 요청·3DS 리턴을 처리하고, 통보(웹훅)를 받기 위한 개발·검수 기준을 정리합니다.  
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

1. 키트·JPAY 바인딩(`JPAY` 계열 `pg_cd`, 운영 Y) 확인  
2. **통보 URL** 스텁  
3. 브로커 시크릿 강제 시 헤더  
4. **`POST .../jpay/sale`** 연동 — 응답의 `status`, `redirectUrl` 처리  
5. JPAY 매뉴얼과 병행해 3DS·비동기 노티 시나리오 검증  
6. 본사 검수

---

## 4. 공통 기술 사양

- **HTTPS**, `Content-Type: application/json`, `Accept: application/json`  
- **`compId` 또는 `merchantId`** (GET 쿼리 / POST JSON)  
- 브로커 시크릿 강제 시 **403** / `BROKER_AUTH`  
- 응답 래퍼: `success`, `data`, `message`, `errorCode`  

JPAY 관련 `errorCode` 예: `NOT_FOUND`, `ORG_DISABLED`, `BROKER_AUTH`, `JPAY_ERROR` 등.

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
| `currency` | 아니오 | 기본 `USD` |
| `payUrl` | 아니오 | 비우면 플랫폼 공개 베이스 |

카드·청구지 등: JSON 키 예 `payCardno` → JPAY `pay_cardno` 등(플랫폼 매핑).

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

- [ ] 운영 베이스 URL만 사용  
- [ ] 브로커 시크릿(강제 시)  
- [ ] `sale` 후 `status` / `redirectUrl` / 고객 복귀 처리  
- [ ] 샌드박스·운영 MID 혼선 없음  
- [ ] 통보 URL·멱등  
- [ ] JPAY 공식 매뉴얼과 필드·서명 일치 재확인  

---

## 9. 부록 — 엔드포인트

```
POST {BASE}/api/middleware/v1/pg/jpay/sale
```

**레거시:** `POST {BASE}/api/pay/jpay/sale`

---

**ChillPay 연동:** `가맹점_ChillPay_API_연동가이드.md` 참고.  
본 매뉴얼은 플랫폼 정책에 따라 갱신될 수 있습니다.
