# PG 중계 솔루션 분석 보고서

## 1. 개요

- **목적**: 국내/해외 PG사의 가맹점(MID/KEY)으로 계약 후, 타 업체(실 가맹점·SUB MERCHANT)에게 결제 중계 서비스를 제공하는 **준 PG(Sub-PG)** 시스템
- **기반 소스**: 2011년경 개발 (SJMIR, mir-mall.co.kr 기반)
- **분석 기준 참고 사이트** (뷰어 전용, 참고만 가능, 수정 불가):
  - **https://fxhj.soonpay.co.kr/login** ← **분석은 이 사이트 기준**. Google OTP(2단계 인증) 지원.
  - 기타 참고: https://otl.soonpay.co.kr, https://otl3.soonpay.co.kr/main (온더라인 통합관리자 JPY)

---

## 2. 제공 소스 구조

### 2.1 디렉터리 구성

| 경로 | 설명 |
|------|------|
| `PG소스_CD/pg.mir-mall.co.kr/` | **PG 서버·웹앱** (Resin, Spring MVC, JSP) |
| `PG소스_CD/www.mir-mall.co.kr/` | 결제 연동·데모 (SJMIR_PAYMENT 등) |
| `PG소스_CD/DB_Oracle10g/` | Oracle 10g 스키마 문서 (테이블 HTML) |
| `PG소스_CD/db.mir-mall.co.kr/` | DB 관련 툴/문서 |

### 2.2 핵심 애플리케이션 (pg.mir-mall.co.kr)

- **SJMIR_APP** (WAR)
  - Spring MVC (`*.do` → DispatcherServlet)
  - JSP 뷰, UTF-8 필터, JSTL
  - **컨트롤러**: SSO, WebPay, Trnsctn, Merchant, Group, Settle, Van, Member, Terminal, Code 등
- **SJMIR_PAYMENT** (결제 서버)
  - TCP 소켓 서버로 결제 요청 수신
  - **실 결제 처리**: `Credit`, `WebCredit`, `Refund` 등 → **이니시스 INIpay41** 단일 연동
- **PGMATE**
  - 공통 비즈니스 레이어: `com.pgmate.dao.*`, `com.pgmate.bean.*`
  - 가맹점/총판/거래/정산/코드 등 DAO·Bean 제공

### 2.3 웹 플로우 (가맹점 로그인 ~ 결제)

1. **로그인**: `/webpay.do?request=login` → `WebPayController.login()`  
   - PG_MERCHANT 기준 가맹점 ID/비밀번호(또는 서브계정 비밀번호) 검증  
   - 세션 `WEBSSO` 저장, 120분 타임아웃
2. **결제 준비**: `/webpay.do?request=payReady` → `payReady.jsp`  
   - 가맹점 서비스 관리(PG_MERCHANT_MNG)에서 **웹결제(BIZMEKA)** 사용 여부 확인
3. **결제 실행**: `/webpay.do?request=payExecute`  
   - WebCreditReqBean 생성 → **PAYMENT TCP 서버**로 전송  
   - SJMIR_PAYMENT의 `WebCredit`에서 **INIpay41** 호출 (TB_VAN의 MID/키 사용)
4. **결과**: `payComplete.jsp` / `payFail.jsp`
5. **거래 조회/취소**: `paySearch.jsp`, `payList.jsp` 등 → `trnsctn.do` (TrnsctnController)

### 2.4 PG(VAN) 연동 현황

- **현재 구현**: **이니시스(INIpay)** 단일 연동
  - `com.inicis.inipay.INIpay41`
  - `Credit.java`, `WebCredit.java`에서 `vanBean.getMerchantId()`, `vanBean.getEtc()`(키) 사용
  - TB_VAN: VAN명(예: INICIS1), MERCHANT_ID, ETC(키) 등
- **통화**: 소스 상 WON/KRW, DB에는 PG_TRNSCTN에 AMT_JPY, AMT_USD 등 다통화 컬럼 존재
- **다중 PG/VAN**: 구조상 TB_VAN 다건은 가능하나, **로직은 하나의 INIpay만 사용** → 해외 PG·다중 PG 연동을 위해서는 **PG 어댑터 레이어 신설 필요**

