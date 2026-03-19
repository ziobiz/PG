-- 주소 기타 (상세주소 아래)
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS addr_etc VARCHAR(255);
