-- V272: 가맹 크립토 기본 = 미사용(허용자산·네트워크 비활성)
-- use_yn 기본 N 유지. assets/networks 기본 USDT·TRC20,ERC20 → 빈값.

ALTER TABLE tb_merchant_crypto_config
    ALTER COLUMN assets_csv SET DEFAULT '';

ALTER TABLE tb_merchant_crypto_config
    ALTER COLUMN networks_csv SET DEFAULT '';

COMMENT ON COLUMN tb_merchant_crypto_config.use_yn IS
    '크립토 사용. 기본 N(미사용). Y=사용.';

COMMENT ON COLUMN tb_merchant_crypto_config.assets_csv IS
    '허용자산 CSV(USDC,USDT,EURC). 기본 빈값=전 자산 비활성. 미사용 시에도 빈값 권장.';

COMMENT ON COLUMN tb_merchant_crypto_config.networks_csv IS
    '활성 자산의 네트워크 CSV. 기본 빈값=미선택.';