---

## 3. DB 스키마 요약 (Oracle 10g)

### 3.1 권한·멤버

| 테이블 | 역할 |
|--------|------|
| PG_MERCHANT | 가맹점 마스터 (MERCHANT_ID, 그룹, 사업자, 비밀번호, 서브계정 비밀번호 등) |
| PG_MERCHANT_MNG | 가맹점 서비스·한도·통화·VAN 매핑 (LANGUAGE, CUR_TYPE, LIMIT_*, SERVICE_BIZMEKA, SERVICE_ONLINE 등) |
| TB_GROUP_MNG | 총판 (GROUP_ID, SETTLE_TYPE M/G, RATE, 은행/계좌) |
| TB_USER, TB_WEBPAY_USER | 관리자/웹결제 사용자 |
| TB_TERMINAL | 단말(터미널) ↔ 가맹점 매핑 |
| TB_LIMIT | 총판/가맹점별 한도 (구분 G/M) |

### 3.2 거래·취소

| 테이블 | 역할 |
|--------|------|
| PG_TRNSCTN | 거래 마스터 (TRN_ID, MERCHANT_ID, SERVICE_TYPE, STATUS, 통화, 금액, 가맹점 주문번호 등) |
| PG_TRNSCTN_REFUND | 취소 (승인/매입/정산 취소 타입) |
| PG_TRNSCTN_ACQUIRE, PG_TRNSCTN_SCR, PG_TRNSCTN_CB 등 | 매입/스크래핑/차백 등 |

### 3.3 과금·정산

| 테이블 | 역할 |
|--------|------|
| PG_MERCHANT_BILL | 가맹점별 과금 정책 (정산주기, 비자/마스터/JCB/DINERS %, 건당 고정금액, 선정산 수수료 등) |
| TB_SETTLE | 정산 (과금 ID, 정산형태, 요율, 정산금액, 정산여부, 회차, 대상일자 등) |

### 3.4 기타

| 테이블 | 역할 |
|--------|------|
| TB_VAN | VAN(PG)별 MID/키 (현재 이니시스용) |
| PG_*_NOTICE_*, PG_CENTER_* | 공지, FAQ, 요청 등 |

---

## 4. 메뉴얼 관련

- **가맹점 메뉴얼 / 운영 메뉴얼 / 총판 운영 메뉴얼**  
  프로젝트 내에서 **파일명으로 검색한 결과, 해당 제목의 문서는 없었습니다.**  
  (다른 이름·경로 또는 별도 폴더에 있을 수 있음)
- **운영 관점**은 위 DB 주석, JSP 타이틀, 컨트롤러·URL 구조로 추정 가능:
  - **가맹점**: 로그인 → 웹결제/거래조회·취소 (webpay, trnsctn)
  - **운영**: 가맹점/총판/멤버/터미널/VAN/코드/정산/거래리스크/센터 요청 등 (member, group, van, settle, trnsctn 등)
  - **총판**: TB_GROUP_MNG, TB_LIMIT 구분 G, 정산형태(가맹점별 M / 총판정산 G)

메뉴얼 파일을 별도로 보유하고 계시다면 경로를 알려주시면 해당 내용을 반영해 분석을 보완하겠습니다.

---

## 5. 분석 기준 참고 사이트 (fxhj.soonpay.co.kr) 요약

- **URL**: https://fxhj.soonpay.co.kr/login  
- **분석은 이 사이트 기준**으로 진행. 뷰어 전용(참고만 가능, 수정 불가).
- **확인된 로그인 화면**:  
  - 아이디, 비밀번호, 변경 비밀번호, 인증번호, **OTP 번호**  
  - **Google OTP(2단계 인증)** 지원  
  - Google OTP 안내 문구, 사칭 피해 주의 안내
- **제공 소스와의 차이**:  
  - 현재 제공 소스: 단순 ID/PW + 세션  
  - 참고 사이트(fxhj): **OTP(2단계 인증)**, 비밀번호 변경, 인증번호 등 **보안·운영 기능 강화**
