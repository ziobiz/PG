-- URL 공개 결제창 입력방식 — GENERAL | TYPE_A | TYPE_B | TYPE_C
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS url_pay_input_mode VARCHAR(16) NOT NULL DEFAULT 'GENERAL';
