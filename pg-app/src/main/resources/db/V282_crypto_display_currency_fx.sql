-- V282: 크립토 표시통화(DP) + 입금 건 표시/송금 금액
-- 기준통화(청구)=USD 유지. 표시통화는 가맹 설정. USDC/USDT≈USD, EURC는 FX 역산.

ALTER TABLE tb_merchant_crypto_config
    ADD COLUMN IF NOT EXISTS display_currency VARCHAR(8) NOT NULL DEFAULT 'USD';

COMMENT ON COLUMN tb_merchant_crypto_config.display_currency IS
    '구매자 표시통화(JPY·USD 등). 청구·수수료·한도 앵커는 USD(기준통화).';

ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS display_amount DECIMAL(24, 8) NULL;
ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS display_currency VARCHAR(8) NULL;
ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS asset_amount DECIMAL(24, 8) NULL;

COMMENT ON COLUMN tb_crypto_deposit_txn.display_amount IS '구매자 입력·표시 금액';
COMMENT ON COLUMN tb_crypto_deposit_txn.display_currency IS '표시 통화(가맹 설정·결제 시도 통화)';
COMMENT ON COLUMN tb_crypto_deposit_txn.asset_amount IS '선택 자산 송금 금액(USDC/USDT/EURC FX 역산)';

UPDATE tb_crypto_deposit_txn
SET display_amount = amount,
    display_currency = 'USD',
    asset_amount = amount
WHERE display_amount IS NULL;
