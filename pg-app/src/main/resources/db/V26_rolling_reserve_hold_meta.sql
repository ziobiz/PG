-- 담보금(롤링) 내역: 적용일·보류 영업일 수·해지 시각
ALTER TABLE tb_rolling_reserve ADD COLUMN IF NOT EXISTS hold_start_date DATE;
ALTER TABLE tb_rolling_reserve ADD COLUMN IF NOT EXISTS hold_business_days INT;
ALTER TABLE tb_rolling_reserve ADD COLUMN IF NOT EXISTS released_at TIMESTAMP;
