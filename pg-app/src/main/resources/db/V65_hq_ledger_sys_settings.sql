-- 전산설정관리 (ziobiz/NOTI 시스템·환경설정 대응): 시간·동기화, 자동화 메일 등
CREATE TABLE IF NOT EXISTS tb_hq_ledger_sys_settings (
    id                      BIGINT PRIMARY KEY,
    display_timezone        VARCHAR(64),
    ntp_sync_enabled_yn     CHAR(1) NOT NULL DEFAULT 'N',
    ntp_server_list         VARCHAR(500),
    time_sync_interval_min  INTEGER,
    smtp_host               VARCHAR(255),
    smtp_port               INTEGER,
    smtp_tls_yn             CHAR(1) NOT NULL DEFAULT 'Y',
    smtp_auth_yn            CHAR(1) NOT NULL DEFAULT 'Y',
    smtp_username           VARCHAR(255),
    smtp_password           VARCHAR(512),
    mail_from_address       VARCHAR(255),
    mail_from_name          VARCHAR(200),
    alert_recipient_emails  TEXT,
    email_on_sync_failure_yn       CHAR(1) NOT NULL DEFAULT 'N',
    email_daily_digest_yn          CHAR(1) NOT NULL DEFAULT 'N',
    email_notify_void_batch_yn     CHAR(1) NOT NULL DEFAULT 'N',
    email_notify_refund_batch_yn   CHAR(1) NOT NULL DEFAULT 'N',
    memo                    TEXT,
    created_at              TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO tb_hq_ledger_sys_settings (
    id, ntp_sync_enabled_yn, smtp_tls_yn, smtp_auth_yn,
    email_on_sync_failure_yn, email_daily_digest_yn, email_notify_void_batch_yn, email_notify_refund_batch_yn,
    created_at, updated_at
)
SELECT 1, 'N', 'Y', 'Y', 'N', 'N', 'N', 'N', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_hq_ledger_sys_settings WHERE id = 1);
