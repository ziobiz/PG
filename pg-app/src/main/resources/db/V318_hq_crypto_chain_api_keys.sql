-- 본사 크립토설정: 온체인 입금 감시 API 키 (Alchemy EVM · TronGrid TRC20)
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS crypto_alchemy_api_key VARCHAR(512);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS crypto_trongrid_api_key VARCHAR(512);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS crypto_chain_watch_enabled_yn VARCHAR(1) DEFAULT 'Y';

COMMENT ON COLUMN tb_hq_api_config.crypto_alchemy_api_key IS
    'Alchemy API Key (Ethereum/Base 등 EVM). 비우면 ALCHEMY_API_KEY 환경변수·yml';
COMMENT ON COLUMN tb_hq_api_config.crypto_trongrid_api_key IS
    'TronGrid API Key (TRC20). 비우면 TRONGRID_API_KEY 환경변수·yml';
COMMENT ON COLUMN tb_hq_api_config.crypto_chain_watch_enabled_yn IS
    '온체인 입금 감시 Y/N. 비우면 pg.crypto.chain.enabled(yml) 따름';
