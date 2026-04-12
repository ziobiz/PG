-- 수수료내역 금액 소수 자릿수·반올림 (본사 전산설정관리)
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS fee_list_decimal_places INTEGER NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS fee_list_round_mode VARCHAR(16) NOT NULL DEFAULT 'CEILING';

COMMENT ON COLUMN tb_hq_ledger_sys_settings.fee_list_decimal_places IS '수수료내역 등 금액 소수 자릿수(0~8, 기본 2)';
COMMENT ON COLUMN tb_hq_ledger_sys_settings.fee_list_round_mode IS '소수 처리: CEILING=절상, HALF_UP=반올림, DOWN=그대로(버림)';
