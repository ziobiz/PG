-- V300: DEALMAI DEMO(6000000067) 통합결제에 크립토 수단도 노출 (카드·QR·크립토)
SET client_min_messages TO WARNING;

INSERT INTO tb_merchant_crypto_config (
  org_unit_id, use_yn, checkout_means, settle_mode, payout_mode, hold_days,
  reserve_pct, reserve_follow_hq, display_currency, assets_csv, networks_csv,
  seller_wallet_json, updated_at
)
SELECT
  ou.id,
  'Y',
  'BOTH',
  'SCHEDULED',
  'MANUAL',
  3,
  0,
  'Y',
  'USD',
  'USDT',
  'TRC20,ERC20',
  NULL,
  NOW()
FROM tb_org_unit ou
WHERE ou.code = '6000000067'
ON CONFLICT (org_unit_id) DO UPDATE SET
  use_yn = 'Y',
  checkout_means = 'BOTH',
  display_currency = COALESCE(NULLIF(TRIM(tb_merchant_crypto_config.display_currency), ''), 'USD'),
  assets_csv = CASE
    WHEN COALESCE(TRIM(tb_merchant_crypto_config.assets_csv), '') = '' THEN 'USDT'
    ELSE tb_merchant_crypto_config.assets_csv
  END,
  networks_csv = CASE
    WHEN COALESCE(TRIM(tb_merchant_crypto_config.networks_csv), '') = '' THEN 'TRC20,ERC20'
    ELSE tb_merchant_crypto_config.networks_csv
  END,
  updated_at = NOW();
