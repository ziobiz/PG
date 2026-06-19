-- process_status 코드 중 20자 초과(UNKNOWN_NOTIFY_TARGET, INGRESS_ORG_SCOPE_MISMATCH 등) 저장 실패 방지
ALTER TABLE tb_pg_notify_inbound
    ALTER COLUMN process_status TYPE VARCHAR(32);
