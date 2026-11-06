-- V311: 큐뱅(QBANG) 라이브 환경 — posId/MID 8601191, api.qr-road.com
-- agencyCode=OTL(온더라인) 유지. 가맹 binding mid 는 표시·기록용(posId 덮어쓰기 안 함).
SET client_min_messages TO WARNING;

-- 1) LIVE agency 행 보장 (pg_cd=QBANG)
INSERT INTO tb_pg_agency (
  pg_cd, pg_nm, use_yn, operational_yn, sandbox_yn,
  merchant_mid, api_key, endpoint_api, credentials_extra_json,
  integ_noti_yn, integ_url_pay_yn, integ_web_chatbot_yn, integ_api_yn,
  integ_url_pay_repay_yn, integ_api_subscription_yn,
  pay_follow_auto_void_yn, pay_follow_email_void_yn, pay_follow_manual_void_yn,
  pay_follow_auto_refund_yn, pay_follow_manual_refund_yn, pay_follow_force_refund_yn,
  pay_follow_same_day_refund_yn, created_at
)
SELECT
  'QBANG',
  'QBANG',
  'Y',
  'Y',
  'N',
  '8601191',
  'OTL',
  'https://api.qr-road.com/endpoints/pos/command',
  '{"posId":"8601191","agencyCode":"OTL","env":"LIVE","partner":"온더라인"}',
  'Y',
  'Y',
  'N',
  'Y',
  'N',
  'N',
  'N',
  'N',
  'N',
  'Y',
  'N',
  'Y',
  'N',
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM tb_pg_agency WHERE UPPER(TRIM(pg_cd)) = 'QBANG'
);

UPDATE tb_pg_agency
SET
  pg_nm = 'QBANG',
  merchant_mid = '8601191',
  api_key = 'OTL',
  sandbox_yn = 'N',
  use_yn = 'Y',
  operational_yn = 'Y',
  integ_url_pay_yn = 'Y',
  integ_api_yn = 'Y',
  integ_noti_yn = COALESCE(NULLIF(TRIM(integ_noti_yn), ''), 'Y'),
  endpoint_api = 'https://api.qr-road.com/endpoints/pos/command',
  credentials_extra_json = '{"posId":"8601191","agencyCode":"OTL","env":"LIVE","partner":"온더라인"}'
WHERE UPPER(TRIM(pg_cd)) = 'QBANG';

-- 2) 기존 DEV 행(QBANG SANDBOX)도 동일 LIVE 자격으로 맞춤(잔존 바인딩 대비) — 표시명은 구분
UPDATE tb_pg_agency
SET
  merchant_mid = '8601191',
  api_key = 'OTL',
  sandbox_yn = 'N',
  use_yn = 'Y',
  operational_yn = 'Y',
  integ_url_pay_yn = 'Y',
  integ_api_yn = 'Y',
  endpoint_api = 'https://api.qr-road.com/endpoints/pos/command',
  credentials_extra_json = '{"posId":"8601191","agencyCode":"OTL","env":"LIVE","partner":"온더라인","migratedFrom":"QBANG SANDBOX"}'
WHERE UPPER(TRIM(pg_cd)) = 'QBANG SANDBOX';

-- 3) 가맹 바인딩 LIVE mid 동기화 + DEV→QBANG 이전(중복 시 SANDBOX만 비활성)
UPDATE tb_merchant_pg_binding b
SET
  mid = '8601191',
  api_key = 'OTL',
  activation_yn = 'Y',
  operational_yn = 'Y',
  qr_wallet_scope = COALESCE(NULLIF(TRIM(b.qr_wallet_scope), ''), 'QBANG'),
  card_brand_scope = COALESCE(NULLIF(TRIM(b.card_brand_scope), ''), 'NONE')
WHERE UPPER(TRIM(b.pg_cd)) = 'QBANG';

-- 이미 QBANG 행이 있는 가맹의 SANDBOX 바인딩 → 비활성
UPDATE tb_merchant_pg_binding b
SET activation_yn = 'N', operational_yn = 'N'
WHERE UPPER(TRIM(b.pg_cd)) = 'QBANG SANDBOX'
  AND EXISTS (
    SELECT 1 FROM tb_merchant_pg_binding x
    WHERE x.org_unit_id = b.org_unit_id
      AND UPPER(TRIM(x.pay_method)) = UPPER(TRIM(COALESCE(b.pay_method, '')))
      AND UPPER(TRIM(x.pg_cd)) = 'QBANG'
  );

-- QBANG 행이 없는 가맹의 SANDBOX → pg_cd=QBANG 로 이전
UPDATE tb_merchant_pg_binding b
SET
  pg_cd = 'QBANG',
  mid = '8601191',
  api_key = 'OTL',
  activation_yn = 'Y',
  operational_yn = 'Y',
  qr_wallet_scope = COALESCE(NULLIF(TRIM(b.qr_wallet_scope), ''), 'QBANG'),
  card_brand_scope = COALESCE(NULLIF(TRIM(b.card_brand_scope), ''), 'NONE')
WHERE UPPER(TRIM(b.pg_cd)) = 'QBANG SANDBOX'
  AND NOT EXISTS (
    SELECT 1 FROM tb_merchant_pg_binding x
    WHERE x.org_unit_id = b.org_unit_id
      AND UPPER(TRIM(x.pay_method)) = UPPER(TRIM(COALESCE(b.pay_method, '')))
      AND UPPER(TRIM(x.pg_cd)) = 'QBANG'
  );

-- 4) 본사 URL결제설정: QBANG LIVE KRW + 구키 정리
DO $$
DECLARE
  j jsonb;
  row_id bigint;
  live_cfg jsonb := '{"amountMode":"STANDARD","settlementCurrency":"KRW","displayCurrency":"THB","fxMode":"AUTO"}'::jsonb;
BEGIN
  SELECT id, COALESCE(NULLIF(BTRIM(url_pay_display_fx_json), ''), '{}')::jsonb
    INTO row_id, j
  FROM tb_hq_api_config
  ORDER BY id
  LIMIT 1;

  IF row_id IS NULL THEN
    RETURN;
  END IF;

  IF jsonb_typeof(j->'pgSettings') IS DISTINCT FROM 'object' THEN
    j := jsonb_set(j, '{pgSettings}', '{}'::jsonb, true);
  END IF;

  j := jsonb_set(j, '{pgSettings,QBANG}', live_cfg, true);
  -- 구 DEV 키도 LIVE와 동일 통화로 맞춤(잔존 참조 대비)
  j := jsonb_set(j, '{pgSettings,QBANG SANDBOX}', live_cfg, true);

  UPDATE tb_hq_api_config
  SET url_pay_display_fx_json = j::text
  WHERE id = row_id;
END $$;
