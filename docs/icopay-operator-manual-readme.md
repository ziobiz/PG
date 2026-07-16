# ICOPAY 운영자 메뉴얼 (HTML · PDF)

역할별 **3종 × 5개 언어** HTML 소스와 PDF 내보내기 스크립트입니다.

## ICOPAY V2.2 (라이브 · 2026-07-16)

- 좌측 **본사정책 / 연동·배포** 허브: [`관리자_메뉴_운영_가이드_V2.md`](./관리자_메뉴_운영_가이드_V2.md)
- **라이브 버전:** **V2.2** · 마이너: V2.1, V2.2 …
- **버전 히스토리 UI:** 본사정책 → 플랫폼 → **업데이트 내용** (탭 전환해도 동일 허브)
- **결제대행사(PG) 추가·신규 연동 시:** 반드시 마이너 **+0.1** (`icopay-platform-release-notes.js`)
- 메뉴·URL 매핑: [`메뉴_분석_및_연결구조.md`](./메뉴_분석_및_연결구조.md)
- LEGACY 즉시 복원: `https://icopay.co.kr/?menu=legacy`
- HTML/PDF 본문의 「본사설정」「배포설정」 절은 **본사정책·연동·배포** 허브명으로 읽으면 됩니다.

## 명명 원칙 · 버전 (V2.0 · 2026-07)

ICOPAY는 단일 API 뒤에서 여러 결제대행사를 통합합니다.

| 구분 | 표기 |
|------|------|
| **통합 기능** (URL결제, checkout, 통합거래, 가맹 API 등) | ChillPay/JPAY **미사용** → ICOPAY·통합 checkout 등 중립 용어 |
| **JPAY 전용** (구독 API, jpay-pay 입력 필드, JPAY 노티 Provision 등) | 「**JPAY 전용**」으로 명시 |
| **ChillPay 전용** (MD5 Secret, CHILLPAY 계열 PG코드 병합 등) | 해당 기술 설명에만 PG명 사용 |

운영자·가맹점 HTML 매뉴얼 15종 + 관리 UI 문자열(5개 언어)에 동일 원칙을 적용합니다.

## 역할 구분

| 역할 | HTML 접두 | PDF 파일명 예시 |
|------|-----------|-----------------|
| **총본사** (Super Admin) | `icopay-operator-manual*.html` | `ICOPAY Super Admin Operation Manual_KR_*_V2.0.pdf` |
| **본사** (HQ Admin) | `icopay-operator-manual-hq*.html` | `ICOPAY Headquarters Operation Manual_KR_*_V2.0.pdf` |
| **총판** (Distributor) | `icopay-operator-manual-dist*.html` | `ICOPAY Distributor Operation Manual_KR_*_V2.0.pdf` |
| **가맹점** (Merchant) | `icopay-merchant-manual*.html` | `ICOPAY Merchant Operation Manual_KR_*_V2.0.pdf` |

**본사·총판** 기능·권한은 동일합니다. 본사용·총판용 PDF를 **각각** 배포합니다.

## PDF 생성

```bash
cd scripts
npm install
node operator-manual-pdf/gen.mjs hq
```

- `hq` — 본사 메뉴얼 5개 언어 (기본)
- `dist` — 총판 메뉴얼
- `super` — 총본사 메뉴얼
- `all` — 15개 전체

출력: `docs/manual-pdf/`

## 명명 원칙 (V2.0 · 2026-07)

ICOPAY는 **단일 API** 뒤에서 여러 결제대행사(PG)를 통합하는 플랫폼입니다. HTML/PDF 매뉴얼 본문에서는 다음을 따릅니다.

| 구분 | 규칙 |
|------|------|
| 통합 기능 | 체크아웃·URL결제·통합거래·웹결제 등 → **ICOPAY 중립 용어** (ChillPay/JPAY를 일반 제목·설명에 쓰지 않음) |
| JPAY | **「JPAY 전용」** 표기가 있는 기능 설명에만 사용 |
| ChillPay | MD5 등 **ChillPay 전용 기술 자격증명** 설명에만 사용 |
| PG 등록 예시 | `신규 PG코드 (연동용도·MID·Route 지정)` / `내부 등록 PG코드` |
| 대행 수수료 예시 | `각 결제대행사(PG코드), Eximbay 등` |

각 HTML 매뉴얼 목차 직후 **「명명 원칙」** warn-box가 포함됩니다 (KO/EN/JP/CH/TH).

사용자 문서 폴더에 복사:

```bash
node operator-manual-pdf/gen.mjs hq "C:\Users\ziobi\Documents\ICOPAY 메뉴얼"
```

## HTML 미리보기

브라우저에서 `docs/icopay-operator-manual-hq.html` 등을 열고 **인쇄 → PDF 저장**도 가능합니다.
