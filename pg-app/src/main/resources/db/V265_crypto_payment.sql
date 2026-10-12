-- V265: ICOPAY 크립토 결제·정산 (USDT/USDC/EURC · MANUAL/AUTO · IMMEDIATE/SCHEDULED)
SET client_min_messages TO WARNING;

CREATE TABLE IF NOT EXISTS tb_crypto_asset (
    code            VARCHAR(16) PRIMARY KEY,
    display_name    VARCHAR(64) NOT NULL,
    symbol          VARCHAR(16) NOT NULL,
    pastel_bg       VARCHAR(16) NOT NULL,
    pastel_fg       VARCHAR(16) NOT NULL,
    icon_key        VARCHAR(32) NOT NULL,
    sort_ord        INTEGER NOT NULL DEFAULT 0,
    use_yn          VARCHAR(1) NOT NULL DEFAULT 'Y'
);

CREATE TABLE IF NOT EXISTS tb_crypto_network (
    code            VARCHAR(16) PRIMARY KEY,
    display_name    VARCHAR(64) NOT NULL,
    chain_hint      VARCHAR(64),
    confirmations   INTEGER NOT NULL DEFAULT 1,
    pastel_bg       VARCHAR(16) NOT NULL,
    pastel_fg       VARCHAR(16) NOT NULL,
    sort_ord        INTEGER NOT NULL DEFAULT 0,
    use_yn          VARCHAR(1) NOT NULL DEFAULT 'Y'
);

CREATE TABLE IF NOT EXISTS tb_crypto_asset_network (
    asset_code      VARCHAR(16) NOT NULL REFERENCES tb_crypto_asset(code),
    network_code    VARCHAR(16) NOT NULL REFERENCES tb_crypto_network(code),
    contract_hint   VARCHAR(128),
    decimals        INTEGER NOT NULL DEFAULT 6,
    use_yn          VARCHAR(1) NOT NULL DEFAULT 'Y',
    PRIMARY KEY (asset_code, network_code)
);

CREATE TABLE IF NOT EXISTS tb_crypto_wallet_brand (
    code            VARCHAR(32) PRIMARY KEY,
    display_name    VARCHAR(64) NOT NULL,
    pastel_bg       VARCHAR(16) NOT NULL,
    pastel_fg       VARCHAR(16) NOT NULL,
    accent          VARCHAR(16) NOT NULL,
    icon_key        VARCHAR(32) NOT NULL,
    networks_csv    VARCHAR(128) NOT NULL,
    sort_ord        INTEGER NOT NULL DEFAULT 0,
    use_yn          VARCHAR(1) NOT NULL DEFAULT 'Y'
);

