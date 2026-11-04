-- V307: 본사 대표수탁 — Ethereum(ERC20) USDT·USDC 실주소 등록
-- 주소는 기존 BEP20 수탁과 동일(EVM). ReplaceMe ERC20 공용행은 유지(비활성).

SET client_min_messages TO WARNING;

-- 동일 자산×네트워크의 기존 활성행은 비활성(대표 중복 방지)
UPDATE tb_crypto_custody_address
SET use_yn = 'N', primary_yn = 'N'
WHERE UPPER(TRIM(network_code)) = 'ERC20'
  AND UPPER(TRIM(COALESCE(asset_code, ''))) IN ('USDT', 'USDC')
  AND use_yn = 'Y';

-- USDT × ERC20
INSERT INTO tb_crypto_custody_address (
  network_code, asset_code, address, label, use_yn, primary_yn
)
SELECT
  'ERC20',
  'USDT',
  '0xbb837ecfb2d879b6036b8cfb000b26f68b7ca370',
  'HQ USDT ERC20 (Ethereum)',
  'Y',
  'Y'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_crypto_custody_address c
  WHERE UPPER(TRIM(c.network_code)) = 'ERC20'
    AND UPPER(TRIM(COALESCE(c.asset_code, ''))) = 'USDT'
    AND LOWER(TRIM(c.address)) = LOWER('0xbb837ecfb2d879b6036b8cfb000b26f68b7ca370')
);

UPDATE tb_crypto_custody_address
SET
  use_yn = 'Y',
  primary_yn = 'Y',
  label = COALESCE(NULLIF(TRIM(label), ''), 'HQ USDT ERC20 (Ethereum)')
WHERE UPPER(TRIM(network_code)) = 'ERC20'
  AND UPPER(TRIM(COALESCE(asset_code, ''))) = 'USDT'
  AND LOWER(TRIM(address)) = LOWER('0xbb837ecfb2d879b6036b8cfb000b26f68b7ca370');

-- USDC × ERC20
INSERT INTO tb_crypto_custody_address (
  network_code, asset_code, address, label, use_yn, primary_yn
)
SELECT
  'ERC20',
  'USDC',
  '0xbb837ecfb2d879b6036b8cfb000b26f68b7ca370',
  'HQ USDC ERC20 (Ethereum)',
  'Y',
  'Y'
WHERE NOT EXISTS (
  SELECT 1 FROM tb_crypto_custody_address c
  WHERE UPPER(TRIM(c.network_code)) = 'ERC20'
    AND UPPER(TRIM(COALESCE(c.asset_code, ''))) = 'USDC'
    AND LOWER(TRIM(c.address)) = LOWER('0xbb837ecfb2d879b6036b8cfb000b26f68b7ca370')
);

UPDATE tb_crypto_custody_address
SET
  use_yn = 'Y',
  primary_yn = 'Y',
  label = COALESCE(NULLIF(TRIM(label), ''), 'HQ USDC ERC20 (Ethereum)')
WHERE UPPER(TRIM(network_code)) = 'ERC20'
  AND UPPER(TRIM(COALESCE(asset_code, ''))) = 'USDC'
  AND LOWER(TRIM(address)) = LOWER('0xbb837ecfb2d879b6036b8cfb000b26f68b7ca370');

-- DEALMAI DEMO: Ethereum(ERC20) + BEP20 허용, USDT·USDC
UPDATE tb_merchant_crypto_config m
SET
  use_yn = 'Y',
  assets_csv = 'USDT,USDC',
  networks_csv = 'ERC20,BEP20',
  updated_at = NOW()
FROM tb_org_unit ou
WHERE m.org_unit_id = ou.id
  AND ou.code = '6000000067';
