-- 무효·수동무효 건당 수수료 (취소·환불 사이 정책 항목)
-- 거래 status: 21 = 무효(승인 후 규정 시간 내 자동 무효), 22 = 수동무효(이메일 등 수동 무효 구간)
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS void_fee_per_tx NUMERIC(12, 0) NOT NULL DEFAULT 0;
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS manual_void_fee_per_tx NUMERIC(12, 0) NOT NULL DEFAULT 0;
