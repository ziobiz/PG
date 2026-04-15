-- PG↔ICOPAY 통합정산 예정일(T+N 영업일 동일시각 / D+N 달력일+일괄시각). 사용안함=OFF.
ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS ext_settle_mode VARCHAR(8) NOT NULL DEFAULT 'OFF',
    ADD COLUMN IF NOT EXISTS ext_settle_lag INTEGER NULL,
    ADD COLUMN IF NOT EXISTS ext_settle_batch_time TIME WITHOUT TIME ZONE NULL;

COMMENT ON COLUMN tb_pg_agency.ext_settle_mode IS 'OFF=미사용, T=T+N(영업일·결제동일시각), D=D+N(달력+N일·일괄시각)';
COMMENT ON COLUMN tb_pg_agency.ext_settle_lag IS 'N (1~10). OFF면 무시';
COMMENT ON COLUMN tb_pg_agency.ext_settle_batch_time IS 'D 모드: 해당 정산일의 정산 시각(HH:mm). T 모드는 무시';

-- 가맹 MID별 덮어쓰기. NULL=연동(tb_pg_agency) 기본 따름, OFF=이 MID는 예정일 미표시(강제)';
ALTER TABLE tb_merchant_pg_binding
    ADD COLUMN IF NOT EXISTS ext_settle_mode VARCHAR(8) NULL,
    ADD COLUMN IF NOT EXISTS ext_settle_lag INTEGER NULL,
    ADD COLUMN IF NOT EXISTS ext_settle_batch_time TIME WITHOUT TIME ZONE NULL;

COMMENT ON COLUMN tb_merchant_pg_binding.ext_settle_mode IS 'NULL=연동 기본, OFF/T/D=가맹 MID별 덮어쓰기';
