-- V260: 가맹 ICOPAY 샌드박스 (키 env · 활성 · 보관일 · 거래/통보)
SET client_min_messages TO WARNING;

ALTER TABLE tb_merchant_icopay_broker_credential
    ADD COLUMN IF NOT EXISTS env_mode VARCHAR(10) NOT NULL DEFAULT 'LIVE';

COMMENT ON COLUMN tb_merchant_icopay_broker_credential.env_mode IS 'LIVE | SANDBOX';

DO $$
BEGIN
    ALTER TABLE tb_merchant_icopay_broker_credential
        DROP CONSTRAINT IF EXISTS uq_merchant_icopay_broker_vendor;
EXCEPTION WHEN undefined_object THEN
    NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE tb_merchant_icopay_broker_credential
        DROP CONSTRAINT IF EXISTS uq_merchant_icopay_broker_vendor_env;
EXCEPTION WHEN undefined_object THEN
    NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_merchant_icopay_broker_vendor_env'
    ) THEN
        ALTER TABLE tb_merchant_icopay_broker_credential
            ADD CONSTRAINT uq_merchant_icopay_broker_vendor_env UNIQUE (org_unit_id, vendor_scope, env_mode);
    END IF;
END $$;

ALTER TABLE tb_merchant_notify_url
    ALTER COLUMN url_type TYPE VARCHAR(32);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS sandbox_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_merchant_profile.sandbox_use_yn IS 'Y=샌드박스 활성(관리자). N=비활성';

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS sandbox_retain_days INTEGER NOT NULL DEFAULT 3;

COMMENT ON COLUMN tb_hq_api_config.sandbox_retain_days IS '샌드박스 결제·통보 이력 보관 일수(기본 3)';

CREATE TABLE IF NOT EXISTS tb_merchant_sandbox_txn (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NOT NULL,
    comp_id         VARCHAR(32) NOT NULL,
    order_no        VARCHAR(100) NOT NULL,
    amount          NUMERIC(18, 2) NOT NULL,
    currency        VARCHAR(8) NOT NULL DEFAULT 'KRW',
    product_name    VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    session_token   VARCHAR(128),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP,
    CONSTRAINT uq_merchant_sandbox_txn_order UNIQUE (org_unit_id, order_no)
);

CREATE INDEX IF NOT EXISTS ix_merchant_sandbox_txn_created
    ON tb_merchant_sandbox_txn (created_at);

CREATE TABLE IF NOT EXISTS tb_merchant_sandbox_notify_log (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT,
    comp_id         VARCHAR(64) NOT NULL,
    sandbox_txn_id  BIGINT,
    order_no        VARCHAR(100),
    url_type        VARCHAR(32) NOT NULL,
    target_url      VARCHAR(1000) NOT NULL,
    result_status   VARCHAR(16) NOT NULL,
    http_status     INTEGER,
    retry_cnt       INTEGER NOT NULL DEFAULT 0,
    error_message   TEXT,
    payload_body    TEXT,
    sent_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_merchant_sandbox_notify_sent
    ON tb_merchant_sandbox_notify_log (sent_at);
