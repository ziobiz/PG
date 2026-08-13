# 전산 노티 수신 · 결제환경 (NOTI 참고)

**노티(서버→서버 Notify)** 와 **URL 결제(브라우저 결제·결과 페이지)** 는 연동 목적·필수 필드·응답 규칙이 다릅니다. 혼동하지 말 것. 요약·NOTI 재전송 계약은 [NOTI_노티재전송_Cursor개발요청.md](./NOTI_노티재전송_Cursor개발요청.md) 의 「노티 연동 vs URL 결제 연동」절을 참고합니다.

> **무료 HTTPS(NOTI와 동일, Let's Encrypt):** [무료_SSL_NOTI와_동일_LetsEncrypt.md](./무료_SSL_NOTI와_동일_LetsEncrypt.md)

개발·검증 URL은 **http://localhost:8080** 기준입니다.

## 본사설정 > 전산노티·결제환경 (`/hq/notifyEnv`)

- **노티 수신 URL**: `POST http://localhost:8080/api/open/pg-notify/{ingressToken}`  
  - 이 전체 URL을 **ziobiz/NOTI** 의 전산노티대상(전사) 설정에 등록합니다.  
  - **토큰 재발급** 시 NOTI 쪽 URL도 반드시 같이 변경합니다.
- **공개 URL 베이스**: 운영 배포 후 `https://실제도메인` 형태로 저장하면, 안내되는 노티 URL이 고정됩니다. 비우면 API를 호출한 요청의 Host 기준으로 조합됩니다.
- **로그인·OTP·비밀번호·관리담당 정책**은 본사설정 **사용자설정** (`/hq/userSettings`)으로 옮겼습니다. DB는 여전히 `tb_hq_notify_env_config`입니다.

## 본사설정 > 전산설정관리 (`/hq/ledgerSysSettings`)

- **결제 후속조치 (NOTI 환경설정 대응)**: 자동무효 / 이메일무효 / 자동환불 / 강제환불 및 자동무효 지연(시간) — **Y**일 때만 관리자 `결제내역` 그리드의 해당 버튼이 API에서 허용됩니다. 저장 값은 **`tb_hq_notify_env_config`** 에 반영됩니다 (노티구성설정 화면과 동일 테이블).

## 가맹점 구분: MID + 루트번호

노티 본문(JSON 또는 `x-www-form-urlencoded`)에서 다음 키를 읽습니다.

| 의미 | 허용 키 (대소문자 무시 일부) |
|------|------------------------------|
| MID | `mid`, `merchantId`, `MerchantCode`, `merchant_code`, `mchtId` |
| 루트번호 | `rootNo`, `root_no`, `routeNo`, `route_no`, `root` |
| 합침 | `midRoot`, `mid_root` → `MID_루트` 형태 (마지막 `_` 기준 분리) |

매칭 순서:

1. `tb_merchant_pg_binding` 에서 `mid` 일치 행을 조회 (운영대상 `operational_yn=Y` 우선).
2. 루트번호가 오면 `root_no` 가 일치하는 연동을 우선.
3. 없으면 `root_no` 가 비어 있는 연동으로 폴백.

가맹점 **업체정보 > 결제대행사** 테이블에 **루트번호** 열을 채워 두면 노티로 가맹점이 정확히 구분됩니다.

## 수신 저장

수신 내역은 `tb_pg_notify_inbound` 에 원문·매핑된 `merchant_id`(업체코드)·`process_status` 로 적재됩니다.  
이후 ChillPay/NOTI 필드에 맞춰 `pg_trnsctn` 자동 반영·중복 제거 등을 확장할 수 있습니다.

## 노티미들웨어 → PG 중계(관리자 무효·취소·환불)

칠페이 노티가 PG 로 직접 오지 않는 경우(예: **관리자 무효**만 노티미들웨어 DB에 반영된 경우)에도 PG 결제내역(`pg_trnsctn`)과 맞추려면, **노티미들웨어 서버**가 아래 URL로 **추가 POST** 하면 됩니다.  
기존 노티 URL과 **동일**한 `ingressToken`, **동일**한 `app.pg-notify` IP·HMAC 정책을 적용합니다. **HMAC 은 이 JSON 원문** 기준으로 계산합니다.

- **URL**: `POST {공개베이스}/api/open/pg-notify/{ingressToken}/noti-middleware-relay`  
  - `Content-Type: application/json`
  - 선택: 경로에 `/{targetCode}` 를 넣을 수 있음 — `POST .../pg-notify/{token}/{cb|rs}/noti-middleware-relay` (채널은 본사설정 노티 대상 코드와 동일)

**요청 본문 예시 (무효)**

```json
{
  "eventType": "VOID",
  "transactionId": "31098397",
  "merchantCode": "M035594",
  "routeNo": "1",
  "orderNo": "20260407172417973120",
  "compId": "6000000017",
  "reason": "관리자 무효 요청"
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `eventType` | `internalStatusCode` 없을 때 필수 | `VOID` / `CANCEL` / `REFUND` (대소문자 무시) |
| `transactionId` | 예 | ChillPay `TransactionId` |
| `merchantCode` | 예 | MID (`MerchantCode`) |
| `routeNo` | 아니오 | `RouteNo` |
| `orderNo` | 아니오 | `OrderNo` |
| `compId` | 아니오 | 업체코드 — 본문에 `icopayCompId=` 로 넣어 가맹점 매칭 보강 |
| `reason` | 아니오 | 사유 문구 |
| `internalStatusCode` | 아니오 | 직접 ICOPAY 코드 지정 시 `21`·`20`·`30` 등 (`eventType` 대신 사용 가능) |

서버는 내부에서 ChillPay 호환 JSON 으로 합성한 뒤, 기존 노티 수신과 동일 파이프로 `pg_trnsctn` 에 반영합니다.

## ElementPay (THB) — 노티미들웨어 경유 (가맹 무수정·본사 PG 전환)

ElementPay는 **PG가 ICOPAY를 직접 호출하지 않습니다.** 외부 NOTI가 중간에서 수신·재전송합니다.  
가맹점은 Callback/Result URL을 바꾸지 않고, 본사가 결제대행사만 전환할 수 있습니다.

| 단계 | 설정 |
|------|------|
| 1. ElementPay Cabinet | **Webhooks** = `https://noti.icopay.net/noti/elementpay` (**고정 1개**, 가맹 URL 금지) |
| 2. NOTI → ICOPAY | `POST {공개베이스}/api/middleware/notify/v1/pg-notify/{ingressToken}/ELEMENTPAY` |
| 3. ICOPAY 처리 | `check`/`pay` → ElementPay JSON+HMAC / `payment.*` → `pg_trnsctn` |
| 4. NOTI → 가맹 Callback | Webhook 후 가맹 **callbackUrl** 릴레이 (`raw`/`json`/`form`) |
| 5. 브라우저 Result | ICOPAY `_successUrl` 등 = `https://noti.icopay.net/noti/result/elementpay?order=…&compId=…&merchantId=…` → NOTI가 가맹 **resultUrl**로 전달 |

**NOTI 가맹 매칭 순서:** ① 쿼리/바디 `compId`·`merchantId` → ② (선택) `icopayOrderLookupUrl` Comp-Id 조회 → ③ 최근 `elementpay/webhook` 로그 order → 실패 시 폴백 페이지(타 가맹 오전달 없음).

**운영관리 노티생성:** PG=ElementPay → NOTI `pgKind=elementpay` 등록. 수신통보에 Webhook·Result 고정 URL 자동 반영.  
**전산 대상(THB):** 본사설정 → 결제환경 → 「전산 대상 매핑」에 **THB 전산 대상 ID**를 지정하면, 기준화폐 THB 가맹 노티생성 시 드롭다운·자동 제안에 노출됩니다(JPY/USD와 동일).

**NOTI 필수:** `check`·`pay` 응답 본문 `{response,hash}` **무변환** 패스스루.  
**Result 입구:** [`docs/NOTI_ElementPay_Result입구_개발요청.md`](./NOTI_ElementPay_Result입구_개발요청.md) — NOTI `/noti/result/elementpay` (ICOPAY V2.97 연동).

**가맹 비식별:**

- ElementPay Merchant Key·Secret 은 **ICOPAY 본사 집계 1세트**만 (`tb_pg_agency` `ELEMENTPAY`).
- 결제 API·웹훅에 가맹 업체코드 미전송 — `order` 로 복원.
- EP에 가맹 쇼핑몰 URL **금지** (NOTI Result 입구만).

배포설정 `ELEMENTPAY` 행: `integ_noti_yn=Y`, `integ_url_pay_yn=Y` (마이그레이션 `V229_elementpay_pg_agency.sql`).

## 로컬 vs 서버 환경

- **기능 개발·연동 테스트**: 로컬에서 Spring Boot + 관리자(site)로 충분합니다.  
- **NOTI·칠페이가 실제로 콜백을 보내야 할 때**: 인터넷에서 접근 가능한 **HTTPS 공개 URL**이 필요합니다.  
  - 선택지: (1) 배포 서버에 올린 뒤 `공개 URL 베이스` 설정 (2) 개발용 터널(ngrok 등)로 임시 공개.  
- **권장 순서**: 로컬에서 동작·플래그·MID 매핑 검증 → 스테이징/운영 서버에 배포 → NOTI에 운영 노티 URL 등록.

**서버로 전환할 때** 전체 절차(DB·JAR·FTP·노티 URL·config.js)는 **`docs/서버_환경_전환_체크리스트.md`** 를 따릅니다.  
운영에서 API가 별도 호스트(예: `api.*`)인 경우, 관리자 `config.js` / `PG_API_BASE` 와 **전산노티·결제환경의 공개 URL 베이스**를 같은 API 호스트로 맞춥니다.
