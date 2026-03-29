-- 월간 이용 수수료: 매출 비율(%)이 아니라 정책 통화 단위 고정 금액(월 1회 정산 반영).
-- 기존 NUMERIC(5,2)는 범위가 좁아 큰 월 정액을 담기 어려움.
-- 과거에 매출 %로 저장한 값이 있다면 본사 기본정책·가맹 정책에서 월 정액(원 등)으로 다시 입력하세요.
ALTER TABLE tb_commission_policy
  ALTER COLUMN usage_rate TYPE NUMERIC(12, 0)
  USING round(COALESCE(usage_rate, 0))::NUMERIC(12, 0);
