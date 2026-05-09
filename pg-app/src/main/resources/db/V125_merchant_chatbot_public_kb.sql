-- 고객 대면 챗봇 안내(가맹점 설정): 회사명·주소·연락처·소개 등
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_kb_company_nm VARCHAR(200);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_kb_addr VARCHAR(600);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_kb_tel VARCHAR(100);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_kb_email VARCHAR(120);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_kb_contact_nm VARCHAR(100);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_kb_intro TEXT;
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_kb_product_desc TEXT;
