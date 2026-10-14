-- 수수료 채널 FIAT|CRYPTO + CRYPTO 전용 필드 + 배분 채널
-- FIAT의 USDT 송금/%·3DS 는 유지. CRYPTO 정책에는 매입/에스크로/인증 수수료 사용.

ALTER TABLE tb_commission_policy
    ADD COLUMN IF NOT EXISTS fee_channel VARCHAR(16) NOT NULL DEFAULT 'FIAT';

ALTER TABLE tb_commission_policy
    ADD COLUMN IF NOT EXISTS acquisition_ops_fee_pct NUMERIC(5, 2) NOT NULL DEFAULT 0;

ALTER TABLE tb_commission_policy
    ADD COLUMN IF NOT EXISTS escrow_ops_fee_pct NUMERIC(5, 2) NOT NULL DEFAULT 0;

ALTER TABLE tb_commission_policy
    ADD COLUMN IF NOT EXISTS auth_fee_per_tx NUMERIC(12, 1) NOT NULL DEFAULT 0;

UPDATE tb_commission_policy
SET fee_channel = 'FIAT'
WHERE fee_channel IS NULL OR TRIM(fee_channel) = '';

CREATE INDEX IF NOT EXISTS idx_commission_policy_fee_channel
    ON tb_commission_policy (fee_channel);

-- CRYPTO 기본 템플릿 (scope 유일: DEFAULT_CRYPTO)
INSERT INTO tb_commission_policy (
    scope, policy_name, deploy_yn, fee_channel, currency_code,
    per_tx_fee, cancel_rate, usage_rate, fail_fee, pay_rate, refund_rate,
    void_fee_per_tx, manual_void_fee_per_tx, fee_settlement_per_tx,
    remittance_transfer_fee, usdt_transfer_fee_usd, fee_usdt, fee_fx,
    rolling_pct, rolling_days, fee_3ds_rate, chargeback_fee_per_tx,
    acquisition_ops_fee_pct, escrow_ops_fee_pct, auth_fee_per_tx,
    created_at, updated_at
)
SELECT
    'DEFAULT_CRYPTO', 'CRYPTO 기본', 'Y', 'CRYPTO', 'USDT',
    0, 0, 0, 0, 0, 0,
    0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0,
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_commission_policy WHERE scope = 'DEFAULT_CRYPTO'
);

-- 배분: 채널별 슬롯 (comp_id + fee_channel)
ALTER TABLE tb_distribution_fee_config
    ADD COLUMN IF NOT EXISTS fee_channel VARCHAR(16) NOT NULL DEFAULT 'FIAT';

UPDATE tb_distribution_fee_config
SET fee_channel = 'FIAT'
WHERE fee_channel IS NULL OR TRIM(fee_channel) = '';

ALTER TABLE tb_distribution_fee_config DROP CONSTRAINT IF EXISTS tb_distribution_fee_config_comp_id_key;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_tb_distribution_fee_config_comp_id'
    ) THEN
        ALTER TABLE tb_distribution_fee_config DROP CONSTRAINT uk_tb_distribution_fee_config_comp_id;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_dist_fee_comp_channel
    ON tb_distribution_fee_config (comp_id, fee_channel);

-- 입금 확정 과금 스냅샷
ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS fee_amt NUMERIC(24, 8);

ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS fee_charged_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS pay_trn_id VARCHAR(20);
