-- API URL 인라인 중계 결제 방식: 공개 URL(url_pay_checkout_mode)과 별도 선택
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS api_url_pay_checkout_mode VARCHAR(16) NOT NULL DEFAULT 'STANDARD';
UPDATE tb_merchant_profile SET api_url_pay_checkout_mode = url_pay_checkout_mode;
