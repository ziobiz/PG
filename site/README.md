# 우리 PG 사이트 (개발 중)

**https://fxhj.soonpay.co.kr** 을 참고해 분석한 메뉴 구성을, **우리가 개발하는 PG 솔루션**에 동일한 형태로 적용한 프로젝트입니다.  
외부 사이트 연동(iframe 등)은 하지 않습니다. 메뉴 클릭 시 우리 사이트 내 해당 페이지가 로드됩니다.

## 실행 방법

- `site` 폴더를 웹 서버로 띄워서 접속합니다.
  ```bash
  cd site && npx serve -p 3000
  ```
- 또는 배포 서버의 문서 루트에 `site` 를 두고 접속합니다.

## 구조

- `index.html`: 레이아웃 + 좌측 메뉴(전체) + 상단 탭 + 컨텐츠 영역
- `js/app.js`: `fnTopMenuMove(url)` 구현, 탭 추가/전환, iframe 로드, `SITE_CONFIG` 처리
- `css/site.css`: 좌측 메뉴·탭·컨텐츠 영역 스타일

## 참고

- 메뉴 데이터: `../docs/menu-structure.json`
- 메뉴-내용 연결 분석: `../docs/메뉴_분석_및_연결구조.md`
