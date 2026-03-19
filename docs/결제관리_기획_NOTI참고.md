# 결제관리 화면 정의 (NOTI 참고 · PG UI 유지)

참고 저장소: **github.com/ziobiz/NOTI** (표기 오타: ziobizm → ziobiz)  
개발·문서 URL 표기: **http://localhost:8080** 기준 (워크스페이스 규칙).

- 전사 노티 URL·MID/루트 매핑: **`docs/전산노티_연동_NOTI.md`**
- 후속조치: **`POST /api/calc/payAction`** + 본사설정 > 전산노티·결제환경

## 원칙

| 구분 | 내용 |
|------|------|
| **데이터·연동** | NOTI(칠페이 API, 노티 수신·저장, 로그 분석)와 동일 **개념·필드**를 참고 |
| **UI** | **PG의 통합 결제내역 그리드**(검색·요약·컬럼)만 사용. NOTI 화면 레이아웃·스타일을 가져오지 않음 |

## 결제관리 메뉴 (9개)

| 순서 | 화면명 | URL | 정의 |
|------|--------|-----|------|
| 1 | 결제내역 | `/calc/payList` | **통합 결제내역**. 칠페이 API로 가져온 거래·기타 출처 전부. NOTI의 **피지거래내역**과 동일 성격(필드 수준 참고). |
| 2 | 노티내역 | `/calc/payNotiList` | 전산 노티 수신·로그분석으로 적재. NOTI **종합거래의 노티거래내역**과 동일. `origin=NOTI` 등으로 구분. |
| 3 | 성공내역 | `/calc/paySuccessList` | 통합 결제내역에서 **승인 성공**만 간추림. |
| 4 | 실패내역 | `/calc/payFailList` | 통합에서 **실패·거절**만 간추림. |
| 5 | 환불내역 | `/calc/payRefundList` | 통합에서 **환불**만. |
| 6 | 강제환불 | `/calc/payForceRefundList` | 통합에서 **강제환불**만. |
| 7 | 취소내역 | `/calc/payCancelList` | 통합에서 **취소**만. |
| 8 | 상계취소내역 | `/calc/offsetCancList` | **승인 성공을 제외**한 전 건(실패·환불·강제환불·취소·기타). 정산 **상계** 판단·빈도 분석용. |
| 9 | URL결제내역 | `/pay/easyPay` | 가맹점 API연동 노티 외, **플랫폼이 칠페이 결제 API로 발급한 결제수소(URL)** 로 발생한 **전 상태**(성공·실패·환불·취소 등). 통합에도 나오나 `origin=URL` 만 필터. |

## API

`GET /api/calc/payList?payListVariant=...`

| payListVariant | 설명 |
|----------------|------|
| `INTEGRATED` | 통합 결제내역 (미지정 시 동일) |
| `NOTI` | 노티 적재 건 (`origin=NOTI`) |
| `SUCCESS` / `FAIL` / `REFUND` / `FORCE_REFUND` / `CANCEL` | 상태 필터 |
| `OFFSET_CANCEL` | `status`가 승인성공(`10`)이 **아닌** 건 (NULL 포함) |
| `URL_PAY` | `origin=URL` |

## 상태 코드 (로컬·매핑용)

운영 시 ChillPay·NOTI 매핑표로 조정.

- `10` 승인 성공  
- `20` 취소  
- `30` 환불  
- `31` 강제환불  
- `F0`, `99` 실패 등  

## 차후

- ChillPay 거래 조회 API로 통합 결제내역 실데이터 동기화  
- 노티 인바운드 → `pg_trnsctn` 적재 및 `origin` 일관화  
- 상계취소 집계 → 정산 모듈 연계  
