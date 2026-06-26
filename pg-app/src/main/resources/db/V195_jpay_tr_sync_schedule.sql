-- JPAY 통합조회 자동 동기화 주기(분). 0=사용 안 함
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS jpay_tr_sync_schedule_min INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_hq_ledger_sys_settings.jpay_tr_sync_schedule_min IS 'JPAY 통합조회 포털 Export 자동 동기화 주기(분). 0=미사용, 10~720';
