-- V319: PAYSO JPDP 결제대행사 (표시통화 DP · 실결제 THB)
-- pg_cd=PAYSO JPDP → isPaysoFamily(PAYSO_*) 동일 연동. API Key/Secret 은 본사 UI에 입력(저장소에 시크릿 미포함).
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
  'PAYSO JPDP',
  'PAYSO JPDP',
  'Y',
  'Y',
  'N',
  '73154816',
  NULL,
  NULL,
  'https://apis.paysolutions.asia',
  '{"terminalId":"TID00001","keyVersion":1,"env":"LIVE","partner":"Dealmai","paymentChannel":"https://pay.sn/dealmai","amountModeHint":"DISPLAY","settlementCurrencyHint":"THB","displayCurrencyHint":"JPY"}',
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
  'N',
  'N',
  'N',
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM tb_pg_agency WHERE UPPER(TRIM(pg_cd)) = 'PAYSO JPDP'
);

UPDATE tb_pg_agency
SET
  pg_nm = 'PAYSO JPDP',
  merchant_mid = COALESCE(NULLIF(TRIM(merchant_mid), ''), '73154816'),
  sandbox_yn = 'N',
  use_yn = 'Y',
  operational_yn = 'Y',
  integ_url_pay_yn = 'Y',
  integ_api_yn = 'Y',
  integ_noti_yn = COALESCE(NULLIF(TRIM(integ_noti_yn), ''), 'Y'),
  endpoint_api = COALESCE(NULLIF(TRIM(endpoint_api), ''), 'https://apis.paysolutions.asia'),
  credentials_extra_json = CASE
    WHEN credentials_extra_json IS NULL OR BTRIM(credentials_extra_json) = '' THEN
      '{"terminalId":"TID00001","keyVersion":1,"env":"LIVE","partner":"Dealmai","paymentChannel":"https://pay.sn/dealmai","amountModeHint":"DISPLAY","settlementCurrencyHint":"THB","displayCurrencyHint":"JPY"}'
    ELSE credentials_extra_json
  END
WHERE UPPER(TRIM(pg_cd)) = 'PAYSO JPDP';

-- 본사 URL결제설정 pgSettings: DISPLAY · 표시 JPY · 실결제 THB
DO $$
DECLARE
  r RECORD;
  j jsonb;
  jpdp_cfg jsonb := '{"amountMode":"DISPLAY","settlementCurrency":"THB","displayCurrency":"JPY","fxMode":"AUTO","displayCurrencyMode":"FIXED"}'::jsonb;
BEGIN
  FOR r IN
    SELECT id, COALESCE(NULLIF(BTRIM(url_pay_display_fx_json), ''), '{}')::jsonb AS fx
    FROM tb_hq_api_config
  LOOP
    j := r.fx;
    IF jsonb_typeof(j->'pgSettings') IS DISTINCT FROM 'object' THEN
      j := jsonb_set(j, '{pgSettings}', '{}'::jsonb, true);
    END IF;
    j := jsonb_set(j, '{pgSettings,PAYSO JPDP}', jpdp_cfg, true);
    UPDATE tb_hq_api_config
    SET url_pay_display_fx_json = j::text
    WHERE id = r.id;
  END LOOP;
END $$;
