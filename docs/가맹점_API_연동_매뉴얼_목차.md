# ICOPAY 가맹점 API 연동 매뉴얼 — 목차·배포 안내

가맹점에게는 **ICOPAY 통합 문서만** 노출합니다.  
결제대행사(운영 PG) 이름·PG별 URL은 가맹 화면에 **넣지 않습니다**.

---

## 공식 전달 경로 (필수)

가맹점은 **별도 메일·파일 배포 없이**, 관리자 로그인 후 아래 메뉴에서 연동합니다.

1. 본사: 배포설정 → **가맹점 API 생성**으로 브로커 시크릿·API 배포 완료  
2. 가맹점: **로그인 → 업체관리 → 가맹점API** (`/comp/merchantApiPortal`)  
3. 화면에 표시되는 내용이 곧 연동 스펙입니다.  
   - 연동 키(compId·브로커 시크릿·API 베이스 URL)  
   - 연동 빠른 시작(3단계)  
   - Checkout 엔드포인트·**파라미터 규격 표**  
   - 샘플·체크리스트·WordPress(해당 시)

본사 **API배포문서**(`/hq/merchantApiDeployDocs`)는 위와 **동일 키트**의 미리보기입니다.  
가맹점에 ZIP/MD를 따로 보낼 필요가 없습니다.

---

## 참고 문서 (저장소)

| 용도 | 문서 |
|------|------|
| 빠른 시작(영문 정적) | `site/merchant-api-samples/docs/icopay-merchant-quickstart.md` |
| 빠른 시작(국문 정적) | `site/merchant-api-samples/docs/icopay-merchant-quickstart.ko.md` |
| 저장소 원본 | `docs/가맹점_ICOPAY_간단연동_빠른시작.md` |
| 파라미터 규격 | `docs/가맹점_통합Checkout_API_연동파라미터_규격.md` |
| 연동 흐름 | `docs/가맹점_통합Checkout_API_연동흐름.md` |
| HTML 샘플·표 (서버) | `{publicApiBaseUrl}/merchant-api-samples/` |

화면·체크리스트 언어: **KO / EN / JP / CH / TH**.

**문의:** 본사(ICOPAY) 관리자 채널.

---

## 본사·내부 전용 (가맹 화면·키트 금지)

아래 파일은 **운영·본사 내부** 참고용입니다. 가맹점API·키트·메일 첨부·FTP에 넣지 마세요.

- `가맹점_ChillPay_API_연동가이드.md`, `ChillPay_URL결제_인라인API_배포가이드.md`
- `가맹점_JPAY_API_연동가이드.md`, `JPAY_URL결제_인라인API_배포가이드.md`, `JPAY_샌드박스_검수_절차.md`
- `가맹점_PG_API_연동가이드.md`, `WordPress_JPAY_플러그인_배포가이드.md`

---

## 개정 이력 (목차 문서)

| 버전 | 일자 | 요약 |
|------|------|------|
| 2.1 | 2026-07-15 | 공식 전달 = 가맹 로그인 후 가맹점API. 본사 API배포문서는 미리보기 |
| 2.0 | 2026-07-15 | 가맹 배포 = ICOPAY 통합만. PG별 매뉴얼은 내부 전용으로 격하 |
| 1.5 | 2026-06-01 | 배포설정 「API배포문서」 메뉴 |
| 1.4 | 2026-06-01 | 통합 Checkout prepare 파라미터 규격 |
