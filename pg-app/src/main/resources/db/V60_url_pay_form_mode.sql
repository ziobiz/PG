-- URL 공개 결제 페이지: 입력 폼 구성 (FULL=전체 필드, SIMPLE=간편)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_form_mode VARCHAR(20) DEFAULT 'FULL';

COMMENT ON COLUMN tb_hq_api_config.url_pay_form_mode IS 'URL 결제 폼: FULL 전체입력, SIMPLE 간편입력';
