-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS user_type VARCHAR(20);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS assistant_role_type VARCHAR(20);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS parent_username VARCHAR(50);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS menu_policy_code VARCHAR(50);

ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS otp_policy_mode VARCHAR(20) DEFAULT 'NOTI';
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS password_policy_mode VARCHAR(20) DEFAULT 'NOTI';
ALTER TABLE tb_hq_notify_env_config ADD COLUMN IF NOT EXISTS forgot_password_enabled_yn VARCHAR(1) DEFAULT 'N';
