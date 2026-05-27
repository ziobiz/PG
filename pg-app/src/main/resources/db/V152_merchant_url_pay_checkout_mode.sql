-- 가맹 URL·챗봇·API 중계 결제 방식: STANDARD(일반 URL) | REPAY(재결제 URL·저장 카드)
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS url_pay_checkout_mode VARCHAR(16) NOT NULL DEFAULT 'STANDARD';
