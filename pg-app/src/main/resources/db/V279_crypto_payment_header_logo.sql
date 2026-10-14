-- V279: 크립토 결제창 상단 로고 — A형 (웹결제 로고 따름 / 미활성)
-- 기본 FOLLOW_HQ = 웹결제(URL) 로고설정과 동일 표시. DISABLED = 크립토·수단선택 상단 로고 미노출.

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS crypto_payment_header_logo_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.crypto_payment_header_logo_mode IS
    '크립토 결제창 상단 로고: FOLLOW_HQ=웹결제 로고 따름 | DISABLED=미활성(미노출)';
