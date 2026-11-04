-- V306: DEALMAI DEMO 크립토 허용 네트워크에 BEP20 추가
-- ReplaceMe(TRC20/ERC20) 비활성 후 본사 실수탁(BEP20 USDT/USDC)과 매칭되도록.

SET client_min_messages TO WARNING;

UPDATE tb_merchant_crypto_config m
SET
  networks_csv = CASE
    WHEN COALESCE(TRIM(m.networks_csv), '') = '' THEN 'BEP20,TRC20,ERC20'
    WHEN UPPER(',' || REPLACE(REPLACE(m.networks_csv, ' ', ''), ';', ',') || ',')
         LIKE '%,BEP20,%' THEN m.networks_csv
    ELSE 'BEP20,' || TRIM(m.networks_csv)
  END,
  assets_csv = CASE
    WHEN COALESCE(TRIM(m.assets_csv), '') = '' THEN 'USDT,USDC'
    ELSE m.assets_csv
  END,
  use_yn = 'Y',
  updated_at = NOW()
FROM tb_org_unit ou
WHERE m.org_unit_id = ou.id
  AND ou.code = '6000000067';
