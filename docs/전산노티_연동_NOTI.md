# 전산 노티 수신 · 결제환경 (NOTI 참고)

> **무료 HTTPS(NOTI와 동일, Let's Encrypt):** [무료_SSL_NOTI와_동일_LetsEncrypt.md](./무료_SSL_NOTI와_동일_LetsEncrypt.md)

개발·검증 URL은 **http://localhost:8080** 기준입니다.

## 본사설정 > 전산노티·결제환경 (`/hq/notifyEnv`)

- **노티 수신 URL**: `POST http://localhost:8080/api/open/pg-notify/{ingressToken}`  
  - 이 전체 URL을 **ziobiz/NOTI** 의 전산노티대상(전사) 설정에 등록합니다.  
  - **토큰 재발급** 시 NOTI 쪽 URL도 반드시 같이 변경합니다.
- **공개 URL 베이스**: 운영 배포 후 `https://실제도메인` 형태로 저장하면, 안내되는 노티 URL이 고정됩니다. 비우면 API를 호출한 요청의 Host 기준으로 조합됩니다.
- **결제 후속조치 스위치**: 자동무효 / 이메일무효 / 자동환불 / 강제환불 — **Y**일 때만 관리자 화면 `결제내역` 그리드의 버튼이 API에서 허용됩니다 (NOTI 환경설정과 동일 역할).

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

## 로컬 vs 서버 환경

- **기능 개발·연동 테스트**: 로컬에서 Spring Boot + 관리자(site)로 충분합니다.  
- **NOTI·칠페이가 실제로 콜백을 보내야 할 때**: 인터넷에서 접근 가능한 **HTTPS 공개 URL**이 필요합니다.  
  - 선택지: (1) 배포 서버에 올린 뒤 `공개 URL 베이스` 설정 (2) 개발용 터널(ngrok 등)로 임시 공개.  
- **권장 순서**: 로컬에서 동작·플래그·MID 매핑 검증 → 스테이징/운영 서버에 배포 → NOTI에 운영 노티 URL 등록.

**서버로 전환할 때** 전체 절차(DB·JAR·FTP·노티 URL·config.js)는 **`docs/서버_환경_전환_체크리스트.md`** 를 따릅니다.  
운영에서 API가 별도 호스트(예: `api.*`)인 경우, 관리자 `config.js` / `PG_API_BASE` 와 **전산노티·결제환경의 공개 URL 베이스**를 같은 API 호스트로 맞춥니다.
