-- 기타(비고) 수수료 슬롯 4건: 이름·유형(PCT|FIX)·값
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_1_name VARCHAR(64);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_1_mode VARCHAR(8);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_1_value NUMERIC(15, 4);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_2_name VARCHAR(64);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_2_mode VARCHAR(8);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_2_value NUMERIC(15, 4);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_3_name VARCHAR(64);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_3_mode VARCHAR(8);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_3_value NUMERIC(15, 4);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_4_name VARCHAR(64);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_4_mode VARCHAR(8);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS extra_fee_4_value NUMERIC(15, 4);
