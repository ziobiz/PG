-- 웹결제 카드입력 활성/비활성 + 비활성 안내문구·다국어(JSON)
ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS url_pay_card_input_mode VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN IF NOT EXISTS url_pay_card_input_disabled_text VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS url_pay_card_input_disabled_text_i18n TEXT NULL;

COMMENT ON COLUMN tb_merchant_profile.url_pay_card_input_mode IS 'ACTIVE=카드입력 표시, DISABLED=카드입력 숨김+안내문구';
COMMENT ON COLUMN tb_merchant_profile.url_pay_card_input_disabled_text IS '카드입력 비활성 시 KO 안내 원문';
COMMENT ON COLUMN tb_merchant_profile.url_pay_card_input_disabled_text_i18n IS '카드입력 비활성 안내 다국어 JSON {KOR,ENG,JPN,CHN,THA} — 저장 시 1회 번역';
