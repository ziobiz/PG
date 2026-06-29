-- 결제개요(검수): 결제 시 도시 (JPAY payCity / CF-IPCity 등)
ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS payer_city VARCHAR(128) NULL;

COMMENT ON COLUMN pg_trnsctn.payer_city IS '결제 고객 도시 (예: SEOUL)';
