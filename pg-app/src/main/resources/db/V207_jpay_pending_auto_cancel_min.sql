-- JPAY 요청(08) 무응답 자동취소 대기(분). 0=미사용
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS jpay_pending_auto_cancel_min INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_hq_ledger_sys_settings.jpay_pending_auto_cancel_min IS 'JPAY 요청 무응답 자동취소 대기(분). 0=미사용, 30·60·120·180·240·300·360·720';
