-- 웹용 QR 열기 화면: 상단 이름·하단 문구 (본사 기본 + 가맹 FOLLOW_HQ|ACTIVE|DISABLED)
ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS web_pay_qr_title_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS web_pay_qr_title_text_default VARCHAR(120) NULL,
  ADD COLUMN IF NOT EXISTS web_pay_qr_footer_mode_default VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS web_pay_qr_footer_text_default VARCHAR(120) NULL;

UPDATE tb_hq_api_config
SET web_pay_qr_footer_text_default = 'Provided by ICOPAY'
WHERE web_pay_qr_footer_text_default IS NULL OR TRIM(web_pay_qr_footer_text_default) = '';

ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS web_pay_qr_title_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS web_pay_qr_title_text VARCHAR(120) NULL,
  ADD COLUMN IF NOT EXISTS web_pay_qr_footer_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ',
  ADD COLUMN IF NOT EXISTS web_pay_qr_footer_text VARCHAR(120) NULL;
