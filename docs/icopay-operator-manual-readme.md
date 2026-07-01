# ICOPAY 운영자 메뉴얼 (HTML · PDF)

역할별 **3종 × 5개 언어** HTML 소스와 PDF 내보내기 스크립트입니다.

## 역할 구분

| 역할 | HTML 접두 | PDF 파일명 예시 |
|------|-----------|-----------------|
| **총본사** (Super Admin) | `icopay-operator-manual*.html` | `ICOPAY Super Admin Operation Manual_KR_260625_V3.0.pdf` |
| **본사** (HQ Admin) | `icopay-operator-manual-hq*.html` | `ICOPAY Headquarters Operation Manual_KR_260625_V3.0.pdf` |
| **총판** (Distributor) | `icopay-operator-manual-dist*.html` | `ICOPAY Distributor Operation Manual_KR_260625_V3.0.pdf` |

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

사용자 문서 폴더에 복사:

```bash
node operator-manual-pdf/gen.mjs hq "C:\Users\ziobi\Documents\ICOPAY 메뉴얼"
```

## HTML 미리보기

브라우저에서 `docs/icopay-operator-manual-hq.html` 등을 열고 **인쇄 → PDF 저장**도 가능합니다.
