-- V304: QBANG SANDBOX — URL결제설정 페이지에서 일반형+실결제 KRW 예외 저장 형태 유지
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
    '{"amountMode":"STANDARD","settlementCurrency":"KRW"}'::jsonb,
    true
  );

  UPDATE tb_hq_api_config
  SET url_pay_display_fx_json = j::text
  WHERE id = row_id;
END $$;
