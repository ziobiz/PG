-- JPAY 포털 통합내역 — 총판(MASTER_DIST)별 merchant.j-pay.net 계정 (1 총판 = 1 포털 ID)
CREATE TABLE IF NOT EXISTS tb_jpay_portal_account (
    id                  BIGSERIAL PRIMARY KEY,
    master_org_unit_id  BIGINT       NOT NULL,
    master_comp_code    VARCHAR(64)  NOT NULL,
    label               VARCHAR(200),
    pg_cd               VARCHAR(32),
    portal_username     VARCHAR(255) NOT NULL,
    portal_password     VARCHAR(512) NOT NULL,
    use_yn              VARCHAR(1)   NOT NULL DEFAULT 'Y',
    sort_order          INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_jpay_portal_account_master UNIQUE (master_org_unit_id)
);

CREATE INDEX IF NOT EXISTS ix_jpay_portal_account_use ON tb_jpay_portal_account (use_yn, sort_order, id);

COMMENT ON TABLE tb_jpay_portal_account IS 'JPAY 가맹 포털 로그인 — 총판(MASTER_DIST) 1:1, 통합내역 Export 자동 동기화용';
COMMENT ON COLUMN tb_jpay_portal_account.master_org_unit_id IS '총판 tb_org_unit.id (org_level=MASTER_DIST)';
COMMENT ON COLUMN tb_jpay_portal_account.pg_cd IS '연동 JPAY PG코드(예: JPAY, JPAY_USD) — 표시·검증용';
