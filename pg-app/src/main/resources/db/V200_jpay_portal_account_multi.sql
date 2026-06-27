-- JPAY 포털 계정 — 총판당 복수 등록(JPY/USD PG코드 구분)
ALTER TABLE tb_jpay_portal_account DROP CONSTRAINT IF EXISTS uq_jpay_portal_account_master;

CREATE UNIQUE INDEX IF NOT EXISTS uq_jpay_portal_account_master_pg
    ON tb_jpay_portal_account (master_org_unit_id, pg_cd)
    WHERE pg_cd IS NOT NULL AND TRIM(pg_cd) <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uq_jpay_portal_account_master_user
    ON tb_jpay_portal_account (master_org_unit_id, portal_username);

COMMENT ON TABLE tb_jpay_portal_account IS 'JPAY 가맹 포털 로그인 — 총판(MASTER_DIST)별 복수 계정(JPY/USD PG코드 구분), 통합내역 Export 동기화용';
