-- 가맹점 출금제한 유형(tb_settlement_setting.withdraw_restrict_type)
-- 이체및송금구분 코드 확장(AUTO_NO_MANUAL, ARBITRARY 등)

ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS withdraw_restrict_type VARCHAR(32);

ALTER TABLE tb_settlement_setting ALTER COLUMN transfer_type TYPE VARCHAR(32);
