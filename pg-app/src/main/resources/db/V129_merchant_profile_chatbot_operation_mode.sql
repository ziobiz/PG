-- PostgreSQL 등 영구 DB용. 운영(application.yml ddl-auto: validate) 반영 시 1회 실행.
-- 로컬 H2(dev)는 엔티티 매핑 또는 ddl-auto 로 컬럼 생성 가능.

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_operation_mode VARCHAR(40);

COMMENT ON COLUMN tb_merchant_profile.chatbot_operation_mode IS '챗봇 운영방식: SALE_PREPAID|SALE_POSTPAID|RESERVATION_PREPAID|RESERVATION_POSTPAID|HYBRID_RESERVATION_PREPAID|FACE_TO_FACE_POSTPAID';
