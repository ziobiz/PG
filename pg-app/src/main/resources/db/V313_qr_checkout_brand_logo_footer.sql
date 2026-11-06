-- V313: QR 결제창(/checkout-qr) 로고·브랜드제목(PayPay QR)·푸터 — 본사 기본 + 가맹 FOLLOW_HQ
SET client_min_messages TO WARNING;

ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS qr_payment_header_logo_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS qr_payment_brand_title_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS qr_payment_brand_title_text_default VARCHAR(120),
  ADD COLUMN IF NOT EXISTS qr_payment_brand_title_text_default_i18n TEXT,
  ADD COLUMN IF NOT EXISTS qr_payment_footer_mode_default VARCHAR(16) NOT NULL DEFAULT 'DISABLED',
  ADD COLUMN IF NOT EXISTS qr_payment_footer_text_default VARCHAR(500),
  ADD COLUMN IF NOT EXISTS qr_payment_footer_text_default_i18n TEXT;

COMMENT ON COLUMN tb_hq_api_config.qr_payment_header_logo_mode_default IS
  'QR 결제창 상단 로고 본사기본 ACTIVE=웹결제 로고 노출 DISABLED=숨김';
COMMENT ON COLUMN tb_hq_api_config.qr_payment_brand_title_mode_default IS
  'QR 결제창 로고 아래 브랜드제목(PayPay QR) ACTIVE|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.qr_payment_footer_mode_default IS
  'QR 결제창 하단 푸터 ACTIVE|DISABLED';

UPDATE tb_hq_api_config
SET
  qr_payment_header_logo_mode_default = COALESCE(NULLIF(TRIM(qr_payment_header_logo_mode_default), ''), 'ACTIVE'),
  qr_payment_brand_title_mode_default = COALESCE(NULLIF(TRIM(qr_payment_brand_title_mode_default), ''), 'ACTIVE'),
  qr_payment_brand_title_text_default = COALESCE(NULLIF(TRIM(qr_payment_brand_title_text_default), ''), 'PayPay QR'),
  qr_payment_footer_mode_default = COALESCE(NULLIF(TRIM(qr_payment_footer_mode_default), ''), 'DISABLED'),
  qr_payment_header_subtitle_text_default = COALESCE(
    NULLIF(TRIM(qr_payment_header_subtitle_text_default), ''),
    'PayPay 앱으로 QR을 스캔해 결제해 주세요.'
  )
WHERE TRUE;

ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS qr_payment_header_logo_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS qr_payment_brand_title_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS qr_payment_brand_title_text VARCHAR(120),
  ADD COLUMN IF NOT EXISTS qr_payment_brand_title_text_i18n TEXT,
  ADD COLUMN IF NOT EXISTS qr_payment_footer_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS qr_payment_footer_text VARCHAR(500),
  ADD COLUMN IF NOT EXISTS qr_payment_footer_text_i18n TEXT;

COMMENT ON COLUMN tb_merchant_profile.qr_payment_header_logo_mode IS
  'QR 결제창 로고 FOLLOW_HQ|DISABLED (FOLLOW_HQ=본사기본→웹결제 로고)';
COMMENT ON COLUMN tb_merchant_profile.qr_payment_brand_title_mode IS
  'QR 브랜드제목 FOLLOW_HQ|ACTIVE|DISABLED';
COMMENT ON COLUMN tb_merchant_profile.qr_payment_footer_mode IS
  'QR 푸터 FOLLOW_HQ|ACTIVE|DISABLED';

UPDATE tb_merchant_profile
SET
  qr_payment_header_logo_mode = COALESCE(NULLIF(TRIM(qr_payment_header_logo_mode), ''), 'FOLLOW_HQ'),
  qr_payment_brand_title_mode = COALESCE(NULLIF(TRIM(qr_payment_brand_title_mode), ''), 'FOLLOW_HQ'),
  qr_payment_footer_mode = COALESCE(NULLIF(TRIM(qr_payment_footer_mode), ''), 'FOLLOW_HQ')
WHERE TRUE;
