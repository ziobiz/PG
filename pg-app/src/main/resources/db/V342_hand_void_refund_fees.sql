-- 수동무효(수무효)·수동환불(수환불) 건당 수수료 분리.
-- 기존 manual_void_fee_per_tx = 이메일무효(이무효) 전용으로 유지(라벨만 변경).

ALTER TABLE tb_commission_policy
  ADD COLUMN IF NOT EXISTS hand_void_fee_per_tx NUMERIC(12, 1) NOT NULL DEFAULT 0;
ALTER TABLE tb_commission_policy
  ADD COLUMN IF NOT EXISTS hand_refund_fee_per_tx NUMERIC(12, 1) NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_commission_policy.void_fee_per_tx IS '무효처리(40) 건당 무효수수료';
COMMENT ON COLUMN tb_commission_policy.hand_void_fee_per_tx IS '수동무효(21) 건당 수무효수수료';
COMMENT ON COLUMN tb_commission_policy.manual_void_fee_per_tx IS '이메일무효(22·41) 건당 이무효수수료';
COMMENT ON COLUMN tb_commission_policy.refund_rate IS '환불처리(42) 건당 환불수수료';
COMMENT ON COLUMN tb_commission_policy.hand_refund_fee_per_tx IS '수동환불(30) 건당 수환불수수료';

ALTER TABLE tb_pg_agency_cost_policy
  ADD COLUMN IF NOT EXISTS hand_void_fee_per_tx NUMERIC(12, 1) NOT NULL DEFAULT 0;
ALTER TABLE tb_pg_agency_cost_policy
  ADD COLUMN IF NOT EXISTS hand_refund_fee_per_tx NUMERIC(12, 1) NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_pg_agency_cost_policy.void_fee_per_tx IS '무효처리(40) 건당 무효수수료';
COMMENT ON COLUMN tb_pg_agency_cost_policy.hand_void_fee_per_tx IS '수동무효(21) 건당 수무효수수료';
COMMENT ON COLUMN tb_pg_agency_cost_policy.manual_void_fee_per_tx IS '이메일무효(22·41) 건당 이무효수수료';
COMMENT ON COLUMN tb_pg_agency_cost_policy.refund_rate IS '환불처리(42) 건당 환불수수료';
COMMENT ON COLUMN tb_pg_agency_cost_policy.hand_refund_fee_per_tx IS '수동환불(30) 건당 수환불수수료';
