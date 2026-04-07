-- 결제 후속조치: 기준 Zone, 환불·강제환불 일자(일), 정산 반영 플래그
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS pay_follow_ref_zone VARCHAR(64);
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS auto_refund_after_days INTEGER;
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS force_refund_after_days INTEGER;
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS auto_void_reflect_settlement_yn VARCHAR(1) DEFAULT 'N';
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS email_void_reflect_settlement_yn VARCHAR(1) DEFAULT 'N';
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS auto_refund_reflect_settlement_yn VARCHAR(1) DEFAULT 'N';
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS force_refund_reflect_settlement_yn VARCHAR(1) DEFAULT 'N';

COMMENT ON COLUMN tb_hq_notify_env_config.pay_follow_ref_zone IS '후속조치 경과 판단 기준 IANA Zone. NULL이면 전산 표준시(displayTimezone)와 동일 취급';
COMMENT ON COLUMN tb_hq_notify_env_config.auto_refund_after_days IS '자동환불: 승인일 기준 경과 일수. NULL=미설정';
COMMENT ON COLUMN tb_hq_notify_env_config.force_refund_after_days IS '강제환불: 승인일 기준 경과 일수. NULL=미설정';
COMMENT ON COLUMN tb_hq_notify_env_config.auto_void_reflect_settlement_yn IS '자동무효 정산 반영 Y/N';
COMMENT ON COLUMN tb_hq_notify_env_config.email_void_reflect_settlement_yn IS '이메일무효 정산 반영 Y/N';
COMMENT ON COLUMN tb_hq_notify_env_config.auto_refund_reflect_settlement_yn IS '자동환불 정산 반영 Y/N';
COMMENT ON COLUMN tb_hq_notify_env_config.force_refund_reflect_settlement_yn IS '강제환불 정산 반영 Y/N';
