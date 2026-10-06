# 가맹 ICOPAY 샌드박스 — 사용 안내

> **메뉴 표기는 관리자 왼쪽 메뉴에 표시된 이름만 사용합니다.**  
> (구명칭: 배포설정·가맹점 API 생성·API배포설정 등은 쓰지 않습니다.)

목적: 가맹 ↔ ICOPAY **API 통신·콜백 수신**만 검증. 실PG·NOTI·실정산 없음.

---

## 관련 메뉴 (현재 구성)

| 단계 | 왼쪽 메뉴 | 화면에서 할 일 |
|---|---|---|
| 키 발급 | **연동·배포** → **가맹 API 출시** → **키·문서** | 가맹 선택 후 **샌드박스 시크릿 발급** |
| URL·활성 | **업체관리** (가맹 상세) | 카드 **결제통보 URL (SANDBOX)** |
| 보관일 | **연동·배포** → **가맹 API 출시** → **공통설정** | 카드 **샌드박스 이력 보관** |
| 거래 확인 | **결제관리** → **샌드박스내역** | 시뮬 결제 목록 |
| 통보 확인 | **통보관리** → **샌드박스통보** | 전송·**재송부** |
| 가맹 문서 | **업체관리** → **가맹점API** | 연동 문서 조회(시크릿 재발급 불가) |

참고(라이브와 혼동 금지):

- **통보관리** → **결제통보 URL관리** / **결제통보 전송관리** → **라이브** 전용  
- 샌드박스 통보는 **샌드박스통보**만 사용합니다.

---

## 1. 관리자 준비

### 1-1. 샌드박스 시크릿

1. **연동·배포** → **가맹 API 출시** → **키·문서**
2. 가맹점 검색·선택
3. **샌드박스 시크릿 발급** 클릭  
   - 응답의 `brokerSecretPlain`을 가맹에 전달(한 번만 표시)
4. (라이브용) **브로커 시크릿 재발급**은 Live 키이며 샌드박스와 분리됩니다.

### 1-2. 활성 + SANDBOX 통보 URL

1. **업체관리**에서 해당 가맹 상세 열기  
2. 카드 **결제통보 URL (SANDBOX)**  
   - **샌드박스 사용** = 활성  
   - **URL Background (SANDBOX)** / **URL Result (SANDBOX)** 입력 후 저장  
3. 수정은 **총본사·본사** 관리자만 가능. 가맹 로그인은 조회만.

### 1-3. 보관 일수 (선택)

1. **연동·배포** → **가맹 API 출시** → **공통설정**  
2. **샌드박스 이력 보관** → **샌드박스 보관(일)** (기본 3)

---

## 2. 가맹 API (통신 테스트)

Live와 **동일 URL**, 헤더만 **샌드박스 시크릿**.

| 단계 | Method | 경로 |
|---|---|---|
| 준비 | POST | `/api/middleware/v1/merchant/checkout/prepare` |
| 승인(시뮬) | POST | `/api/middleware/v1/merchant/checkout/complete`  
| 또는 | POST | `/api/middleware/v1/merchant/checkout/sandbox/complete` |
| 조회 | GET | `/api/middleware/v1/merchant/checkout/status?compId=…&orderNo=…` |

헤더: `X-Icopay-Merchant-Broker-Secret: {샌드박스시크릿}`

성공 시 ICOPAY가 **결제통보 URL (SANDBOX)** 로만 JSON POST.  
라이브 **결제통보 URL**·**수신통보 URL**·NOTI는 사용하지 않습니다.

### 2-1. 가맹 서버 분기 (필수)

```
prepare 응답
  ├─ data.sandbox === true  또는  data.integrationMode === "SANDBOX"
  │     → payUrl / embed / 카드창을 열지 않음
  │     → POST {BASE}/api/middleware/v1/merchant/checkout/complete
  │        body: { "compId", "orderNo", "sessionToken" }
  │     → GET status 로 APPROVED 확인
  └─ 그 외 (라이브)
        → data.payUrl 또는 /v1/embed-checkout/{compId}
```

- 샌드박스 prepare에는 **payUrl이 없습니다.** payUrl이 비었다고 라이브 결제 HTML을 직접 열면 안 됩니다.
- `sessionToken`이 `sb_` 로 시작하면 샌드박스입니다. 중립 `/checkout/{compId}` 도 실결제창으로 보내지 않습니다.
- 실패 시뮬: complete body에 `"forceFail":"Y"` 또는 `"result":"FAIL"`.
- 라이브 시크릿으로 complete → `errorCode=SANDBOX_ONLY`.
- 샌드박스 비활성인데 샌드박스 시크릿 → `errorCode=SANDBOX_DISABLED`.

PHP 샘플: `IcopayMerchantApi::isSandboxPrepare` + `completeSandboxCheckout`.

---

## 3. 확인

- **결제관리** → **샌드박스내역** — 시뮬 승인만 여기 표시
- **결제관리** → **결제내역** — 라이브 시크릿·실PG만. 샌드박스 건은 **나오지 않는 것이 정상**
- **통보관리** → **샌드박스통보** (실패 시 행 선택 후 **재송부**)
- 보관: **연동·배포** → **가맹 API 출시** → **공통설정** → **샌드박스 이력 보관** (기본 3일 후 삭제)

---

## 4. 다른 가맹에 샌드박스 배포 (본사 체크리스트)

가맹마다 **별도**로 아래를 모두 마친 뒤에만 테스트 키를 전달합니다.

1. **연동·배포** → **가맹 API 출시** → **키·문서**에서 해당 가맹 선택  
   - 라이브 **브로커 시크릿**이 이미 있으면 유지  
   - **샌드박스 시크릿 발급** (한 번만 평문 표시) → **이 값만** 가맹 테스트 서버에 넣게 함
2. **업체관리** 상세 → 카드 **결제통보 URL (SANDBOX)**  
   - **샌드박스 사용 = 활성** (비활성이면 prepare가 `SANDBOX_DISABLED`)  
   - Background/Result = 가맹 **테스트 수신 URL만** (라이브 통보 URL 복사 금지)
3. 가맹에 전달할 것: `compId`, **샌드박스 시크릿**, API Base URL, 이 문서의 **2-1 분기**, 확인 메뉴(**샌드박스내역**)
4. 가맹에 **넣지 말 것:** 라이브 시크릿, 관리자 URL결제 링크, 실카드/테스트카드 번호(실PG용)
5. 가맹 서버에서 분기 구현 확인 후 1건 prepare+complete → 본사가 **샌드박스내역**에서 건수 확인
6. 라이브 오픈 시 가맹 서버 헤더를 **라이브 브로커 시크릿**으로 교체. 샌드박스 키를 운영 서버에 남기지 않음

---

## 금지

- 샌드박스 통보에 라이브 결제통보·수신통보 URL 사용  
- 샌드박스 건을 **결제내역**·**결제통보 전송관리**에 섞어 안내  
- 가맹점이 **결제통보 URL (SANDBOX)**·샌드박스 활성 여부를 수정  
- 라이브 시크릿으로 “테스트”하고 카드창이 열리면 실결제입니다. 샌드박스가 아닙니다  
- HQ 설정만 `SANDBOX`로 두고 API 헤더는 라이브 키를 쓰는 혼용  
- 관리자 **URL결제**로 샌드박스 검증 (URL결제는 라이브 경로)
