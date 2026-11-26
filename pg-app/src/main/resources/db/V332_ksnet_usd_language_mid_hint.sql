-- V332: KSNET 해외 USD — 결제창 language=ENG 힌트·USD MID 메타(시크릿 미포함)
-- 실제 storeid/apiKey/거래대사 비밀번호는 본사 UI 또는 운영 전용 SQL로만 주입.
SET client_min_messages TO WARNING;

UPDATE tb_pg_agency
SET
  credentials_extra_json = jsonb_strip_nulls(
    COALESCE(NULLIF(BTRIM(credentials_extra_json), ''), '{}')::jsonb
    || jsonb_build_object(
      'showCard', COALESCE(NULLIF(BTRIM(COALESCE(credentials_extra_json::jsonb->>'showCard','')), ''), 'C(25)'),
      'settlementCurrencyHint', 'USD',
      'currencyHint', 'USD',
      'amountModeHint', 'STANDARD',
      'defaultKspayLanguage', 'ENG',
      'storeNameEn', 'ONTHELINE CO., LTD.'
    )
  )::text,
  endpoint_api = CASE
    WHEN UPPER(TRIM(COALESCE(sandbox_yn, 'Y'))) = 'N' THEN COALESCE(NULLIF(BTRIM(endpoint_api), ''), 'https://kspay.ksnet.to')
    ELSE COALESCE(NULLIF(BTRIM(endpoint_api), ''), 'https://pgdev.ksnet.co.kr')
  END
WHERE UPPER(TRIM(pg_cd)) IN ('KSNET', 'KSNET USDP');
