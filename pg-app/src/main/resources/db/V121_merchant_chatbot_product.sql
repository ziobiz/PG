-- 가맹점 챗봇 상품 (공개 카탈로그·URL 결제 연동용)
CREATE TABLE IF NOT EXISTS tb_merchant_chatbot_product (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NOT NULL REFERENCES tb_org_unit (id) ON DELETE CASCADE,
    product_code    VARCHAR(64),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    amount          NUMERIC(18, 4) NOT NULL DEFAULT 0,
    currency_code   VARCHAR(10) NOT NULL DEFAULT 'KRW',
    image_url       VARCHAR(512),
    sort_order      INT NOT NULL DEFAULT 0,
    use_yn          VARCHAR(1) NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_mcp_org_sort ON tb_merchant_chatbot_product (org_unit_id, sort_order, id);
