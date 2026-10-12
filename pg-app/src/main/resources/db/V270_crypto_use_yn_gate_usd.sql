-- V270: 크립토 게이트는 use_yn만 · 레거시 checkout_means 동기화 · USD 기준 주석
SET client_min_messages TO WARNING;

COMMENT ON TABLE tb_merchant_crypto_config IS
    '가맹 크립토. 활성=use_yn=Y(+업체등록 사용 Y). checkout_means는 레거시(게이트 미사용). 금액·수수료·한도 기준 USD.';

COMMENT ON COLUMN tb_merchant_crypto_config.checkout_means IS
    '레거시. UI/게이트 미사용. use_yn=Y이면 CRYPTO, N이면 CARD로 동기화만 유지.';

COMMENT ON COLUMN tb_crypto_deposit_txn.amount IS
    '입금 요청 금액. USDT/USDC는 USD 액면. 견적·수수료·한도 기준통화 USD.';

-- 기존: use_yn=Y 인데 means가 CARD라 크립토가 꺼져 있던 건 CRYPTO로 맞춤
UPDATE tb_merchant_crypto_config
SET checkout_means = 'CRYPTO'
WHERE UPPER(COALESCE(use_yn, 'N')) = 'Y'
  AND UPPER(COALESCE(checkout_means, '')) NOT IN ('CRYPTO', 'BOTH');

UPDATE tb_merchant_crypto_config
SET checkout_means = 'CARD'
WHERE UPPER(COALESCE(use_yn, 'N')) <> 'Y';
