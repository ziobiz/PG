-- 결제 후속조치: 무효·이메일무효를 승인일 당일 시각 구간(분)으로 판단
-- auto_void_after_hours / email_void_after_hours 는 더 이상 사용하지 않음(컬럼은 호환용 유지)

ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS auto_void_start_min INTEGER;
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS auto_void_end_min INTEGER;
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS email_void_start_min INTEGER;
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS email_void_end_min INTEGER;

COMMENT ON COLUMN tb_hq_notify_env_config.auto_void_start_min IS '자동무효: 승인일(기준 Zone) 당일 허용 시작 시각(자정부터 분). NULL=0시로 간주';
COMMENT ON COLUMN tb_hq_notify_env_config.auto_void_end_min IS '자동무효: 승인일 당일 마감 시각(분). NULL=23:59까지';
COMMENT ON COLUMN tb_hq_notify_env_config.email_void_start_min IS '이메일무효: 시작 시각(분). NULL이면 auto_void_end_min+1분(자동무효 마감 직후)';
COMMENT ON COLUMN tb_hq_notify_env_config.email_void_end_min IS '이메일무효: 마감 시각(분)';
