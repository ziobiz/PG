-- 본사설정 리스크설정 — 일괄운영관리 (가맹점사용·URL결제·로그인 제한)

CREATE TABLE IF NOT EXISTS tb_hq_bulk_ops_policy (
    id                          BIGINT PRIMARY KEY,
    policy_type                 VARCHAR(32) NOT NULL,
    mode                        VARCHAR(16) NOT NULL DEFAULT 'NONE',
    pause_snapshot_json         TEXT,
    updated_at                  TIMESTAMP,
    updated_by                  VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_hq_bulk_ops_policy_type ON tb_hq_bulk_ops_policy (policy_type);

INSERT INTO tb_hq_bulk_ops_policy (id, policy_type, mode, updated_at)
SELECT 1, 'ORG_USE', 'NONE', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_hq_bulk_ops_policy WHERE policy_type = 'ORG_USE');

INSERT INTO tb_hq_bulk_ops_policy (id, policy_type, mode, updated_at)
SELECT 2, 'URL_PAY', 'NONE', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_hq_bulk_ops_policy WHERE policy_type = 'URL_PAY');

CREATE TABLE IF NOT EXISTS tb_hq_bulk_login_restriction (
    id                          BIGSERIAL PRIMARY KEY,
    target_org_level            VARCHAR(20),
    target_org_unit_id          BIGINT,
    target_org_code             VARCHAR(50),
    target_org_name             VARCHAR(200),
    mode                        VARCHAR(16) NOT NULL DEFAULT 'FORCE_N',
    pause_snapshot_json         TEXT,
    status                      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at                  TIMESTAMP,
    updated_at                  TIMESTAMP,
    updated_by                  VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_hq_bulk_login_restriction_status ON tb_hq_bulk_login_restriction (status);
