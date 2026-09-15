-- 결제창이동: 본사설정 따름 제거(기존 FOLLOW_HQ → 비활성). 경고메세지 직접입력 다국어 JSON.
UPDATE tb_merchant_profile
   SET url_pay_checkout_move_mode = 'DISABLED'
 WHERE UPPER(TRIM(COALESCE(url_pay_checkout_move_mode, ''))) IN ('FOLLOW_HQ', 'HQ');

COMMENT ON COLUMN tb_merchant_profile.url_pay_checkout_move_mode IS 'DISABLED(기본)|DIRECT|AUTO — URL 결제 전용 결제창이동(가맹 API·Woo 미적용)';

ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS web_payment_header_subtitle_text_i18n TEXT NULL;

COMMENT ON COLUMN tb_merchant_profile.web_payment_header_subtitle_text_i18n IS '경고메세지 직접입력 다국어 JSON — 저장 시 1회 번역, 표시 시 재번역 금지';
