-- 결제내역 처리사유(상태 변경 사유): 실패·취소·무효·환불 등
ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS outcome_reason TEXT;

ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS outcome_reason_code VARCHAR(64);

ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS outcome_reason_source VARCHAR(32);

ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS outcome_reason_at TIMESTAMP;

COMMENT ON COLUMN pg_trnsctn.outcome_reason IS 'PG/ICOPAY 처리사유(실패·취소·무효·환불 등 상태 변경 시)';
COMMENT ON COLUMN pg_trnsctn.outcome_reason_code IS 'returncode·PaymentStatus 등 원문 코드';
COMMENT ON COLUMN pg_trnsctn.outcome_reason_source IS 'JPAY, CHILLPAY, ICOPAY';
