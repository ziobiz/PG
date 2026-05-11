-- 챗봇 결제 상단 "프로모션" 영역에만 노출할 상품 표시 (Y일 때만 상단 노출)
ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS promotion_shelf_yn VARCHAR(1) NOT NULL DEFAULT 'N';
