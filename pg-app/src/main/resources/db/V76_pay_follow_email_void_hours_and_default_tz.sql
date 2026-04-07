-- 결제 후속조치: 이메일무효 경과(시간) + 전산 기본 표준시(태국)
-- NOTI 종합거래 무효·이메일무효 배치 기준과 동일 개념(상단 시간·동기화 설정의 ZoneId 사용)

ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS email_void_after_hours INTEGER;

COMMENT ON COLUMN tb_hq_notify_env_config.email_void_after_hours IS '이메일무효: 승인 후 경과 기준(시간). NULL=미설정';

UPDATE tb_hq_ledger_sys_settings
SET display_timezone = 'Asia/Bangkok'
WHERE id = 1
  AND (display_timezone IS NULL OR BTRIM(display_timezone) = '');
