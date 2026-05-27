-- 챗봇 결제 URL 방식: 공개 URL·API 중계(url_pay_checkout_mode)와 별도 선택
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_url_pay_checkout_mode VARCHAR(16) NOT NULL DEFAULT 'STANDARD';
UPDATE tb_merchant_profile SET chatbot_url_pay_checkout_mode = url_pay_checkout_mode;
