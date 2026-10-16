-- V285: 통합결제창 안내·푸터메시지·추가푸터 (본사 기본 + 가맹 FOLLOW_HQ)
SET client_min_messages TO WARNING;

ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS unified_payment_header_subtitle_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS unified_payment_header_subtitle_text_default VARCHAR(500),
  ADD COLUMN IF NOT EXISTS unified_payment_header_subtitle_text_default_i18n TEXT,
  ADD COLUMN IF NOT EXISTS unified_payment_notice_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS unified_payment_notice_text_default VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS unified_payment_notice_text_default_i18n TEXT,
  ADD COLUMN IF NOT EXISTS unified_payment_extra_footer_mode_default VARCHAR(16) NOT NULL DEFAULT 'DISABLED',
  ADD COLUMN IF NOT EXISTS unified_payment_extra_footer_text_default VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS unified_payment_extra_footer_text_default_i18n TEXT;

COMMENT ON COLUMN tb_hq_api_config.unified_payment_header_subtitle_mode_default IS '통합결제 상단안내 본사기본 ACTIVE|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.unified_payment_notice_mode_default IS '통합결제 노란주의(푸터메시지) 본사기본 ACTIVE|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.unified_payment_extra_footer_mode_default IS '통합결제 추가푸터 본사기본 ACTIVE|DISABLED';

ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS unified_payment_notice_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS unified_payment_notice_text VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS unified_payment_notice_text_i18n TEXT,
  ADD COLUMN IF NOT EXISTS unified_payment_extra_footer_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS unified_payment_extra_footer_text VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS unified_payment_extra_footer_text_i18n TEXT;

-- 기존 통합결제 상단안내는 가맹 기본을 본사설정따름으로 (DEFAULT → FOLLOW_HQ 은 신규만; 기존값 유지)
UPDATE tb_merchant_profile
SET unified_payment_header_subtitle_mode = 'FOLLOW_HQ'
WHERE unified_payment_header_subtitle_mode IS NULL
   OR TRIM(unified_payment_header_subtitle_mode) = ''
   OR UPPER(TRIM(unified_payment_header_subtitle_mode)) = 'DEFAULT';

COMMENT ON COLUMN tb_merchant_profile.unified_payment_notice_mode IS 'FOLLOW_HQ|ACTIVE|DISABLED';
COMMENT ON COLUMN tb_merchant_profile.unified_payment_extra_footer_mode IS 'FOLLOW_HQ|ACTIVE|DISABLED';
