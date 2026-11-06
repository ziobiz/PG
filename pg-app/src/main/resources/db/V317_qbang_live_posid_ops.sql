-- V317: 큐뱅 실운영 posId (큐뱅 전달) — api.qr-road.com · sandbox=N
-- posId: 00000000-2026-1103-0004-300781505930
-- agencyCode=OTL 유지. 가맹 binding mid 는 표시용(API posId 는 agency만 사용).
SET client_min_messages TO WARNING;

UPDATE tb_pg_agency
SET
  pg_nm = 'QBANG',
  merchant_mid = '00000000-2026-1103-0004-300781505930',
  api_key = 'OTL',
  sandbox_yn = 'N',
  use_yn = 'Y',
  operational_yn = 'Y',
  integ_url_pay_yn = 'Y',
  integ_api_yn = 'Y',
  integ_noti_yn = COALESCE(NULLIF(TRIM(integ_noti_yn), ''), 'Y'),
  endpoint_api = 'https://api.qr-road.com/endpoints/pos/command',
  credentials_extra_json = '{"posId":"00000000-2026-1103-0004-300781505930","agencyCode":"OTL","env":"LIVE","partner":"온더라인"}'
WHERE UPPER(TRIM(pg_cd)) = 'QBANG';

-- 운영 가맹 바인딩 mid 표시 동기화(API posId 덮어쓰기 아님)
UPDATE tb_merchant_pg_binding b
SET
  mid = '00000000-2026-1103-0004-300781505930',
  api_key = COALESCE(NULLIF(TRIM(b.api_key), ''), 'OTL'),
  activation_yn = 'Y',
  operational_yn = 'Y',
  qr_wallet_scope = COALESCE(NULLIF(TRIM(b.qr_wallet_scope), ''), 'QBANG')
WHERE UPPER(TRIM(b.pg_cd)) = 'QBANG';

-- SANDBOX(DEV)는 분리 유지 — 실운영 키로 덮지 않음
UPDATE tb_pg_agency
SET
  sandbox_yn = 'Y',
  endpoint_api = 'https://api-test.qr-road.com/endpoints/pos/command'
WHERE UPPER(TRIM(pg_cd)) = 'QBANG SANDBOX'
  AND (endpoint_api IS NULL OR endpoint_api NOT ILIKE '%api-test.qr-road.com%');
