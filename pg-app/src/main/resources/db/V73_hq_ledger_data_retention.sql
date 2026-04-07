-- 전산설정관리: 데이터 유형별 보관 기간(일) 정책 JSON
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS data_retention_policy_json TEXT;

COMMENT ON COLUMN tb_hq_ledger_sys_settings.data_retention_policy_json IS
    '데이터 보관 일수 정책. JSON 객체: { "PG_NOTIFY_INBOUND": 90, ... }';
