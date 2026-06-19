-- URL 공개 결제창 표시 옵션 — 상품명·회사명·다국어 메뉴
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS url_pay_product_name_use_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS url_pay_company_name_show_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS url_pay_lang_menu_use_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
