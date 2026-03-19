-- 주소 국가 (기본정보)
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS addr_country_cd VARCHAR(20);
