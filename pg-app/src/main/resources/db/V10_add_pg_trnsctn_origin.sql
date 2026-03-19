-- PostgreSQL 등 영구 DB용. H2 dev는 ddl-auto:create-drop 로 엔티티 반영.
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS origin VARCHAR(20);
