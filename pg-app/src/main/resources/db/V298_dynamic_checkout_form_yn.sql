-- 본사 결제라우팅: 다이나믹 결제방식 (Y=모바일·태블릿 다이나믹 / N=전 기기 일반). PC는 활성 시에도 일반.
ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS dynamic_checkout_form_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

UPDATE tb_hq_api_config
SET dynamic_checkout_form_yn = 'Y'
WHERE dynamic_checkout_form_yn IS NULL OR TRIM(dynamic_checkout_form_yn) = '';

COMMENT ON COLUMN tb_hq_api_config.dynamic_checkout_form_yn IS
  '다이나믹 결제방식 Y=활성(모바일·태블릿 다이나믹, PC 일반) N=전 기기 일반. ICOPAY 카드 입력창만.';
