# 본사설정 — 계정·OTP·사용자관리

> 라이브 반영: 플랫폼 **V4.25** 기준 (사용자관리 조직·설정권한 · 메뉴얼 반영)  
> 관리자: https://icopay.co.kr/ → **사용자관리 → 사용자관리** (`/user/userMng`)

---

## 총본사 업체정보조회 폼

- **HEADQUARTERS**일 때 다음 항목은 화면에서 숨깁니다: 사용여부, 로그인ID, 사업형태, 취급물품, 계좌은행, 이체수수료(원), 계좌번호, 예금주, 기준화폐1~3.
- 구현: `site/js/screens.js` 업체정보조회(`myCompMng`) 필드에 `hideForHeadquarters`, 렌더러에 `comp-info-hide-if-hq`, `app.js`에서 상세 로드 후 `applyCompInfoHeadquartersVisibility` 호출.

## 본사설정 — 사용자설정 — 로그인·OTP 정책

- 메뉴: **본사정책 → 접근·권한 → 사용자설정** (`/hq/userSettings`).
- **OTP 사용 필수** (`otpRequiredYn` Y/N) 등: `tb_hq_notify_env_config`. UI는 이 화면에서 편집하며, 저장 API는 `POST /api/hq/notifyEnv/save`에 해당 필드만 전달합니다.
- **SUPERVISOR** 부여·해제는 이 화면에서만 가능합니다(사용자관리 목록에서는 변경 불가).

## 계정·업체접근

- 메뉴: **본사정책 → 접근·권한 → 계정·업체접근** (`/hq/accountMng`).
- 사용자(로그인 ID)별로 허용할 **업체코드**를 지정합니다 (`tb_user_comp_access`).
- API: `GET/POST /api/hq/accountAccess`, `POST .../add`, `DELETE .../{id}`.

---

## 사용자관리 그리드 (`/user/userMng`)

### 열 구성 (기본)

| 열 | 설명 |
|----|------|
| No. | 번호 (고정) |
| 업체코드 · 업체명 | 소속 조직 (고정) |
| 사용자ID* · 사용자명* · 연락처* | 계정 기본 정보 |
| **조직** | 조직도 단계: 총본사·본사·총판·지사·대리점·영업점·가맹점 |
| **권한그룹*** | **수정용**. 담당자(ASSISTANT)는 셀렉트로 변경. 대표(REPRESENTATIVE)·SUPERVISOR는 조회 전용 |
| **설정권한** | **현재 적용 표시**(수정 아님). 권한그룹과 동일 의미의 단축 표기 |
| 비밀번호 · OTP | 등록/미등록 · 초기화(권한 시) |
| 사용여부* · 전환사유 · 삭제 | 상태·임시행 삭제 |

- **VIEW SETTING**으로 열 표시·순서를 조정할 수 있습니다. 고정열: No. · 업체코드 · 업체명 · 사용자ID.
- 시스템 계정 구분(`AppUser.role`의 USER/ADMIN)은 목록에 쓰지 않습니다. 예전에 「역할」이 모두 USER로 보이던 표기는 제거되었습니다.

### 권한그룹(수정) ↔ 설정권한(표시)

| 권한그룹* (저장·코드) | 설정권한 (화면 단축) |
|----------------------|---------------------|
| 감독담당 (SUPERVISOR) | 감독 |
| 관리담당 (MANAGER) | 관리 |
| 운영담당 (OPERATOR) | 운영 |
| 정산담당 (SETTLEMENT) | 정산 |
| 기술담당 (TECH) | 기술 |
| 대표 | 대표 |
| 업체사용자 | 일반 |
| CHATBOT / 챗봇관리자 | 챗봇 |

- DB `permission_group_nm`·`assistant_role_type`은 기존 긴 이름·코드를 유지하고, **목록 표시만** 단축합니다.
- 권한그룹을 바꾸면 같은 행의 **설정권한**이 즉시 맞춰집니다(저장 전 미리보기).

### 운영 팁

1. 총판과 총본사 사용자를 구분할 때는 **조직** 열을 확인합니다(업체코드만으로는 단계가 안 보입니다).
2. 메뉴 접근 범위는 본사정책 **접근·권한**·담당자 권한그룹별 메뉴와 함께 적용됩니다.
3. 비밀번호·OTP 초기화는 **관리(MANAGER)** 등 허용된 계정만 가능합니다.

### 데이터

- `tb_user`: `org_unit_code`, `permission_group_nm`, `assistant_role_type`, `otp_registered_yn`, `user_type` 등.
- 목록 API: `UserListService` — `orgLevel` / `orgLevelNm` / `roleNm`(단축 설정권한).

## DB 마이그레이션

- 계정·OTP·업체접근 초기: `pg-app/src/main/resources/db/V12_user_hq_otp_account_access.sql` 참고.
- 사용자관리 조직·설정권한 표시는 **스키마 변경 없음**(표시·API 필드만).
