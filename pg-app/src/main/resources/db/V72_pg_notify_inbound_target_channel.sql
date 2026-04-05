-- 노티 수신 로그: URL 대상코드·채널(CALLBACK/RESULT) — 본사 노티수령정보 화면용
ALTER TABLE tb_pg_notify_inbound ADD COLUMN IF NOT EXISTS notify_target_code VARCHAR(64);
ALTER TABLE tb_pg_notify_inbound ADD COLUMN IF NOT EXISTS notify_channel_type VARCHAR(20);