CREATE TABLE IF NOT EXISTS tb_crypto_custody_address (
    id              BIGSERIAL PRIMARY KEY,
    network_code    VARCHAR(16) NOT NULL REFERENCES tb_crypto_network(code),
    asset_code      VARCHAR(16) REFERENCES tb_crypto_asset(code),
    address         VARCHAR(128) NOT NULL,
    label           VARCHAR(100),
    use_yn          VARCHAR(1) NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_crypto_custody_net
    ON tb_crypto_custody_address (network_code, use_yn);

CREATE TABLE IF NOT EXISTS tb_merchant_crypto_config (
    org_unit_id     BIGINT PRIMARY KEY,
    use_yn          VARCHAR(1) NOT NULL DEFAULT 'N',
    checkout_means  VARCHAR(16) NOT NULL DEFAULT 'CARD',
    settle_mode     VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    payout_mode     VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    hold_days       INTEGER NOT NULL DEFAULT 3,
    assets_csv      VARCHAR(128) NOT NULL DEFAULT 'USDT',
    networks_csv    VARCHAR(128) NOT NULL DEFAULT 'TRC20,ERC20',
    seller_wallet_json TEXT,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN tb_merchant_crypto_config.checkout_means IS 'CARD | CRYPTO | BOTH';
COMMENT ON COLUMN tb_merchant_crypto_config.settle_mode IS 'IMMEDIATE | SCHEDULED';
COMMENT ON COLUMN tb_merchant_crypto_config.payout_mode IS 'MANUAL | AUTO';

CREATE TABLE IF NOT EXISTS tb_crypto_deposit_txn (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NOT NULL,
    comp_id         VARCHAR(32) NOT NULL,
    order_no        VARCHAR(100) NOT NULL,
    amount          NUMERIC(24, 8) NOT NULL,
    asset_code      VARCHAR(16) NOT NULL,
    network_code    VARCHAR(16) NOT NULL,
    product_name    VARCHAR(200),
    buyer_email     VARCHAR(200),
    buyer_name      VARCHAR(100),
    custody_address VARCHAR(128) NOT NULL,
    session_token   VARCHAR(128) NOT NULL,
    status          VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    settle_mode     VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    payout_mode     VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    deposit_tx_hash VARCHAR(128),
    confirmed_at    TIMESTAMP,
    expire_at       TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_crypto_deposit_order UNIQUE (org_unit_id, order_no)
);

CREATE INDEX IF NOT EXISTS ix_crypto_deposit_status
    ON tb_crypto_deposit_txn (status, created_at);
CREATE INDEX IF NOT EXISTS ix_crypto_deposit_token
    ON tb_crypto_deposit_txn (session_token);

CREATE TABLE IF NOT EXISTS tb_crypto_payout_queue (
    id              BIGSERIAL PRIMARY KEY,
    deposit_txn_id  BIGINT NOT NULL REFERENCES tb_crypto_deposit_txn(id),
    org_unit_id     BIGINT NOT NULL,
    comp_id         VARCHAR(32) NOT NULL,
    order_no        VARCHAR(100) NOT NULL,
    asset_code      VARCHAR(16) NOT NULL,
    network_code    VARCHAR(16) NOT NULL,
    amount_gross    NUMERIC(24, 8) NOT NULL,
    amount_fee      NUMERIC(24, 8) NOT NULL DEFAULT 0,
    amount_net      NUMERIC(24, 8) NOT NULL,
    settle_mode     VARCHAR(16) NOT NULL,
    payout_mode     VARCHAR(16) NOT NULL,
    status          VARCHAR(24) NOT NULL DEFAULT 'QUEUED',
    seller_address  VARCHAR(128),
    payout_tx_hash  VARCHAR(128),
    scheduled_at    TIMESTAMP,
    processed_at    TIMESTAMP,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_crypto_payout_status
    ON tb_crypto_payout_queue (status, scheduled_at);

-- 시드: 자산
INSERT INTO tb_crypto_asset (code, display_name, symbol, pastel_bg, pastel_fg, icon_key, sort_ord, use_yn) VALUES
 ('USDC', 'USDC (Circle)', 'USDC', '#DBEAFE', '#1D4ED8', 'usdc', 1, 'Y'),
 ('USDT', 'USDT (Tether)', 'USDT', '#D4F5E9', '#0F766E', 'usdt', 2, 'Y'),
 ('EURC', 'EURC (Euro Coin)', 'EURC', '#EDE9FE', '#6D28D9', 'eurc', 3, 'Y')
ON CONFLICT (code) DO UPDATE SET
 display_name = EXCLUDED.display_name,
 pastel_bg = EXCLUDED.pastel_bg,
 pastel_fg = EXCLUDED.pastel_fg,
 sort_ord = EXCLUDED.sort_ord,
 use_yn = EXCLUDED.use_yn;

INSERT INTO tb_crypto_network (code, display_name, chain_hint, confirmations, pastel_bg, pastel_fg, sort_ord, use_yn) VALUES
 ('TRC20', 'TRON (TRC-20)', 'TRON', 1, '#FFE4E6', '#BE123C', 1, 'Y'),
 ('ERC20', 'Ethereum (ERC-20)', 'ETH', 12, '#E0E7FF', '#3730A3', 2, 'Y')
ON CONFLICT (code) DO UPDATE SET
 display_name = EXCLUDED.display_name,
 pastel_bg = EXCLUDED.pastel_bg,
 pastel_fg = EXCLUDED.pastel_fg;

INSERT INTO tb_crypto_asset_network (asset_code, network_code, contract_hint, decimals, use_yn) VALUES
 ('USDT', 'TRC20', 'TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t', 6, 'Y'),
 ('USDT', 'ERC20', '0xdac17f958d2ee523a2206206994597c13d831ec7', 6, 'Y'),
 ('USDC', 'ERC20', '0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48', 6, 'Y'),
 ('EURC', 'ERC20', '0x1abaea1f7c830bd89acc67ec4af516284b1bc33c', 6, 'Y')
ON CONFLICT (asset_code, network_code) DO UPDATE SET use_yn = EXCLUDED.use_yn;

INSERT INTO tb_crypto_wallet_brand (code, display_name, pastel_bg, pastel_fg, accent, icon_key, networks_csv, sort_ord, use_yn) VALUES
 ('METAMASK', 'MetaMask', '#FFE8D6', '#9A3412', '#EA580C', 'metamask', 'ERC20', 1, 'Y'),
 ('TRUST', 'Trust Wallet', '#D6F0FF', '#075985', '#0284C7', 'trust', 'TRC20,ERC20', 2, 'Y'),
 ('COINBASE', 'Coinbase Wallet', '#DBE4FF', '#1E3A8A', '#2563EB', 'coinbase', 'ERC20', 3, 'Y'),
 ('BINANCE', 'Binance Wallet', '#FFF3C4', '#854D0E', '#CA8A04', 'binance', 'TRC20,ERC20', 4, 'Y'),
 ('OKX', 'OKX Wallet', '#E5E7EB', '#111827', '#111827', 'okx', 'TRC20,ERC20', 5, 'Y'),
 ('TRONLINK', 'TronLink', '#D6E8FF', '#1E3A8A', '#1D4ED8', 'tronlink', 'TRC20', 6, 'Y'),
 ('TOKENPOCKET', 'TokenPocket', '#DCEBFF', '#1E40AF', '#2563EB', 'tokenpocket', 'TRC20,ERC20', 7, 'Y'),
 ('PHANTOM', 'Phantom', '#E4E2FF', '#4C1D95', '#5B21B6', 'phantom', 'ERC20', 8, 'Y'),
 ('BITGET', 'Bitget Wallet', '#D1FAE5', '#065F46', '#059669', 'bitget', 'TRC20,ERC20', 9, 'Y'),
 ('IMTOKEN', 'imToken', '#E0E7FF', '#312E81', '#4338CA', 'imtoken', 'TRC20,ERC20', 10, 'Y')
ON CONFLICT (code) DO UPDATE SET
 pastel_bg = EXCLUDED.pastel_bg,
 pastel_fg = EXCLUDED.pastel_fg,
 accent = EXCLUDED.accent,
 networks_csv = EXCLUDED.networks_csv,
 use_yn = 'Y';

-- 플레이스홀더 수탁 주소 (운영에서 본사 크립토설정으로 교체)
INSERT INTO tb_crypto_custody_address (network_code, asset_code, address, label, use_yn)
SELECT 'TRC20', 'USDT', 'TICOPayCustodyReplaceMeTRC20USDT0001', 'HQ USDT TRC20 (replace)', 'Y'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_crypto_custody_address WHERE network_code = 'TRC20' AND COALESCE(asset_code,'') = 'USDT'
);

INSERT INTO tb_crypto_custody_address (network_code, asset_code, address, label, use_yn)
SELECT 'ERC20', NULL, '0xICOPayCustodyReplaceMeERC200000000001', 'HQ ERC20 shared (replace)', 'Y'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_crypto_custody_address WHERE network_code = 'ERC20' AND asset_code IS NULL
);
