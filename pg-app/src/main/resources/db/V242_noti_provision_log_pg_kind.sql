-- 노티생성 이력: JPAY / ElementPay 구분
ALTER TABLE tb_noti_provision_log
    ADD COLUMN IF NOT EXISTS pg_kind VARCHAR(16) NOT NULL DEFAULT 'jpay';
