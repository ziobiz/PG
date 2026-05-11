-- 챗봇 주문(고객 정보·예약 슬롯) + 기본설정 예약 슬롯 + 상품별 슬롯 오버라이드

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_reservation_slot_minutes INTEGER;
UPDATE tb_merchant_profile SET chatbot_reservation_slot_minutes = 60 WHERE chatbot_reservation_slot_minutes IS NULL;
ALTER TABLE tb_merchant_profile ALTER COLUMN chatbot_reservation_slot_minutes SET DEFAULT 60;
ALTER TABLE tb_merchant_profile ALTER COLUMN chatbot_reservation_slot_minutes SET NOT NULL;

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_reservation_zone_id VARCHAR(64);
UPDATE tb_merchant_profile SET chatbot_reservation_zone_id = 'Asia/Seoul'
  WHERE chatbot_reservation_zone_id IS NULL OR trim(chatbot_reservation_zone_id) = '';
ALTER TABLE tb_merchant_profile ALTER COLUMN chatbot_reservation_zone_id SET DEFAULT 'Asia/Seoul';
ALTER TABLE tb_merchant_profile ALTER COLUMN chatbot_reservation_zone_id SET NOT NULL;

ALTER TABLE tb_merchant_chatbot_product ADD COLUMN IF NOT EXISTS reservation_slot_minutes INTEGER NULL;

CREATE TABLE IF NOT EXISTS tb_merchant_chatbot_order (
    id                      BIGSERIAL PRIMARY KEY,
    org_unit_id             BIGINT NOT NULL,
    product_id              BIGINT NULL,
    product_title           VARCHAR(200) NOT NULL,
    amount                  NUMERIC(18, 4) NOT NULL,
    currency_code           VARCHAR(10) NOT NULL DEFAULT 'KRW',
    listing_type_snapshot   VARCHAR(16) NOT NULL DEFAULT 'SALE',
    orderer_name            VARCHAR(100),
    orderer_email           VARCHAR(120),
    orderer_phone           VARCHAR(50),
    orderer_addr            VARCHAR(600),
    reservation_start       TIMESTAMPTZ NULL,
    reservation_end         TIMESTAMPTZ NULL,
    status                  VARCHAR(24) NOT NULL DEFAULT 'PENDING_PAYMENT',
    checkout_order_no       VARCHAR(20) NOT NULL,
    pg_trn_id               VARCHAR(20) NULL,
    order_memo              TEXT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NULL,
    CONSTRAINT uq_merchant_chatbot_order_checkout UNIQUE (checkout_order_no)
);

CREATE INDEX IF NOT EXISTS ix_mcb_order_org_created ON tb_merchant_chatbot_order (org_unit_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_mcb_order_org_status ON tb_merchant_chatbot_order (org_unit_id, status);

COMMENT ON TABLE tb_merchant_chatbot_order IS '챗봇 고객 주문(결제 전 접수·결제 후 확정). checkout_order_no = 공개 결제 폼 OrderNo';
COMMENT ON COLUMN tb_merchant_chatbot_order.checkout_order_no IS 'pay.html/ChillPay OrderNo 와 동일(최대 20자)';
