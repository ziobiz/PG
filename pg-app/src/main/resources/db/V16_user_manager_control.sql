-- 사용자관리 권한 기능 토글 (총본사 환경설정)
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS manager_user_control_enabled_yn VARCHAR(1) DEFAULT 'N';

ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS manager_password_reset_enabled_yn VARCHAR(1) DEFAULT 'N';

