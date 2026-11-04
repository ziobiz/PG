-- V301: 큐뱅(QBANG) 개발(DEV) 샌드박스 실연동 자격 — 온더라인(OTL)
-- POS-ID / agencyCode 는 큐뱅 플랫폼인프라본부 안내(DEV) 기준.
SET client_min_messages TO WARNING;

UPDATE tb_pg_agency
SET
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

-- agency 행이 없으면 생성(idempotent)
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
  'QBANG SANDBOX',
  'QBANG SANDBOX',
  'Y',
  'Y',
  'Y',
  '00000000-2026-1103-0004-006700912345',
  'OTL',
  'https://api-test.qr-road.com/endpoints/pos/command',
  '{"posId":"00000000-2026-1103-0004-006700912345","agencyCode":"OTL","env":"DEV","partner":"온더라인","sandboxThbToKrw":40}',
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
  SELECT 1 FROM tb_pg_agency WHERE UPPER(TRIM(pg_cd)) = 'QBANG SANDBOX'
);

-- DEALMAI DEMO 및 기존 QBANG SANDBOX 바인딩 MID/agencyCode 동기화
UPDATE tb_merchant_pg_binding b
SET
  mid = '00000000-2026-1103-0004-006700912345',
  api_key = 'OTL',
  activation_yn = 'Y',
  operational_yn = 'Y',
  qr_wallet_scope = 'QBANG',
  card_brand_scope = COALESCE(NULLIF(TRIM(b.card_brand_scope), ''), 'NONE')
WHERE UPPER(TRIM(b.pg_cd)) = 'QBANG SANDBOX';

-- DEALMAI DEMO: QR 사용 유지 + QBANG SANDBOX 바인딩 보장
UPDATE tb_merchant_profile mp
SET qr_payment_use_yn = 'Y'
FROM tb_org_unit ou
WHERE ou.id = mp.org_unit_id
  AND ou.code = '6000000067';

INSERT INTO tb_merchant_pg_binding (
  org_unit_id, pg_cd, activation_yn, operational_yn, pay_method,
  mid, api_key, iv_key, installment_yn, sort_order,
  url_pay_pricing_mode, card_brand_scope, currency_scope, qr_wallet_scope, created_at
)
SELECT
  ou.id,
  'QBANG SANDBOX',
  'Y',
  'Y',
  'WEB',
  '00000000-2026-1103-0004-006700912345',
  'OTL',
  '',
  'N',
  10,
  'CHECKOUT_CURRENCY',
  'NONE',
  'ALL',
  'QBANG',
  NOW()
FROM tb_org_unit ou
WHERE ou.code = '6000000067'
  AND NOT EXISTS (
    SELECT 1 FROM tb_merchant_pg_binding b
    WHERE b.org_unit_id = ou.id
      AND UPPER(TRIM(b.pg_cd)) = 'QBANG SANDBOX'
      AND UPPER(TRIM(b.pay_method)) = 'WEB'
  );
