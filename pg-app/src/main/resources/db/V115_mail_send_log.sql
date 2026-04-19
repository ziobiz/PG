-- 이메일무효(VOID) 요청 메일 등 발송 이력 — 전산설정 테스트·결제내역 이메일무효
CREATE TABLE IF NOT EXISTS tb_mail_send_log (
    id              BIGSERIAL PRIMARY KEY,
    mail_kind       VARCHAR(32)  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    to_address      VARCHAR(500) NOT NULL,
    subject         VARCHAR(500),
    body_preview    TEXT,
    error_message   TEXT,
    pg_trn_id       VARCHAR(32),
    actor_username  VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mail_send_log_created ON tb_mail_send_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mail_send_log_kind ON tb_mail_send_log (mail_kind);

COMMENT ON TABLE tb_mail_send_log IS 'SMTP 발송 이력(이메일무효·테스트 등)';
COMMENT ON COLUMN tb_mail_send_log.mail_kind IS 'VOID_TEST=전산설정 테스트, VOID_TXN=거래 이메일무효';
COMMENT ON COLUMN tb_mail_send_log.status IS 'SUCCESS 또는 FAIL';
