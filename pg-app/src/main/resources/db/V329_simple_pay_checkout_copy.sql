-- 간편결제 QR URL(/pay-simple) 결제 안내 — REDIRECT(RE) 확인창 문구와 분리
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_lead_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_lead_text_default VARCHAR(500),
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_lead_text_default_i18n TEXT,
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_hint_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_hint_text_default VARCHAR(500),
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_hint_text_default_i18n TEXT,
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_auto_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_auto_text_default VARCHAR(500),
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_auto_text_default_i18n TEXT;

COMMENT ON COLUMN tb_hq_api_config.simple_pay_checkout_lead_mode_default IS '간편결제 QR URL 상단 안내 본사기본 ACTIVE|CUSTOM|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.simple_pay_checkout_hint_mode_default IS '간편결제 QR URL 하단(PC QR) 안내 본사기본';
COMMENT ON COLUMN tb_hq_api_config.simple_pay_checkout_auto_mode_default IS '간편결제 QR URL PC 자동완료 안내 본사기본';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_lead_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_lead_text VARCHAR(500),
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_lead_text_i18n TEXT,
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_hint_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_hint_text VARCHAR(500),
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_hint_text_i18n TEXT,
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_auto_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_auto_text VARCHAR(500),
    ADD COLUMN IF NOT EXISTS simple_pay_checkout_auto_text_i18n TEXT;

COMMENT ON COLUMN tb_merchant_profile.simple_pay_checkout_lead_mode IS '간편결제 QR URL 상단 안내 FOLLOW_HQ|ACTIVE|CUSTOM|DISABLED — 간편결제 ON 업체 /pay-simple 전용';
COMMENT ON COLUMN tb_merchant_profile.simple_pay_checkout_hint_mode IS '간편결제 QR URL 하단(PC QR) 안내';
COMMENT ON COLUMN tb_merchant_profile.simple_pay_checkout_auto_mode IS '간편결제 QR URL PC 자동완료 안내';
