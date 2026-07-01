-- NOTI Provision API (JPAY 가맹 자동등록) — 본사설정 연동
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS noti_provision_enabled_yn VARCHAR(1) DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS noti_provision_base_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS noti_provision_api_key VARCHAR(512),
    ADD COLUMN IF NOT EXISTS noti_provision_default_internal_target_id VARCHAR(120);
