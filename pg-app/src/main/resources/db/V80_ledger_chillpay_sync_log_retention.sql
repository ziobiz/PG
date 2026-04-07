-- 통합내역(칠페이) 동기화·로그 보관 일수 + 후속조치 환불 기본값 보정
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS chillpay_tr_init_sync_months INTEGER NOT NULL DEFAULT 3;
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS chillpay_tr_recent_sync_days INTEGER NOT NULL DEFAULT 2;
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS app_log_memory_retention_days INTEGER NOT NULL DEFAULT 30;
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS app_log_file_retention_days INTEGER NOT NULL DEFAULT 90;

COMMENT ON COLUMN tb_hq_ledger_sys_settings.chillpay_tr_init_sync_months IS '통합내역: 검색 초기화(또는 넓은 구간) 시 과거 몇 개월';
COMMENT ON COLUMN tb_hq_ledger_sys_settings.chillpay_tr_recent_sync_days IS '통합내역: 날짜 미지정 조회 시 최근 며칠(포함)';
COMMENT ON COLUMN tb_hq_ledger_sys_settings.app_log_memory_retention_days IS '애플리케이션 로그(메모리/버퍼) 보관 목표 일수';
COMMENT ON COLUMN tb_hq_ledger_sys_settings.app_log_file_retention_days IS '애플리케이션 로그 파일 보관 목표 일수';

UPDATE tb_hq_notify_env_config SET auto_refund_after_days = 7 WHERE auto_refund_after_days IS NULL;
UPDATE tb_hq_notify_env_config SET force_refund_after_days = 0 WHERE force_refund_after_days IS NULL;
UPDATE tb_hq_notify_env_config SET email_void_end_min = 1439 WHERE email_void_end_min IS NULL OR email_void_end_min <> 1439;
