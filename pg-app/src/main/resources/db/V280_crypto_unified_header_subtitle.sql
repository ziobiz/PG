-- V280: 크립토·통합결제 상단 안내문구 (카드 경고메세지와 별도)
-- DEFAULT=채널 기본 다국어 · ACTIVE=직접입력(+i18n) · DISABLED=미노출

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS crypto_payment_header_subtitle_mode VARCHAR(16) NOT NULL DEFAULT 'DEFAULT';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS crypto_payment_header_subtitle_text VARCHAR(200);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS crypto_payment_header_subtitle_text_i18n TEXT;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS unified_payment_header_subtitle_mode VARCHAR(16) NOT NULL DEFAULT 'DEFAULT';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS unified_payment_header_subtitle_text VARCHAR(200);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS unified_payment_header_subtitle_text_i18n TEXT;

COMMENT ON COLUMN tb_merchant_profile.crypto_payment_header_subtitle_mode IS
    '크립토 결제창 로고 아래 안내: DEFAULT|ACTIVE|DISABLED';
COMMENT ON COLUMN tb_merchant_profile.unified_payment_header_subtitle_mode IS
    '통합결제(수단선택) 로고 아래 안내: DEFAULT|ACTIVE|DISABLED';
