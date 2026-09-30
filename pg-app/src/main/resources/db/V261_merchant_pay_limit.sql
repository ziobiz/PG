-- 총판·가맹 결제 한도 (기준 통화 = 총판 기준 통화). 가맹 mode FOLLOW=총판설정따름, DIRECT=직접설정.
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_tx_max NUMERIC(18,2);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_tx_min NUMERIC(18,2);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_day NUMERIC(18,2);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_month NUMERIC(18,2);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_year_corp NUMERIC(18,2);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_year_ind NUMERIC(18,2);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_tx_max_mode VARCHAR(8);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_tx_min_mode VARCHAR(8);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_day_mode VARCHAR(8);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_month_mode VARCHAR(8);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_year_corp_mode VARCHAR(8);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pay_lmt_year_ind_mode VARCHAR(8);
