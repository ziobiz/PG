-- 네트워크별 가스피(본사 공통·전 가맹 동일) + 수수료 이력 채널 + 출금 가스 차감 컬럼

CREATE TABLE IF NOT EXISTS tb_crypto_network_gas_fee (
    id              BIGSERIAL PRIMARY KEY,
    asset_code      VARCHAR(16) NOT NULL,
    network_code    VARCHAR(32) NOT NULL,
    gas_fee_amt     NUMERIC(24, 8) NOT NULL DEFAULT 0,
    use_yn          VARCHAR(1) NOT NULL DEFAULT 'Y',
    remark          VARCHAR(200),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_crypto_network_gas UNIQUE (asset_code, network_code)
);

CREATE INDEX IF NOT EXISTS idx_crypto_network_gas_use
    ON tb_crypto_network_gas_fee (use_yn);

COMMENT ON TABLE tb_crypto_network_gas_fee IS '본사 CRYPTO 네트워크별 가스피(고정·송금 자산 단위). 전 가맹 동일.';

ALTER TABLE tb_commission_history
    ADD COLUMN IF NOT EXISTS fee_channel VARCHAR(16) NOT NULL DEFAULT 'FIAT';

UPDATE tb_commission_history
SET fee_channel = 'FIAT'
WHERE fee_channel IS NULL OR TRIM(fee_channel) = '';

CREATE INDEX IF NOT EXISTS idx_commission_history_comp_channel
    ON tb_commission_history (comp_id, fee_channel);

ALTER TABLE tb_crypto_payout_queue
    ADD COLUMN IF NOT EXISTS amount_gas NUMERIC(24, 8) NOT NULL DEFAULT 0;

ALTER TABLE tb_crypto_payout_queue
    ADD COLUMN IF NOT EXISTS amount_settlement_fee NUMERIC(24, 8) NOT NULL DEFAULT 0;

UPDATE tb_crypto_payout_queue SET amount_gas = 0 WHERE amount_gas IS NULL;
UPDATE tb_crypto_payout_queue SET amount_settlement_fee = 0 WHERE amount_settlement_fee IS NULL;

-- 시드: 주요 자산×네트워크 가스피 0 (운영에서 본사 CRYPTO 설정에서 수정)
INSERT INTO tb_crypto_network_gas_fee (asset_code, network_code, gas_fee_amt, use_yn, remark)
VALUES
    ('USDC', 'ETH', 0, 'Y', 'Ethereum'),
    ('USDC', 'TRON', 0, 'Y', 'TRON'),
    ('USDC', 'POLYGON', 0, 'Y', 'Polygon'),
    ('USDT', 'ETH', 0, 'Y', 'Ethereum'),
    ('USDT', 'TRON', 0, 'Y', 'TRON'),
    ('USDT', 'POLYGON', 0, 'Y', 'Polygon'),
    ('EURC', 'ETH', 0, 'Y', 'Ethereum'),
    ('EURC', 'TRON', 0, 'Y', 'TRON'),
    ('EURC', 'POLYGON', 0, 'Y', 'Polygon')
ON CONFLICT (asset_code, network_code) DO NOTHING;
