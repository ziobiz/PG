-- 취소·환불 수수료: 승인금액 대비 %(cancel_rate/refund_rate) → 건당 고정액(통화 단위).
-- 기존에 %로 저장된 값은 그 숫자가 그대로 건당 금액으로 해석되지 않을 수 있으니, 배포 후 기본정책·가맹 정책을 검토하세요.
ALTER TABLE tb_commission_policy
  ALTER COLUMN cancel_rate TYPE NUMERIC(12, 0)
  USING ROUND(COALESCE(cancel_rate, 0))::NUMERIC(12, 0);
ALTER TABLE tb_commission_policy
  ALTER COLUMN refund_rate TYPE NUMERIC(12, 0)
  USING ROUND(COALESCE(refund_rate, 0))::NUMERIC(12, 0);
