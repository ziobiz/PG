-- Hibernate validate: String(length=1) → JDBC VARCHAR. PostgreSQL CHAR(1)는 bpchar이라 불일치 발생.
-- V65로 이미 CHAR(1) 생성된 DB용 수정.
ALTER TABLE tb_hq_ledger_sys_settings
    ALTER COLUMN ntp_sync_enabled_yn TYPE VARCHAR(1),
    ALTER COLUMN smtp_tls_yn TYPE VARCHAR(1),
    ALTER COLUMN smtp_auth_yn TYPE VARCHAR(1),
    ALTER COLUMN email_on_sync_failure_yn TYPE VARCHAR(1),
    ALTER COLUMN email_daily_digest_yn TYPE VARCHAR(1),
    ALTER COLUMN email_notify_void_batch_yn TYPE VARCHAR(1),
    ALTER COLUMN email_notify_refund_batch_yn TYPE VARCHAR(1);
