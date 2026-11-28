-- 결제대행사 등록 = 근원원가 (설정과 분리). 통화별로 여러 원가 가능.
-- 결제대행사 설정(tb_pg_agency)이 cost_policy_id 로 근원원가를 필수 참조.

ALTER TABLE tb_pg_agency_cost_policy
    ADD COLUMN IF NOT EXISTS cost_nm VARCHAR(100),
    ADD COLUMN IF NOT EXISTS vendor_nm VARCHAR(100),
    ADD COLUMN IF NOT EXISTS display_currency VARCHAR(16),
    ADD COLUMN IF NOT EXISTS vat_rate_pct NUMERIC(5, 2) DEFAULT 0;

UPDATE tb_pg_agency_cost_policy
SET cost_nm = COALESCE(NULLIF(TRIM(cost_nm), ''), pg_cd),
    vendor_nm = COALESCE(NULLIF(TRIM(vendor_nm), ''), pg_cd),
    display_currency = COALESCE(NULLIF(TRIM(display_currency), ''), currency_code, 'KRW'),
    vat_rate_pct = COALESCE(vat_rate_pct, 0)
WHERE TRUE;

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS cost_policy_id BIGINT;

UPDATE tb_pg_agency a
SET cost_policy_id = p.id
FROM tb_pg_agency_cost_policy p
WHERE a.cost_policy_id IS NULL
  AND UPPER(TRIM(a.pg_cd)) = UPPER(TRIM(p.pg_cd));

CREATE INDEX IF NOT EXISTS ix_pg_agency_cost_policy_id ON tb_pg_agency (cost_policy_id);

CREATE TABLE IF NOT EXISTS tb_pg_agency_cost_policy_hist (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    cost_nm VARCHAR(100),
    vendor_nm VARCHAR(100),
    pg_cd VARCHAR(20),
    snapshot_json TEXT NOT NULL,
    changed_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_pg_agency_cost_policy_hist_policy
    ON tb_pg_agency_cost_policy_hist (policy_id, created_at DESC);
