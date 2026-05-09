-- 챗봇 상품: 판매(SALE) / 예약(RESERVATION) 구분
ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS listing_type VARCHAR(16) NOT NULL DEFAULT 'SALE';
