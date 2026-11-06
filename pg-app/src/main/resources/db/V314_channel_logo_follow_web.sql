-- V314: 크립토·QR 로고 — 본사설정 따름(FOLLOW_HQ) vs 웹결제 로고 따름(FOLLOW_WEB) 분리
-- + 채널별 가맹점 로고 URL·HTML 표시명. 웹결제 카드의 크립토 로고 UI는 크립토카드로 이동.
SET client_min_messages TO WARNING;

-- 기존 크립토 FOLLOW_HQ 는 UI상 「웹결제 로고 따름」이었음 → FOLLOW_WEB 으로 이관
UPDATE tb_merchant_profile
SET crypto_payment_header_logo_mode = 'FOLLOW_WEB'
WHERE UPPER(TRIM(COALESCE(crypto_payment_header_logo_mode, ''))) IN (
  'FOLLOW_HQ', 'HQ', 'Y', 'ON', 'FOLLOW_WEB', ''
);

ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS crypto_payment_header_logo_url VARCHAR(500),
  ADD COLUMN IF NOT EXISTS crypto_payment_header_html_title VARCHAR(80),
  ADD COLUMN IF NOT EXISTS qr_payment_header_logo_url VARCHAR(500),
  ADD COLUMN IF NOT EXISTS qr_payment_header_html_title VARCHAR(80);

COMMENT ON COLUMN tb_merchant_profile.crypto_payment_header_logo_mode IS
  '크립토 로고 FOLLOW_HQ|FOLLOW_WEB|DEFAULT|HTML|ACTIVE|DISABLED';
COMMENT ON COLUMN tb_merchant_profile.qr_payment_header_logo_mode IS
  'QR 로고 FOLLOW_HQ|FOLLOW_WEB|DEFAULT|HTML|ACTIVE|DISABLED';
COMMENT ON COLUMN tb_merchant_profile.crypto_payment_header_logo_url IS
  '크립토 로고 ACTIVE 시 가맹점 로고 URL';
COMMENT ON COLUMN tb_merchant_profile.qr_payment_header_logo_url IS
  'QR 로고 ACTIVE 시 가맹점 로고 URL';

ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS crypto_payment_header_logo_mode_default VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_WEB';

-- 구 ACTIVE(=웹결제 로고 노출) → FOLLOW_WEB
UPDATE tb_hq_api_config
SET qr_payment_header_logo_mode_default = 'FOLLOW_WEB'
WHERE UPPER(TRIM(COALESCE(qr_payment_header_logo_mode_default, ''))) IN ('ACTIVE', '');

UPDATE tb_hq_api_config
SET crypto_payment_header_logo_mode_default = COALESCE(
  NULLIF(TRIM(crypto_payment_header_logo_mode_default), ''),
  'FOLLOW_WEB'
)
WHERE TRUE;

COMMENT ON COLUMN tb_hq_api_config.crypto_payment_header_logo_mode_default IS
  '크립토 로고 본사기본 FOLLOW_WEB|DEFAULT|HTML|ACTIVE|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.qr_payment_header_logo_mode_default IS
  'QR 로고 본사기본 FOLLOW_WEB|DEFAULT|HTML|ACTIVE|DISABLED';
