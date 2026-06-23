-- 가맹점 결제통보(URL Background/Result/MIDDLEWARE) 아웃바운드 전송 이력
CREATE TABLE IF NOT EXISTS tb_merchant_notify_outbound_log (
    id              BIGSERIAL PRIMARY KEY,
    comp_id         VARCHAR(64)  NOT NULL,
    org_unit_id     BIGINT,
    trn_id          VARCHAR(32),
    order_no        VARCHAR(64),
    url_type        VARCHAR(32)  NOT NULL,
    target_url      VARCHAR(1000) NOT NULL,
    notify_channel  VARCHAR(32),
    result_status   VARCHAR(16)  NOT NULL,
    http_status     INTEGER,
    retry_cnt       INTEGER      NOT NULL DEFAULT 0,
    error_message   TEXT,
    sent_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_merchant_notify_outbound_sent ON tb_merchant_notify_outbound_log (sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_merchant_notify_outbound_comp ON tb_merchant_notify_outbound_log (comp_id, sent_at DESC);

COMMENT ON TABLE tb_merchant_notify_outbound_log IS 'ICOPAY→가맹점 결제통보 POST 전송 이력(결제통보 전송관리)';
COMMENT ON COLUMN tb_merchant_notify_outbound_log.url_type IS 'BACKGROUND, RESULT, MIDDLEWARE';
COMMENT ON COLUMN tb_merchant_notify_outbound_log.result_status IS 'SUCCESS 또는 FAIL';
COMMENT ON COLUMN tb_merchant_notify_outbound_log.retry_cnt IS '최초 시도 제외 재시도 횟수(0=1회 성공)';
