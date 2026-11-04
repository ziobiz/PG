-- V302: QBANG SANDBOX 실결제 통화 KRW (큐뱅 DEV POS)
-- amountMode=STANDARD: API start 시 fxQuoteToken 없이 QbangPaymentService가 THB→KRW 환전
SET client_min_messages TO WARNING;

DO $$
DECLARE
  j jsonb;
  row_id bigint;
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

  j := jsonb_set(
    j,
    '{pgSettings,QBANG SANDBOX}',
    '{"amountMode":"STANDARD","settlementCurrency":"KRW","displayCurrency":"THB","fxMode":"AUTO"}'::jsonb,
    true
  );

  UPDATE tb_hq_api_config
  SET url_pay_display_fx_json = j::text
  WHERE id = row_id;
END $$;
