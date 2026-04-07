-- 이메일무효 마감 시각은 기존 컬럼(email_void_end_min) 사용(앱에서 더 이상 23:59 고정 덮어쓰기 안 함).
-- 환불: 결제일 익일(태국) 구간 시작 시각(분, 0=00:00)
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS auto_refund_window_start_min INTEGER;

COMMENT ON COLUMN tb_hq_notify_env_config.auto_refund_window_start_min IS '자동환불: 결제일 익일(Asia/Bangkok) 해당 시각(0~1439분)부터 환불 가능 일수. NULL=0시';
