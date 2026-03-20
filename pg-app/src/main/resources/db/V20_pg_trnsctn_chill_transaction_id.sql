-- 칠페이(ChillPay) 측 TransactionId — 우리 trn_id와 별도 보관
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS chill_transaction_id VARCHAR(64);
