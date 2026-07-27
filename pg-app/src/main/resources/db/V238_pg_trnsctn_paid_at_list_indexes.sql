-- 결제내역 기간 검색: COALESCE(paid_at, created_at) 대신 paid_at/created_at OR 조건이
-- 인덱스를 타도록 paid_at 계열 인덱스 보강(당월 수 건에도 전체 스캔→504 방지)

CREATE INDEX IF NOT EXISTS idx_pg_trnsctn_paid_at
    ON pg_trnsctn (paid_at);

CREATE INDEX IF NOT EXISTS idx_pg_trnsctn_merchant_paid
    ON pg_trnsctn (merchant_id, paid_at);

CREATE INDEX IF NOT EXISTS idx_pg_trnsctn_created_at
    ON pg_trnsctn (created_at);
