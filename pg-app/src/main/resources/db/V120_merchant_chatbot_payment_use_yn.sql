-- 가맹점 챗봇결제 사용 여부 (관리자 챗봇 메뉴 표시·챗봇결제 URL 안내용)
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_payment_use_yn VARCHAR(1) DEFAULT 'N';

UPDATE tb_merchant_profile
SET chatbot_payment_use_yn = 'N'
WHERE chatbot_payment_use_yn IS NULL;
