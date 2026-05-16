-- Hibernate ddl-auto=validate: String @Column(length=1) → JDBC VARCHAR.
-- PostgreSQL CHAR(1)는 bpchar로 매핑되어 스키마 검증 실패할 수 있음 → VARCHAR(1)로 통일.
-- 이미 CHAR로 생성된 DB만 보정(신규는 V141/V142가 VARCHAR로 생성).
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.table_name = 'tb_org_tablet_menu'
      AND c.column_name = 'use_yn'
      AND c.udt_name = 'bpchar'
  ) THEN
    ALTER TABLE tb_org_tablet_menu
      ALTER COLUMN use_yn TYPE VARCHAR(1)
      USING (substring(trim(both from use_yn::text), 1, 1));
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.table_name = 'tb_org_unit'
      AND c.column_name = 'tablet_feature_use_yn'
      AND c.udt_name = 'bpchar'
  ) THEN
    ALTER TABLE tb_org_unit
      ALTER COLUMN tablet_feature_use_yn TYPE VARCHAR(1)
      USING (substring(trim(both from tablet_feature_use_yn::text), 1, 1));
  END IF;
END $$;
