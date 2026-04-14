-- 총판별 가맹 정산주기 슬롯 5 → 10 (대표 슬롯 0~9)
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_6 VARCHAR(64);
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_7 VARCHAR(64);
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_8 VARCHAR(64);
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_9 VARCHAR(64);
ALTER TABLE tb_master_dist_settlement_cycle_config ADD COLUMN IF NOT EXISTS cycle_code_10 VARCHAR(64);
