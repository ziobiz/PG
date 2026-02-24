# FXHJ 100% 동일 구현 명세 (output/fxhj-main-after-login.html, fxhj-login-ui 기준)

## 1. 로그인 (fxhj-login-ui/index.html)
- 타이틀: ONTHELINE FXHJ
- 안내: "잠시만 기다려주십시오", OTP 키값 전송 안내 3줄
- 폼: 아이디, 비밀번호, 변경 비밀번호, 인증번호, OTP번호(6자리), 로그인 버튼, "초기비밀번호 변경" 버튼
- 사칭 안내: 제목 "※ 사칭 피해 주의 안내 ※", 본문 5줄
- 모달 초기비밀번호: 기존비밀번호, 변경 비밀번호, 유효성 문구(4자미만, 아이디+1!, 초기화 비밀번호 사용불가), 저장/닫기

## 2. 메인 레이아웃 (fxhj)
- body: data-layout-config='{"leftSideBarTheme":"dark"}', data-leftbar-theme="dark", data-leftbar-compact-mode=""
- dimmed: id="dimm", spinner + "잠시만 기다려주십시오"
- wrapper > left-side-menu (z-index:99 !important)
- 로고: href="/main", logo-lg(img+높이70px), logo-sm
- 접기: leftSideFoldBtn, mdi-chevron-double-left, leftSideFoldSpan "접기"
- 좌측 메뉴 아이콘: dripicons-store(업체관리), dripicons-card(결제), uil-dollar-alt(정산), mdi mdi-file-send(통보), dripicons-user-group(사용자)
- 메뉴 순서(업체관리): 공지사항, 업체정보조회, 업체관리, 수수료관리, 업체변경이력 (저장본에 업체등록 없음)
- 상단: list-unstyled topbar-right-menu float-right, 접속 IP/접속시간 (font-weight 600), dropdown (mdi mdi-account-cog), 나의정보(mdi-account-circle), 로그아웃(mdi-logout), button-menu-mobile (mdi-menu), tab 영역, 전체닫기 (mdi-close-box-multiple-outline)
- 탭: copyTopTab style display:flex; border-right:1px solid #ddd; background-color:#eff0f2; tab-close-button "×", tab_id, top_tab_url
- breadcrumb: "대메뉴 > 하위메뉴" (font-weight bold)
- page-title: 아이콘 + 제목 (예: uil-crosshairs 결제내역)
- footer: "Copyright © 2023 ICOPAY Service by Ontheline Co., Ltd."

## 3. 적용 완료 사항 (site/)
- 로그인: 타이틀 ONTHELINE FXHJ, 아이디/비밀번호/변경 비밀번호/인증번호/OTP번호, OTP 안내 3줄, 사칭 안내 5줄, 초기비밀번호 변경 모달(기존비밀번호/변경 비밀번호/유효성 문구/저장·닫기)
- 메인: body data-leftbar-compact-mode, dimmed 레이어(id=dimm), 로고 href=/main, 접기 버튼, 상단 float-right·접속 IP/접속시간 font-weight 600, 탭 스타일 border #ddd·background #eff0f2, 전체닫기, footer "Copyright © 2023 ICOPAY Service by Ontheline Co., Ltd.", breadcrumb font-weight bold
- 좌측 메뉴: fxhj 저장본과 동일 5대 23하위 (업체등록 없음)
- 결제내역: 조회일자 TRAN/APR/CANC/BALC, 결제구분·정산구분(결제취소/정산취소 포함), 요약 건수/승인금액/취소금액/합계금액/정산수수료/정산부가세/지급액, 버튼 검색/정산확정/엑셀다운로드, 그리드 컬럼 가맹점·업체코드·구분·번호·결제구분·정산구분·승인금액·승인일시·터미널ID·지급액
- 서버: /main 라우트 추가 → index.html

## 4. 결제내역(payList) 검색/요약/버튼
- 조회일자: select 거래일자(TRAN)/승인일자(APR)/취소일자(CANC)/지급일자(BALC), searchFromDate~searchToDate, 당일/전일/전월/당월
- 업체: searchCompType 업체코드(ID)/업체명(NAME), searchCompText, 터미널ID searchTmnId
- 결제구분: 전체/결제(10)/취소(20)
- 정산구분: 전체/정산대기(10)/정산완료(20)/결제취소(30)/정산취소(40)
- 검색 버튼
- 요약: 건수, 승인금액, 취소금액, 합계금액, 정산수수료, 정산부가세, 지급액
- 버튼: 정산확정, 엑셀다운로드
- 그리드 컬럼: 가맹점, 업체코드, 사업자구분/번호, 결제구분, 정산구분, 결제카드, 카드승인번호, ... (fxhj는 TUI Grid 38컬럼)
- 페이지네이션: recordsPerPage 100/500/1000, 개씩 보기, paging, pageCnt, totalPageCount, 이동
