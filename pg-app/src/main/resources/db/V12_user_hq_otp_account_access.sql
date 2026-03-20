-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)

ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS otp_required_yn VARCHAR(1) DEFAULT 'Y';

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS org_unit_code VARCHAR(32);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS permission_group_nm VARCHAR(100);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS otp_registered_yn VARCHAR(1) DEFAULT 'N';

CREATE TABLE IF NOT EXISTS tb_user_comp_access (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    comp_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_user_comp_access UNIQUE (username, comp_code)
);
