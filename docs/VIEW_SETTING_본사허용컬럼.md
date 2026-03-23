# VIEW SETTING · 본사별 허용 컬럼 (총본사 → 본사·하위)

## 개요

- **총본사(HEADQUARTERS)** 또는 **ADMIN**이 본사(**REGIONAL**) 단위·화면별로 “VIEW SETTING에서 사용자가 켤 수 있는 그리드 열”의 **상한**을 둡니다.
- 해당 본사에 소속된 사용자(본사 직원, 하위 총판·가맹점 등, 조직 트리 상 동일 **REGIONAL** 조상을 갖는 경우)는 **허용된 키 목록 안에서만** 개인 VIEW SETTING을 저장합니다.
- **정책 행이 없으면** 기존과 동일하게 제한 없음(전 열 선택 가능).

## DB

- 테이블: `tb_org_view_column_allowance` (`V22_org_view_column_allowance.sql`)
- UK: `(regional_org_code, page_url)`
- `allowed_keys_json`: JSON 문자열 배열, 예: `["orderNo","pgNm"]`

## API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/hq/orgViewColumnAllowance/regionalBranches` | 본사(REGIONAL) 목록 (총본사/ADMIN) |
| GET | `/api/hq/orgViewColumnAllowance?regionalOrgCode=&pageUrl=` | 해당 본사·화면 정책 조회 |
| POST | `/api/hq/orgViewColumnAllowance/save` | body: `regionalOrgCode`, `pageUrl`, `allowedKeysJson` |
| POST | `/api/hq/orgViewColumnAllowance/delete` | body: `regionalOrgCode`, `pageUrl` — 정책 행 삭제(제한 해제) |

## 사용자 VIEW SETTING

- `GET /api/user/viewSetting?pageUrl=...` 응답에 추가:
  - `columnAllowanceRestricted` (boolean): 정책 적용 여부
  - `allowedKeysJson` (string 또는 null): 허용 키 배열 JSON
  - `regionalScopeOrgCode` (string, 선택): 판단에 쓰인 본사 코드
- 저장 시 서버가 **허용 목록 밖 키는 자동 제거**합니다.

## 전산 UI

- **본사설정 → 본사별 노출설정** (`/hq/orgViewColumnAllowance`): 본사·화면 선택 후, 해당 본사에 VIEW SETTING으로 **노출할 그리드 열**을 체크해 저장.
- 지원 화면(초기): **결제내역** (`/calc/payList`), **업체관리** (`/comp/compMngTree`). 추가 시 동일 API에 `pageUrl`만 맞추면 됩니다.

## 본사(REGIONAL) 판별

- 로그인 사용자의 조직: 가맹점 프로필의 `OrgUnit` → 없으면 `tb_user.org_unit_code` → ADMIN이면 총본사.
- 조직에서 상위로 올라가며 첫 **REGIONAL**의 `code`가 스코프. **HEADQUARTERS** 소속이면 제한 없음.

## 로컬 확인

- http://localhost:8080 → 본사설정 → VIEW허용컬럼(본사)
