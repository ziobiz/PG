-- 정산주기 코드 확장 (실시간·분·시간·D+N·W+N·Weekly2 등)
ALTER TABLE tb_settlement_setting ALTER COLUMN calc_cycle TYPE VARCHAR(64);
