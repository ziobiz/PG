-- 노티생성 전산 대상 매핑: THB (ElementPay 등)
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS noti_provision_internal_target_thb VARCHAR(120);

COMMENT ON COLUMN tb_hq_notify_env_config.noti_provision_internal_target_thb IS
    'THB 가맹 노티생성 시 자동 제안할 NOTI internal-target ID (ElementPay 등)';
