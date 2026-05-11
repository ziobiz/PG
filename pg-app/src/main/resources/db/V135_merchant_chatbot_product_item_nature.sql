-- 챗봇 상품(항목) 성격: 공산품/음식/동물/사람(서비스) 등 — LLM 응대 톤·호칭 보정용

ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS item_nature VARCHAR(24) NOT NULL DEFAULT 'GOODS';

COMMENT ON COLUMN tb_merchant_chatbot_product.item_nature IS
    '항목 성격: GOODS/FOOD/ANIMAL/SERVICE/SERVICE_PERSON(사람) 등. 사람 서비스는 물건처럼 표현하지 않도록 챗봇 응대에 사용';

