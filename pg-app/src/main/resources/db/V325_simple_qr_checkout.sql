-- 간편결제(QR URL) — REDIRECT 확장. 기본 미사용(기존 동작 유지).
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS simple_qr_checkout_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS api_broker_simple_qr_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_merchant_profile.simple_qr_checkout_use_yn IS '간편결제(QR URL) prepare 허용 Y/N — REDIRECT 채널 확장';
COMMENT ON COLUMN tb_hq_api_config.api_broker_simple_qr_enabled_yn IS '본사 간편결제(QR URL) API 제공 Y/N';
