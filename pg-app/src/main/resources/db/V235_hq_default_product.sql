-- 본사 결제창 표시: 상품명 사용=활성(직접입력) 시 기본 상품 정보
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_default_product_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS url_pay_default_product_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS url_pay_default_product_amount NUMERIC(18, 2),
    ADD COLUMN IF NOT EXISTS url_pay_default_product_desc VARCHAR(500);
