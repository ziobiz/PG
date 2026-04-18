-- 정산 실행당 1회 부과: 정산수수료·송금수수료(정책 통화 기준, tb_settlement_run 저장).
-- total_fee 는 거래 집계 기반 수수료만 유지하고, 본 컬럼은 별도 공제액으로 저장합니다.
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS settlement_batch_fee_amt NUMERIC(21, 8);
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS remittance_fee_amt NUMERIC(21, 8);

COMMENT ON COLUMN tb_settlement_run.settlement_batch_fee_amt IS '정산 실행당 1회 정산수수료(정책 fee_settlement_per_tx 금액을 실행당 1회 적용)';
COMMENT ON COLUMN tb_settlement_run.remittance_fee_amt IS '정산 실행당 1회 송금 이체 수수료(remittance_transfer_fee)';
