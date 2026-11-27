# PAYSO Void 정책 (ICOPAY 운영)

## 공식 API (Web API Guideline)

- 엔드포인트: `POST /payment/gateway/v2/payment/voidpayment`
- **정산 전(Void)** 만 제공. 공개 가이드에 **정산 후 Refund REST 없음**.
- 실패 코드 예: `4A010` — cannot be voided (정산·창 종료 후).

## 운영 확인 (문서화 · 2026-11-27)

- API 스펙에 고정 HH:mm 마감 문구는 없으나, **실무상 Void는 태국(`Asia/Bangkok`) 달력 당일 승인 건만 성공**하는 것으로 본다.
- 익일·정산 완료 건은 ICOPAY Void 대신 **결제대행사 포털 환불**.

## ICOPAY 반영

| 구분 | 동작 |
|------|------|
| 후속조치 무효 창 | 본사 「시간 선택 국가」Zone의 **당일 시작~마감** (통상 Bangkok) |
| 자동무효 / 자동환불(창 내) | 동일 Void API |
| 강제환불 | ICOPAY **수동 반영**(빨간 확인) — Void API 미호출 |
| 구매자·가맹 API | PG명 미노출(ICOPAY 중립) |

코드 앵커: `PaysoApiClient` · `PaysoPaymentService#requestVoid` · `PayFollowPolicyService#isVoidRefundWindowFollowTransaction` · `PgAgencyPayFollowCapability`.
