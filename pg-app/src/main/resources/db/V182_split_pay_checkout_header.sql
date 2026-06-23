-- URL 분할결제 결제창 — 로고·안내메세지·다국어 메뉴 (웹결제와 별도 설정)
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_header_logo_mode VARCHAR(16) NOT NULL DEFAULT 'HTML';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_header_logo_url VARCHAR(500);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_header_subtitle_mode VARCHAR(16) NOT NULL DEFAULT 'DEFAULT';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_header_subtitle_text VARCHAR(200);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_lang_menu_use_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
