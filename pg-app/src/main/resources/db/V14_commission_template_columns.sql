-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS policy_name VARCHAR(100);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS deploy_yn VARCHAR(1) DEFAULT 'N';
