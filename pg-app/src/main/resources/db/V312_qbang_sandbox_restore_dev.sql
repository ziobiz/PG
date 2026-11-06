-- V312: QBANG LIVE(8601191)와 QBANG SANDBOX(DEV) 분리
-- LIVE 전환 시 SANDBOX 행까지 LIVE로 맞춰 둔 것을 되돌림.
-- 운영 가맹 바인딩은 QBANG(LIVE)만 유지. SANDBOX 바인딩은 비활성.
SET client_min_messages TO WARNING;

-- 1) LIVE 유지·명확화
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

-- 2) SANDBOX = DEV(온더라인) 복원 — LIVE MID/URL 과 분리
UPDATE tb_pg_agency
SET
  pg_nm = 'QBANG SANDBOX',
  merchant_mid = '00000000-2026-1103-0004-006700912345',
  api_key = 'OTL',
  sandbox_yn = 'Y',
  use_yn = 'Y',
  operational_yn = 'Y',
  integ_url_pay_yn = 'Y',
  integ_api_yn = 'Y',
  integ_noti_yn = COALESCE(NULLIF(TRIM(integ_noti_yn), ''), 'Y'),
  endpoint_api = 'https://api-test.qr-road.com/endpoints/pos/command',
  credentials_extra_json = '{"posId":"00000000-2026-1103-0004-006700912345","agencyCode":"OTL","env":"DEV","partner":"온더라인","sandboxThbToKrw":40}'
WHERE UPPER(TRIM(pg_cd)) = 'QBANG SANDBOX';

-- 3) 운영 바인딩은 LIVE만 — SANDBOX 잔존 바인딩 비활성(실결제 혼선 방지)
UPDATE tb_merchant_pg_binding b
SET
  mid = '8601191',
  api_key = 'OTL',
  activation_yn = 'Y',
  operational_yn = 'Y',
  qr_wallet_scope = COALESCE(NULLIF(TRIM(b.qr_wallet_scope), ''), 'QBANG'),
  card_brand_scope = COALESCE(NULLIF(TRIM(b.card_brand_scope), ''), 'NONE')
WHERE UPPER(TRIM(b.pg_cd)) = 'QBANG';

UPDATE tb_merchant_pg_binding b
SET
  activation_yn = 'N',
  operational_yn = 'N',
  mid = '00000000-2026-1103-0004-006700912345',
  api_key = 'OTL'
WHERE UPPER(TRIM(b.pg_cd)) = 'QBANG SANDBOX';

-- 4) 본사 URL결제설정: LIVE·SANDBOX 키 각각
DO $$
DECLARE
  j jsonb;
  row_id bigint;
  live_cfg jsonb := '{"amountMode":"STANDARD","settlementCurrency":"KRW","displayCurrency":"THB","fxMode":"AUTO"}'::jsonb;
  sand_cfg jsonb := '{"amountMode":"STANDARD","settlementCurrency":"KRW","displayCurrency":"THB","fxMode":"AUTO"}'::jsonb;
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
  j := jsonb_set(j, '{pgSettings,QBANG SANDBOX}', sand_cfg, true);

  UPDATE tb_hq_api_config
  SET url_pay_display_fx_json = j::text
  WHERE id = row_id;
END $$;
