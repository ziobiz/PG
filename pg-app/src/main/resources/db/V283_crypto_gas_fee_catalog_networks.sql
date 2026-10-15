-- V283: 네트워크별 가스피 — 카탈로그 자산×네트워크 전개(USDC/USDT/EURC) + 레거시 ETH/TRON 코드 비활성
SET client_min_messages TO WARNING;

-- 잘못된 시드 코드(ETH/TRON)는 카탈로그 코드(ERC20/TRC20)와 불일치 → 비활성
UPDATE tb_crypto_network_gas_fee
SET use_yn = 'N',
    remark = COALESCE(NULLIF(TRIM(remark), ''), 'legacy') || ' · legacy ETH/TRON code'
WHERE UPPER(network_code) IN ('ETH', 'TRON');

-- USDC (Native USDC — TRC20 없음)
INSERT INTO tb_crypto_network_gas_fee (asset_code, network_code, gas_fee_amt, use_yn, remark)
VALUES
    ('USDC', 'SOL',      0, 'Y', 'USDC 주력 · Solana'),
    ('USDC', 'BASE',     0, 'Y', 'USDC 주력 · Base'),
    ('USDC', 'ERC20',    0, 'Y', 'Ethereum'),
    ('USDC', 'BEP20',    0, 'Y', 'BNB Smart Chain'),
    ('USDC', 'POLYGON',  0, 'Y', 'Polygon'),
    ('USDC', 'ARBITRUM', 0, 'Y', 'Arbitrum One'),
    ('USDC', 'OPTIMISM', 0, 'Y', 'OP Mainnet'),
    ('USDC', 'AVAXC',    0, 'Y', 'Avalanche C-Chain'),
    ('USDC', 'OPBNB',    0, 'Y', 'opBNB'),
    ('USDC', 'NEAR',     0, 'Y', 'NEAR'),
    ('USDC', 'KAIA',     0, 'Y', 'Kaia'),
    ('USDC', 'STELLAR',  0, 'Y', 'Stellar')
ON CONFLICT (asset_code, network_code) DO UPDATE SET
    use_yn = 'Y',
    remark = COALESCE(NULLIF(TRIM(tb_crypto_network_gas_fee.remark), ''), EXCLUDED.remark);

-- USDT (주력 TRC20)
INSERT INTO tb_crypto_network_gas_fee (asset_code, network_code, gas_fee_amt, use_yn, remark)
VALUES
    ('USDT', 'TRC20',    0, 'Y', 'USDT 주력 · Tron'),
    ('USDT', 'ERC20',    0, 'Y', 'Ethereum'),
    ('USDT', 'BEP20',    0, 'Y', 'BNB Smart Chain'),
    ('USDT', 'POLYGON',  0, 'Y', 'Polygon'),
    ('USDT', 'ARBITRUM', 0, 'Y', 'Arbitrum One'),
    ('USDT', 'SOL',      0, 'Y', 'Solana'),
    ('USDT', 'OPTIMISM', 0, 'Y', 'OP Mainnet'),
    ('USDT', 'AVAXC',    0, 'Y', 'Avalanche C-Chain'),
    ('USDT', 'BASE',     0, 'Y', 'Base (bridged USDT)'),
    ('USDT', 'TON',      0, 'Y', 'TON'),
    ('USDT', 'KAIA',     0, 'Y', 'Kaia'),
    ('USDT', 'STELLAR',  0, 'Y', 'Stellar')
ON CONFLICT (asset_code, network_code) DO UPDATE SET
    use_yn = 'Y',
    remark = COALESCE(NULLIF(TRIM(tb_crypto_network_gas_fee.remark), ''), EXCLUDED.remark);

-- EURC (Circle 주요 + 카탈로그 확장)
INSERT INTO tb_crypto_network_gas_fee (asset_code, network_code, gas_fee_amt, use_yn, remark)
VALUES
    ('EURC', 'ERC20',    0, 'Y', 'EURC 주력 · Ethereum'),
    ('EURC', 'BASE',     0, 'Y', 'EURC 주력 · Base'),
    ('EURC', 'SOL',      0, 'Y', 'EURC 주력 · Solana'),
    ('EURC', 'POLYGON',  0, 'Y', 'Polygon'),
    ('EURC', 'ARBITRUM', 0, 'Y', 'Arbitrum One'),
    ('EURC', 'OPTIMISM', 0, 'Y', 'OP Mainnet'),
    ('EURC', 'AVAXC',    0, 'Y', 'Avalanche C-Chain'),
    ('EURC', 'BEP20',    0, 'Y', 'BNB Smart Chain'),
    ('EURC', 'KAIA',     0, 'Y', 'Kaia'),
    ('EURC', 'STELLAR',  0, 'Y', 'Stellar')
ON CONFLICT (asset_code, network_code) DO UPDATE SET
    use_yn = 'Y',
    remark = COALESCE(NULLIF(TRIM(tb_crypto_network_gas_fee.remark), ''), EXCLUDED.remark);

COMMENT ON TABLE tb_crypto_network_gas_fee IS '본사 CRYPTO 네트워크별 가스피(고정·송금 자산 단위). USDC/USDT/EURC 탭·카탈로그 페어 기준. 전 가맹 동일.';
