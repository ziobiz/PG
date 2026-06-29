-- 모바일·3DS 결제창 모드 — 본사 기본값 + 가맹 오버라이드 (EMBED | MOBILE_REDIRECT | ALWAYS_REDIRECT)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS mobile_checkout_mode_default VARCHAR(32) NOT NULL DEFAULT 'EMBED';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS mobile_checkout_mode VARCHAR(32) NULL;

COMMENT ON COLUMN tb_hq_api_config.mobile_checkout_mode_default IS '모바일 결제창 기본: EMBED, MOBILE_REDIRECT, ALWAYS_REDIRECT';
COMMENT ON COLUMN tb_merchant_profile.mobile_checkout_mode IS '가맹 모바일 결제창 오버라이드. NULL=본사 기본';
