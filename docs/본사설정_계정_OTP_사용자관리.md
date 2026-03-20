# 본사설정 — 계정·OTP·사용자관리 (ziobiz/NOTI 대응)

## 총본사 업체정보조회 폼

- **HEADQUARTERS**일 때 다음 항목은 화면에서 숨깁니다: 사용여부, 로그인ID, 사업형태, 취급물품, 계좌은행, 이체수수료(원), 계좌번호, 예금주, 기준화폐1~3.
- 구현: `site/js/screens.js` 업체정보조회(`myCompMng`) 필드에 `hideForHeadquarters`, 렌더러에 `comp-info-hide-if-hq`, `app.js`에서 상세 로드 후 `applyCompInfoHeadquartersVisibility` 호출.

## 전산노티·결제환경 — OTP 정책

- **OTP 사용 필수** (`otpRequiredYn` Y/N): `tb_hq_notify_env_config.otp_required_yn`, 본사설정 **전산노티·결제환경** 화면 하단 카드에서 저장.
- 로그인/등록 단계의 실제 OTP 검증(TOTP/SMS 등)은 이후 연동 예정이며, 정책 값은 API·DB에 먼저 반영됩니다.

## 계정·업체접근

- 메뉴: **본사설정 → 계정·업체접근** (`/hq/accountMng`).
- 사용자(로그인 ID)별로 허용할 **업체코드**를 지정합니다 (`tb_user_comp_access`).
- API: `GET/POST /api/hq/accountAccess`, `POST .../add`, `DELETE .../{id}`.

## 사용자관리 그리드

- 컬럼: 번호, 사용자ID, 사용자명, 소속업체코드, **권한그룹**, 역할, **OTP**, 사용여부.
- **권한그룹** 정렬 후 같은 그룹의 첫 행에 왼쪽 강조선(`tr-user-group-start`) — 참고 UI(거래 목록의 고객별 그룹)와 유사.
- `tb_user`: `org_unit_code`, `permission_group_nm`, `otp_registered_yn` (신규 사용자 생성 시 업체코드·기본 권한그룹 반영).

## DB 마이그레이션 (PostgreSQL)

- `pg-app/src/main/resources/db/V12_user_hq_otp_account_access.sql` 참고. H2 dev는 `ddl-auto`로 엔티티 반영.