- **추가 분석**: 필요 시 **페이지별 캡처**를 주시면 메뉴 구조·정산/과금 화면·다국어 여부 등을 더 정리할 수 있습니다.

---

## 6. 목표 요구사항과의 매핑

### 6.1 다양한 해외 Main PG 연동

- **현재**: 이니시스(INIpay) 1곳, 국내 위주.
- **필요 작업**:
  - **PG 어댑터 레이어** 설계: 결제 요청/응답을 공통 모델로 표준화.
  - **Main PG별 구현체** 추가: 해외 PG API (Stripe, Braintree, Adyen, 현지 PG 등)별 연동 모듈.
  - TB_VAN 확장 또는 **PG 채널 테이블** 추가: PG사별 MID/KEY/API URL 등.
  - **라우팅 정책**: 가맹점/상품/통화/국가 등에 따라 사용할 Main PG 선택 (정책 테이블 + 서비스 로직).

### 6.2 다국어 지원

- **대상 언어**: 한국어, 영어, 일본어, 태국어, 중국어, 베트남어, 인도네시아어, 인도어, 말레이시아어, 대만, 홍콩 등.
- **현재**:  
  - PG_MERCHANT_MNG에 `LANGUAGE` 컬럼 존재.  
  - JSP/메시지는 하드코딩·한국어 위주.
- **필요 작업**:
  - **리소스 번들** (예: message_ko.properties, message_en.properties 등) 또는 DB/API 기반 다국어 메시지.
  - 언어 선택(세션/도메인/URL 파라미터) 및 리소스 로딩 적용.
  - 프론트/이메일/알림 문구까지 다국어 키 관리.

### 6.3 유연한 과금·정산 설정

- **현재**:  
  - **PG_MERCHANT_BILL**: 정산주기(PERIOD), 카드사별 %, 건당 고정금액, 선정산 수수료 등.  
  - **TB_GROUP_MNG**: 총판 요율, 정산형태(M/G).  
  - **TB_SETTLE**: 정산 회차, 대상일, 지급 여부 등.
- **필요 작업**:
  - **과금/정산 정책을 화면·설정으로 관리**: 신규 정책 추가, 기존 정책 수정, 적용 시점(즉시/예약).
  - **이력 관리**: 정책 변경 시 이전 버전 보관, 정산 회차별 적용 정책 추적.
  - **환경/파라미터** 외부 설정(DB 또는 설정 서버)으로 분리해 코드 수정 없이 변경 가능하도록 구성.

---

## 7. 기술 스택 정리

| 구분 | 현재 |
|------|------|
| 서버 | Resin |
| 웹 | Spring MVC 2.x, JSP, JSTL |
| DB | Oracle 10g |
| 결제 SDK | 이니시스 INIpay41 |
| 인코딩 | UTF-8 (필터), web.xml euc-kr 선언 일부 존재 |
| 세션 | 30분(web.xml), 웹결제 120분(코드) |

---

## 8. 다음 단계 제안

1. **메뉴얼**  
   - 가맹점/운영/총판 메뉴얼 파일이 있다면 경로 공유 → 반영 분석 및 요구사항 매핑 보완.

2. **참고 사이트**  
   - 로그인 이후 메뉴 구조, 정산/과금 화면, 다국어 전환 등 **캡처** 제공 시, 기능 목록 및 UI 구조 정리.

3. **아키텍처**  
   - “다중 해외 PG + 다국어 + 유연한 과금/정산”을 위한 **신규 아키텍처 초안** (PG 어댑터, 정책 엔진, 다국어 레이어) 작성.

4. **마이그레이션**  
   - 기존 Oracle/Resin/Spring 2.x 유지 vs. 재구현(Spring Boot, PostgreSQL 등) 결정 후, 단계별 이전 계획 수립.

이 보고서를 기준으로, 메뉴얼·캡처·우선 적용할 PG 목록 등을 알려주시면 그에 맞춰 상세 설계나 작업 순서를 정리해 드리겠습니다.
