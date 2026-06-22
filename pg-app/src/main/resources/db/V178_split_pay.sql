-- URL 분할구매(분할결제) — 계약·회차·수수료 스냅샷

ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS split_pay_fee_pct NUMERIC(5, 2) NOT NULL DEFAULT 0;
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS split_pay_fixed_fee_per_inst NUMERIC(12, 1) NOT NULL DEFAULT 0;

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_interval_month_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_interval_day_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_day_interval_days INTEGER NOT NULL DEFAULT 10;
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_first_pay_mode VARCHAR(16) NOT NULL DEFAULT 'IMMEDIATE';

CREATE TABLE IF NOT EXISTS tb_split_pay_contract (
    id                          BIGSERIAL PRIMARY KEY,
    contract_no                 VARCHAR(64)  NOT NULL,
    org_unit_id                 BIGINT       NOT NULL,
    merchant_code               VARCHAR(64)  NOT NULL,
    customer_email              VARCHAR(255) NOT NULL,
    customer_name               VARCHAR(200),
    total_amount                NUMERIC(20, 4) NOT NULL,
    currency_code               VARCHAR(10)  NOT NULL DEFAULT 'JPY',
    installment_count           INTEGER      NOT NULL,
    interval_type               VARCHAR(16)  NOT NULL,
    interval_value              INTEGER      NOT NULL DEFAULT 1,
    status                      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    first_pay_mode              VARCHAR(16)  NOT NULL DEFAULT 'IMMEDIATE',
    snap_split_pay_fee_pct      NUMERIC(5, 2) NOT NULL DEFAULT 0,
    snap_split_fixed_per_inst   NUMERIC(12, 1) NOT NULL DEFAULT 0,
    snap_split_fixed_total      NUMERIC(12, 1) NOT NULL DEFAULT 0,
    contract_date               DATE         NOT NULL,
    channel                     VARCHAR(32)  NOT NULL DEFAULT 'URL',
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at                TIMESTAMP,
    CONSTRAINT uq_split_pay_contract_no UNIQUE (contract_no)
);

CREATE INDEX IF NOT EXISTS ix_split_pay_contract_merchant ON tb_split_pay_contract (merchant_code, status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_split_pay_contract_org ON tb_split_pay_contract (org_unit_id, created_at DESC);

CREATE TABLE IF NOT EXISTS tb_split_pay_installment (
    id                  BIGSERIAL PRIMARY KEY,
    contract_id         BIGINT       NOT NULL REFERENCES tb_split_pay_contract(id) ON DELETE CASCADE,
    installment_no      INTEGER      NOT NULL,
    order_no            VARCHAR(64)  NOT NULL,
    amount              NUMERIC(20, 4) NOT NULL,
    scheduled_date      DATE         NOT NULL,
    due_date_adjusted   DATE         NOT NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    pay_token           VARCHAR(64)  NOT NULL,
    pg_trn_id           VARCHAR(32),
    paid_at             TIMESTAMP,
    fee_pct_amount      NUMERIC(12, 4),
    fee_fixed_amount    NUMERIC(12, 4),
    mail_d_minus1_sent  TIMESTAMP,
    mail_d0_sent        TIMESTAMP,
    mail_d1_sent        TIMESTAMP,
    mail_d2_sent        TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_split_pay_inst_order UNIQUE (order_no),
    CONSTRAINT uq_split_pay_inst_contract_no UNIQUE (contract_id, installment_no),
    CONSTRAINT uq_split_pay_inst_token UNIQUE (pay_token)
);

CREATE INDEX IF NOT EXISTS ix_split_pay_inst_due ON tb_split_pay_installment (due_date_adjusted, status);
CREATE INDEX IF NOT EXISTS ix_split_pay_inst_contract ON tb_split_pay_installment (contract_id, installment_no);

COMMENT ON TABLE tb_split_pay_contract IS 'URL 분할구매 계약 마스터';
COMMENT ON TABLE tb_split_pay_installment IS '분할구매 회차별 결제(일반 URL 결제 order_no 매칭)';
