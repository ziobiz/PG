-- URL 재결제(저장 카드·CreditToken) 연동 및 토큰 저장 (PostgreSQL)

ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS integ_url_pay_repay_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS endpoint_url_pay_repay VARCHAR(512);

ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_repay_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_repay_path_template VARCHAR(255) DEFAULT '/pay-repay/{compCode}';

CREATE TABLE IF NOT EXISTS tb_merchant_credit_token (
    id BIGSERIAL PRIMARY KEY,
    org_unit_id BIGINT NOT NULL,
    pg_cd VARCHAR(50) NOT NULL,
    customer_id VARCHAR(200) NOT NULL,
    credit_token VARCHAR(500) NOT NULL,
    card_mask VARCHAR(30),
    card_brand VARCHAR(30),
    active_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_merchant_credit_token
    ON tb_merchant_credit_token (org_unit_id, pg_cd, customer_id, credit_token);

CREATE INDEX IF NOT EXISTS ix_merchant_credit_token_lookup
    ON tb_merchant_credit_token (org_unit_id, pg_cd, customer_id, active_yn);
