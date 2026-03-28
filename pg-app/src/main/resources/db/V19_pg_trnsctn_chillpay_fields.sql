-- ChillPay(칠페이) 거래내역·엑셀 필드 (PostgreSQL). H2 dev는 JPA ddl-auto:create-drop 로 동기화.
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS order_no VARCHAR(64);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS customer_id VARCHAR(100);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS customer_nm VARCHAR(200);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS payment_channel VARCHAR(80);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS icopay_amt NUMERIC(15, 0);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS chill_fee_amt NUMERIC(15, 0);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS total_amt NUMERIC(15, 0);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS route_no VARCHAR(32);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS chill_payment_status VARCHAR(50);
-- Hibernate validate: String + length 1 → VARCHAR(1). CHAR(1)/bpchar 는 타입 불일치 오류 발생.
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS settled_yn VARCHAR(1) DEFAULT 'N';
