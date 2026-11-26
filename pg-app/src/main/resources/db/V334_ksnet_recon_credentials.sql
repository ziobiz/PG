-- V334: KSNET 대사(KSTA) 상점ID · WebFEP 거래조회 메타
-- reconPassword / md5_secret_key 실값은 Git에 넣지 않음 — 본사 결제대행사(KSNET) UI에서 저장.
SET client_min_messages TO WARNING;

UPDATE tb_pg_agency
SET
  merchant_mid = COALESCE(NULLIF(BTRIM(merchant_mid), ''), '2001107183'),
  credentials_extra_json = jsonb_strip_nulls(
    COALESCE(NULLIF(BTRIM(credentials_extra_json), ''), '{}')::jsonb
    || jsonb_build_object(
      'reconStoreId', '2001107183',
      'showCard', COALESCE(NULLIF(BTRIM(COALESCE(credentials_extra_json::jsonb->>'showCard', '')), ''), 'C(25)'),
      'settlementCurrencyHint', COALESCE(NULLIF(BTRIM(COALESCE(credentials_extra_json::jsonb->>'settlementCurrencyHint', '')), ''), 'USD'),
      'currencyHint', COALESCE(NULLIF(BTRIM(COALESCE(credentials_extra_json::jsonb->>'currencyHint', '')), ''), 'USD')
    )
  )::text,
  sandbox_yn = CASE WHEN UPPER(TRIM(COALESCE(sandbox_yn, 'Y'))) = 'N' THEN 'N' ELSE sandbox_yn END
WHERE UPPER(TRIM(pg_cd)) IN ('KSNET', 'KSNET USDP');
