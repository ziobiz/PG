-- 본사 URL·API 입력방식 기본값 (가맹 url_pay_input_mode=FOLLOW_HQ 일 때 채널별 적용)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_input_mode_default VARCHAR(16) NOT NULL DEFAULT 'GENERAL';

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS api_url_pay_input_mode_default VARCHAR(16) NOT NULL DEFAULT 'TYPE_BA';

COMMENT ON COLUMN tb_hq_api_config.url_pay_input_mode_default IS '공개 URL·챗봇·분할 URL 결제창 입력방식 본사 기본(GENERAL|TYPE_*). 가맹 FOLLOW_HQ 시 URL 채널';
COMMENT ON COLUMN tb_hq_api_config.api_url_pay_input_mode_default IS 'API 인라인(entry=merchant_api) 결제창 입력방식 본사 기본. 가맹 FOLLOW_HQ 시 API 채널';
