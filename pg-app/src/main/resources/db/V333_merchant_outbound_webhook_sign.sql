-- V333: 가맹 웹훅(서버 통보) HMAC 서명 — 본사 기본 OFF, 가맹 FOLLOW_HQ|Y|N (가맹 우선)
SET client_min_messages TO WARNING;

ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS merchant_outbound_webhook_sign_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS outbound_webhook_sign_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS outbound_webhook_sign_secret VARCHAR(128);

COMMENT ON COLUMN tb_hq_api_config.merchant_outbound_webhook_sign_yn IS
  '가맹 서버통보(웹훅) HMAC 서명 본사 기본 Y|N. 기본 N';
COMMENT ON COLUMN tb_merchant_profile.outbound_webhook_sign_yn IS
  '가맹 서버통보 HMAC FOLLOW_HQ|Y|N. 가맹 명시값이 본사보다 우선';
COMMENT ON COLUMN tb_merchant_profile.outbound_webhook_sign_secret IS
  'X-Icopay-Signature HMAC-SHA256 공유 시크릿(평문 저장·가맹에만 전달)';

UPDATE tb_hq_api_config
SET merchant_outbound_webhook_sign_yn = 'N'
WHERE merchant_outbound_webhook_sign_yn IS NULL
   OR BTRIM(merchant_outbound_webhook_sign_yn) = '';

-- 요청 가맹만 활성(시크릿은 앱에서 발급·재발급)
UPDATE tb_merchant_profile mp
SET outbound_webhook_sign_yn = 'Y'
FROM tb_org_unit ou
WHERE ou.id = mp.org_unit_id
  AND ou.code = '6000000057';
