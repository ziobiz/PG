-- 총판 노티 대상: CALLBACK / RESULT 구분 (PostgreSQL 수동 적용용; 로컬 H2는 JPA ddl-auto로 컬럼 반영)
ALTER TABLE tb_hq_notify_target ADD COLUMN IF NOT EXISTS channel_type VARCHAR(16);
UPDATE tb_hq_notify_target SET channel_type = 'CALLBACK' WHERE channel_type IS NULL OR TRIM(channel_type) = '';
ALTER TABLE tb_hq_notify_target ALTER COLUMN channel_type SET DEFAULT 'CALLBACK';
ALTER TABLE tb_hq_notify_target ALTER COLUMN channel_type SET NOT NULL;
