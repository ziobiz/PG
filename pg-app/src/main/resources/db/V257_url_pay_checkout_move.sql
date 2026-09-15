-- 웹결제 결제창이동(구 주소 → 신 주소) + 본사 기본 문구
ALTER TABLE tb_merchant_profile
  ADD COLUMN IF NOT EXISTS url_pay_checkout_move_mode VARCHAR(16) NOT NULL DEFAULT 'DISABLED',
  ADD COLUMN IF NOT EXISTS url_pay_checkout_move_target_type VARCHAR(16) NOT NULL DEFAULT 'COMP_CODE',
  ADD COLUMN IF NOT EXISTS url_pay_checkout_move_target VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS url_pay_checkout_move_message VARCHAR(2000) NULL,
  ADD COLUMN IF NOT EXISTS url_pay_checkout_move_message_i18n TEXT NULL;

COMMENT ON COLUMN tb_merchant_profile.url_pay_checkout_move_mode IS 'FOLLOW_HQ|DIRECT|AUTO|DISABLED(기본) — 결제창이동';
COMMENT ON COLUMN tb_merchant_profile.url_pay_checkout_move_target_type IS 'COMP_CODE|URL';
COMMENT ON COLUMN tb_merchant_profile.url_pay_checkout_move_target IS '이동 업체코드 또는 URL';
COMMENT ON COLUMN tb_merchant_profile.url_pay_checkout_move_message IS '직접입력 안내 원문(KO)';
COMMENT ON COLUMN tb_merchant_profile.url_pay_checkout_move_message_i18n IS '직접입력 안내 다국어 JSON — 저장 시 1회 번역';

ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS url_pay_checkout_move_mode_default VARCHAR(16) NOT NULL DEFAULT 'DISABLED',
  ADD COLUMN IF NOT EXISTS url_pay_checkout_move_message_default VARCHAR(2000) NULL,
  ADD COLUMN IF NOT EXISTS url_pay_checkout_move_message_default_i18n TEXT NULL;

COMMENT ON COLUMN tb_hq_api_config.url_pay_checkout_move_mode_default IS 'DIRECT|AUTO|DISABLED(기본) — 가맹 본사설정따름 시';
COMMENT ON COLUMN tb_hq_api_config.url_pay_checkout_move_message_default IS '결제창이동 안내 본사 기본 원문(KO)';
COMMENT ON COLUMN tb_hq_api_config.url_pay_checkout_move_message_default_i18n IS '본사 기본 안내 다국어 JSON — 저장 시 1회 번역';

UPDATE tb_hq_api_config
   SET url_pay_checkout_move_message_default = COALESCE(NULLIF(TRIM(url_pay_checkout_move_message_default), ''),
'고객님, 안녕하세요.
기존에 안내해 드린 결제 링크가 변경되어 번거로움을 드린 점 진심으로 사과드립니다.

원활한 결제 처리를 위해 아래의 변경된 새 주소로 접속하시어 결제를 진행해 주시기를 부탁드립니다.
아래의 확인을 누르면 자동으로 변경된 새 주소로 이동합니다.

불편을 드려 다시 한번 대단히 죄송합니다.')
 WHERE url_pay_checkout_move_message_default IS NULL
    OR TRIM(url_pay_checkout_move_message_default) = '';
