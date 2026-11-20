-- V320: KSNET 해외 USD 결제대행사(샌드박스 골격) + DEALMAI DEMO 바인딩
-- storeid/apiKey 시크릿은 본사 UI 또는 운영 SQL로 주입(저장소에 시크릿 미포함).
SET client_min_messages TO WARNING;

INSERT INTO tb_pg_agency (
  pg_cd, pg_nm, use_yn, operational_yn, sandbox_yn,
  merchant_mid, api_key, md5_secret_key, endpoint_api, credentials_extra_json,
  integ_noti_yn, integ_url_pay_yn, integ_web_chatbot_yn, integ_api_yn,
  integ_url_pay_repay_yn, integ_api_subscription_yn,
  pay_follow_auto_void_yn, pay_follow_email_void_yn, pay_follow_manual_void_yn,
  pay_follow_auto_refund_yn, pay_follow_manual_refund_yn, pay_follow_force_refund_yn,
  pay_follow_same_day_refund_yn, created_at
)
SELECT
  'KSNET',
  'KSNET',
  'Y',
  'Y',
  'Y',
  NULL,
  NULL,
  NULL,
  'https://pgdev.ksnet.co.kr',
  '{"showCard":"C(25)","settlementCurrencyHint":"USD","currencyHint":"USD","amountModeHint":"STANDARD"}',
  'Y',
  'Y',
  'N',
  'Y',
  'N',
  'N',
  'N',
  'N',
  'N',
  'N',
  'Y',
  'Y',
  'N',
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM tb_pg_agency WHERE UPPER(TRIM(pg_cd)) = 'KSNET'
);

UPDATE tb_pg_agency
SET
  pg_nm = 'KSNET',
  sandbox_yn = 'Y',
  use_yn = 'Y',
  operational_yn = 'Y',
  integ_url_pay_yn = 'Y',
  integ_api_yn = 'Y',
  integ_noti_yn = COALESCE(NULLIF(TRIM(integ_noti_yn), ''), 'Y'),
  endpoint_api = COALESCE(NULLIF(TRIM(endpoint_api), ''), 'https://pgdev.ksnet.co.kr'),
  credentials_extra_json = CASE
    WHEN credentials_extra_json IS NULL OR BTRIM(credentials_extra_json) = '' THEN
      '{"showCard":"C(25)","settlementCurrencyHint":"USD","currencyHint":"USD","amountModeHint":"STANDARD"}'
    ELSE credentials_extra_json
  END,
  pay_follow_manual_refund_yn = 'Y',
  pay_follow_force_refund_yn = 'Y'
WHERE UPPER(TRIM(pg_cd)) = 'KSNET';

-- 본사 URL결제설정: KSNET = STANDARD · 실결제 USD (표시통화 DP 아님)
DO $$
DECLARE
  r RECORD;
  j jsonb;
  ksnet_cfg jsonb := '{"amountMode":"STANDARD","settlementCurrency":"USD","displayCurrency":"USD","fxMode":"OFF","displayCurrencyMode":"FIXED"}'::jsonb;
BEGIN
  FOR r IN
    SELECT id, COALESCE(NULLIF(BTRIM(url_pay_display_fx_json), ''), '{}')::jsonb AS fx
    FROM tb_hq_api_config
  LOOP
    j := r.fx;
    IF jsonb_typeof(j->'pgSettings') IS DISTINCT FROM 'object' THEN
      j := jsonb_set(j, '{pgSettings}', '{}'::jsonb, true);
    END IF;
    j := jsonb_set(j, '{pgSettings,KSNET}', ksnet_cfg, true);
    UPDATE tb_hq_api_config
    SET url_pay_display_fx_json = j::text
    WHERE id = r.id;
  END LOOP;
END $$;

-- DEALMAI DEMO(6000000067): KSNET WEB 바인딩 (자격은 agency 집계 MID 사용)
INSERT INTO tb_merchant_pg_binding (
  org_unit_id, pg_cd, activation_yn, operational_yn, pay_method,
  mid, api_key, iv_key, installment_yn, sort_order,
  url_pay_pricing_mode, card_brand_scope, currency_scope, created_at
)
SELECT
  ou.id,
  'KSNET',
  'Y',
  'Y',
  'WEB',
  COALESCE(NULLIF(TRIM(ag.merchant_mid), ''), ''),
  '',
  '',
  'N',
  20,
  'CHECKOUT_CURRENCY',
  'ALL',
  'USD',
  NOW()
FROM tb_org_unit ou
LEFT JOIN tb_pg_agency ag ON UPPER(TRIM(ag.pg_cd)) = 'KSNET'
WHERE ou.code = '6000000067'
  AND NOT EXISTS (
    SELECT 1 FROM tb_merchant_pg_binding b
    WHERE b.org_unit_id = ou.id
      AND UPPER(TRIM(b.pg_cd)) = 'KSNET'
      AND UPPER(TRIM(b.pay_method)) = 'WEB'
  );

UPDATE tb_merchant_pg_binding b
SET
  activation_yn = 'Y',
  operational_yn = 'Y',
  currency_scope = 'USD',
  card_brand_scope = COALESCE(NULLIF(TRIM(b.card_brand_scope), ''), 'ALL'),
  url_pay_pricing_mode = COALESCE(NULLIF(TRIM(b.url_pay_pricing_mode), ''), 'CHECKOUT_CURRENCY')
FROM tb_org_unit ou
WHERE ou.id = b.org_unit_id
  AND ou.code = '6000000067'
  AND UPPER(TRIM(b.pg_cd)) = 'KSNET'
  AND UPPER(TRIM(b.pay_method)) = 'WEB';
