-- 사용자관리: 연락처, 계정상태(사용/미사용/영구정지), 미사용 전환 사유
-- PostgreSQL 등 영구 DB용 (H2 dev: JPA ddl-auto 로 엔티티 반영 가능)

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS mobile VARCHAR(64);
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS user_status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS inactive_reason VARCHAR(500);

UPDATE tb_user SET user_status = CASE WHEN enabled = true THEN 'ACTIVE' ELSE 'INACTIVE' END
WHERE user_status IS NULL OR user_status = '';
