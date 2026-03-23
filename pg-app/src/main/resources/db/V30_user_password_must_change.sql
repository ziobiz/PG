-- PostgreSQL 등 영구 DB용 (H2 dev: JPA ddl-auto 로 엔티티 반영)

ALTER TABLE tb_user ADD COLUMN IF NOT EXISTS password_must_change_yn VARCHAR(1) NOT NULL DEFAULT 'N';
