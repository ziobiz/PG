-- 챗봇 상품·예약 다양화: 카탈로그 정책(산하 허용 유형·이미지 장수 상한), 상품 이미지 4장, 예약금/전액, 주문 스냅샷

-- 조직별: 산하 가맹이 쓸 수 있는 listing 유형 교집합 마스크(비우면 해당 단계에서 추가 제한 없음)
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_catalog_listing_grant VARCHAR(160) NULL;

-- 가맹(MERCHANT 행): 실제 활성화할 상품 유형(비우면 상위 교집합 전부 허용)
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_catalog_listing_enabled VARCHAR(160) NULL;

-- 조직별: 산하 가맹 상품 이미지 최대 장수(1~4). 체인에서 정의된 값 중 최소가 실효.
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_max_product_images_grant INTEGER NULL;

COMMENT ON COLUMN tb_merchant_profile.chatbot_catalog_listing_grant IS '쉼표: SALE,RESERVATION_TIME,RESERVATION_PLACE — 산하 가맹 허용 유형 교집합';
COMMENT ON COLUMN tb_merchant_profile.chatbot_catalog_listing_enabled IS '가맹: 사용할 유형. 비우면 상위 교집합 허용';
COMMENT ON COLUMN tb_merchant_profile.chatbot_max_product_images_grant IS '1~4. 산하 실효=체인 최소값, 미설정 단계 무시 기본 1';

ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS image_url_2 VARCHAR(512) NULL;

ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS image_url_3 VARCHAR(512) NULL;

ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS image_url_4 VARCHAR(512) NULL;

ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS reservation_collect_mode VARCHAR(16) NOT NULL DEFAULT 'FULL';

ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS deposit_amount NUMERIC(18, 4) NULL;

COMMENT ON COLUMN tb_merchant_chatbot_product.reservation_collect_mode IS 'FULL=당일 결제 전액, DEPOSIT=예약금(부분)';
COMMENT ON COLUMN tb_merchant_chatbot_product.deposit_amount IS 'DEPOSIT 시 이번 결제(예약금) 금액';

UPDATE tb_merchant_chatbot_product SET reservation_collect_mode = 'FULL'
    WHERE reservation_collect_mode IS NULL OR trim(reservation_collect_mode) = '';

UPDATE tb_merchant_chatbot_product SET listing_type = 'RESERVATION_TIME'
    WHERE upper(trim(listing_type)) = 'RESERVATION';

ALTER TABLE tb_merchant_chatbot_order
    ADD COLUMN IF NOT EXISTS reservation_collect_snapshot VARCHAR(16) NOT NULL DEFAULT 'FULL';

ALTER TABLE tb_merchant_chatbot_order
    ADD COLUMN IF NOT EXISTS product_line_total_amount NUMERIC(18, 4) NULL;

ALTER TABLE tb_merchant_chatbot_order
    ADD COLUMN IF NOT EXISTS balance_due_amount NUMERIC(18, 4) NULL;

COMMENT ON COLUMN tb_merchant_chatbot_order.product_line_total_amount IS 'DEPOSIT 시 상품 표시 전액(정책가)';
COMMENT ON COLUMN tb_merchant_chatbot_order.balance_due_amount IS '결제 후 잔액(현장 등) 안내용';

UPDATE tb_merchant_chatbot_order SET listing_type_snapshot = 'RESERVATION_TIME'
    WHERE upper(trim(listing_type_snapshot)) = 'RESERVATION';
