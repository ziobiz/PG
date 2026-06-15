-- 가맹 웹결제(URL·JPAY) 상단 로고 — DEFAULT(총판)·DISABLED·ACTIVE(가맹 업로드)
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS web_payment_header_logo_mode VARCHAR(16) NOT NULL DEFAULT 'DEFAULT';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS web_payment_header_logo_url VARCHAR(500);

COMMENT ON COLUMN tb_merchant_profile.web_payment_header_logo_mode IS '웹결제 상단 로고: DEFAULT|DISABLED|ACTIVE';
COMMENT ON COLUMN tb_merchant_profile.web_payment_header_logo_url IS '웹결제 상단 로고 URL(ACTIVE 시 가맹 업로드)';
