-- 본사 정산관리설정: 서버 정산 자동 배치(스케줄 tick) 허용 여부. app.settlement.autoRunEnabled 와 함께 AND.
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS settlement_auto_batch_enabled_yn CHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_hq_ledger_sys_settings.settlement_auto_batch_enabled_yn IS 'Y: 스케줄에서 AUTO 정산 배치 실행 허용. N: 본사 화면에서 중지(가맹 AUTO 설정과 별개).';
