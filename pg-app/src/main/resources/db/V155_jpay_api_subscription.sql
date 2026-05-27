-- JPAY 가맹 API 구독(정기) — ③ 전용. URL/챗봇과 분리.

ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS jpay_subscription_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS jpay_subscription_inline_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS jpay_subscription_path_template VARCHAR(128) NOT NULL DEFAULT '/jpay-subscribe/{compCode}';
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS jpay_subscription_config_json TEXT;

ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS integ_api_subscription_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS endpoint_api_subscription VARCHAR(512);

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS api_jpay_subscription_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';

CREATE TABLE IF NOT EXISTS tb_merchant_jpay_subscription (
    id BIGSERIAL PRIMARY KEY,
    org_unit_id BIGINT NOT NULL,
    comp_code VARCHAR(32) NOT NULL,
    checkout_order_no VARCHAR(64) NOT NULL,
    pg_cd VARCHAR(20) NOT NULL,
    subscription_plan_json TEXT NOT NULL,
    payment_transaction_id VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    period_count INTEGER NOT NULL DEFAULT 0,
    last_notify_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_merchant_jpay_sub_order UNIQUE (comp_code, checkout_order_no)
);

CREATE INDEX IF NOT EXISTS idx_merchant_jpay_sub_org ON tb_merchant_jpay_subscription (org_unit_id);
CREATE INDEX IF NOT EXISTS idx_merchant_jpay_sub_ptx ON tb_merchant_jpay_subscription (payment_transaction_id);

COMMENT ON TABLE tb_merchant_jpay_subscription IS 'JPAY API 구독 마스터(가맹 API 인라인 전용)';
COMMENT ON COLUMN tb_merchant_jpay_subscription.checkout_order_no IS '최초 구독 pay_orderid(J-Pay Cancel 키)';
