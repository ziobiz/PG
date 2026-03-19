-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)
CREATE TABLE IF NOT EXISTS tb_hq_notify_env_config (
    id BIGSERIAL PRIMARY KEY,
    ingress_token VARCHAR(64) NOT NULL UNIQUE,
    public_base_url VARCHAR(500),
    auto_void_yn VARCHAR(1),
    email_void_yn VARCHAR(1),
    auto_refund_yn VARCHAR(1),
    force_refund_yn VARCHAR(1),
    auto_void_after_hours INTEGER,
    notify_ok_response VARCHAR(500),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_pg_notify_inbound (
    id BIGSERIAL PRIMARY KEY,
    mid VARCHAR(80),
    root_no VARCHAR(40),
    merchant_id VARCHAR(20),
    org_unit_id BIGINT,
    raw_body TEXT,
    content_type VARCHAR(120),
    client_ip VARCHAR(64),
    process_status VARCHAR(20),
    error_message VARCHAR(500),
    created_at TIMESTAMP
);

ALTER TABLE tb_merchant_pg_binding ADD COLUMN IF NOT EXISTS root_no VARCHAR(40);
