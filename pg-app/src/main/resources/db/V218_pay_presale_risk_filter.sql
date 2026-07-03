-- JPAY·ChillPay 송부 전 사전 리스크 필터 + 결제창 연락처 자동기억 정책

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS presale_filter_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS filter_buyer_contact_mismatch_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS filter_holder_name_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS filter_velocity_card_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS filter_velocity_email_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS filter_velocity_ip_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS velocity_window_minutes INT NOT NULL DEFAULT 10;

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS velocity_max_attempts INT NOT NULL DEFAULT 3;

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS checkout_contact_remember_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS checkout_contact_remember_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.checkout_contact_remember_mode IS 'FOLLOW_HQ|Y|N — 결제창 이메일·전화·성명 localStorage 자동기억';

CREATE TABLE IF NOT EXISTS tb_pay_risk_filter_event (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NULL,
    merchant_id     VARCHAR(20) NULL,
    order_no        VARCHAR(64) NULL,
    trn_id          VARCHAR(20) NULL,
    pg_vendor       VARCHAR(16) NULL,
    filter_code     VARCHAR(64) NOT NULL,
    filter_desc     VARCHAR(500) NULL,
    detail_json     TEXT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_pay_risk_filter_event_created ON tb_pay_risk_filter_event (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_pay_risk_filter_event_merchant ON tb_pay_risk_filter_event (merchant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_pay_risk_filter_event_code ON tb_pay_risk_filter_event (filter_code, created_at DESC);
