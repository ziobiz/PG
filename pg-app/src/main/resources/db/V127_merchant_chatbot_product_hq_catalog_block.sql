-- 본사·총판: 챗봇 공개 카탈로그·고객 판매 단위 차단 (가맹 사용=Y여도 비노출)
ALTER TABLE tb_merchant_chatbot_product
    ADD COLUMN IF NOT EXISTS hq_catalog_block_yn VARCHAR(1) NOT NULL DEFAULT 'N';
