-- 간편결제(/pay-simple) 로고·부제 — C구조 (FOLLOW_WEB / FOLLOW_API / 직접 / 본사따름)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS simple_pay_logo_mode_default VARCHAR(20) NOT NULL DEFAULT 'HTML',
    ADD COLUMN IF NOT EXISTS simple_pay_logo_url_default VARCHAR(500),
    ADD COLUMN IF NOT EXISTS simple_pay_html_title_default VARCHAR(80),
    ADD COLUMN IF NOT EXISTS simple_pay_brand_sub_mode_default VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS simple_pay_brand_sub_text_default VARCHAR(200),
    ADD COLUMN IF NOT EXISTS simple_pay_brand_sub_text_default_i18n TEXT;

COMMENT ON COLUMN tb_hq_api_config.simple_pay_logo_mode_default IS '간편결제 로고 본사기본: FOLLOW_WEB|FOLLOW_API|DEFAULT|HTML|ACTIVE|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.simple_pay_logo_url_default IS '간편결제 로고 ACTIVE 시 본사 URL';
COMMENT ON COLUMN tb_hq_api_config.simple_pay_html_title_default IS '간편결제 HTML 표시명 본사기본';
COMMENT ON COLUMN tb_hq_api_config.simple_pay_brand_sub_mode_default IS '간편결제 부제 본사기본: FOLLOW_WEB|FOLLOW_API|ACTIVE|CUSTOM|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.simple_pay_brand_sub_text_default IS '간편결제 부제 CUSTOM 본사 문구(한)';
COMMENT ON COLUMN tb_hq_api_config.simple_pay_brand_sub_text_default_i18n IS '간편결제 부제 CUSTOM 본사 5국어 JSON';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS simple_pay_logo_mode VARCHAR(20) NOT NULL DEFAULT 'FOLLOW_HQ',
    ADD COLUMN IF NOT EXISTS simple_pay_logo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS simple_pay_html_title VARCHAR(80),
    ADD COLUMN IF NOT EXISTS simple_pay_brand_sub_mode VARCHAR(20) NOT NULL DEFAULT 'FOLLOW_HQ',
    ADD COLUMN IF NOT EXISTS simple_pay_brand_sub_text VARCHAR(200),
    ADD COLUMN IF NOT EXISTS simple_pay_brand_sub_text_i18n TEXT;

COMMENT ON COLUMN tb_merchant_profile.simple_pay_logo_mode IS '간편결제 로고: FOLLOW_HQ|FOLLOW_WEB|FOLLOW_API|DEFAULT|HTML|ACTIVE|DISABLED';
COMMENT ON COLUMN tb_merchant_profile.simple_pay_logo_url IS '간편결제 로고 ACTIVE 시 가맹 URL';
COMMENT ON COLUMN tb_merchant_profile.simple_pay_html_title IS '간편결제 HTML 표시명';
COMMENT ON COLUMN tb_merchant_profile.simple_pay_brand_sub_mode IS '간편결제 부제: FOLLOW_HQ|FOLLOW_WEB|FOLLOW_API|ACTIVE|CUSTOM|DISABLED';
COMMENT ON COLUMN tb_merchant_profile.simple_pay_brand_sub_text IS '간편결제 부제 CUSTOM 가맹 문구(한)';
COMMENT ON COLUMN tb_merchant_profile.simple_pay_brand_sub_text_i18n IS '간편결제 부제 CUSTOM 가맹 5국어 JSON';
