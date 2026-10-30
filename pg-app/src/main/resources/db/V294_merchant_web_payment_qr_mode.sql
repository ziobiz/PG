-- 웹용 QR(URL 결제): AUTO|ACTIVE|DISABLED — 기본 AUTO(URL 전용 가맹만 실효)
ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS web_payment_qr_mode VARCHAR(16) NOT NULL DEFAULT 'AUTO';
