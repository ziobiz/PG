-- REDIRECT 결제창 상단(lead)·하단(hint) 안내 문구 — 본사 기본 + 가맹 우선

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS redirect_checkout_lead_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS redirect_checkout_lead_text_default VARCHAR(500),
    ADD COLUMN IF NOT EXISTS redirect_checkout_lead_text_default_i18n TEXT,
    ADD COLUMN IF NOT EXISTS redirect_checkout_hint_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS redirect_checkout_hint_text_default VARCHAR(500),
    ADD COLUMN IF NOT EXISTS redirect_checkout_hint_text_default_i18n TEXT;

COMMENT ON COLUMN tb_hq_api_config.redirect_checkout_lead_mode_default IS 'RE 상단안내 본사기본 ACTIVE|CUSTOM|DISABLED';
COMMENT ON COLUMN tb_hq_api_config.redirect_checkout_hint_mode_default IS 'RE 하단안내 본사기본 ACTIVE|CUSTOM|DISABLED';

UPDATE tb_hq_api_config
SET redirect_checkout_lead_mode_default = COALESCE(NULLIF(TRIM(redirect_checkout_lead_mode_default), ''), 'ACTIVE'),
    redirect_checkout_hint_mode_default = COALESCE(NULLIF(TRIM(redirect_checkout_hint_mode_default), ''), 'ACTIVE'),
    redirect_checkout_lead_text_default = COALESCE(NULLIF(TRIM(redirect_checkout_lead_text_default), ''),
        '주문·구매자 정보를 확인한 뒤 결제 페이지로 이동합니다.'),
    redirect_checkout_hint_text_default = COALESCE(NULLIF(TRIM(redirect_checkout_hint_text_default), ''),
        '다음 화면에서 카드정보를 입력 후 결제 완료 합니다.')
WHERE TRUE;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS redirect_checkout_lead_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
    ADD COLUMN IF NOT EXISTS redirect_checkout_lead_text VARCHAR(500),
    ADD COLUMN IF NOT EXISTS redirect_checkout_lead_text_i18n TEXT,
    ADD COLUMN IF NOT EXISTS redirect_checkout_hint_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
    ADD COLUMN IF NOT EXISTS redirect_checkout_hint_text VARCHAR(500),
    ADD COLUMN IF NOT EXISTS redirect_checkout_hint_text_i18n TEXT;

COMMENT ON COLUMN tb_merchant_profile.redirect_checkout_lead_mode IS 'RE 상단안내 FOLLOW_HQ|ACTIVE|CUSTOM|DISABLED';
COMMENT ON COLUMN tb_merchant_profile.redirect_checkout_hint_mode IS 'RE 하단안내 FOLLOW_HQ|ACTIVE|CUSTOM|DISABLED';

UPDATE tb_merchant_profile
SET redirect_checkout_lead_mode = 'FOLLOW_HQ'
WHERE redirect_checkout_lead_mode IS NULL OR TRIM(redirect_checkout_lead_mode) = '';

UPDATE tb_merchant_profile
SET redirect_checkout_hint_mode = 'FOLLOW_HQ'
WHERE redirect_checkout_hint_mode IS NULL OR TRIM(redirect_checkout_hint_mode) = '';
