-- 총판(MASTER_DIST)별 가맹 정산주기 제공: 최대 5코드 + 대표 슬롯(0~4)
CREATE TABLE IF NOT EXISTS tb_master_dist_settlement_cycle_config (
    id                 BIGSERIAL PRIMARY KEY,
    org_unit_id        BIGINT       NOT NULL,
    cycle_code_1       VARCHAR(64),
    cycle_code_2       VARCHAR(64),
    cycle_code_3       VARCHAR(64),
    cycle_code_4       VARCHAR(64),
    cycle_code_5       VARCHAR(64),
    default_slot       INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMP WITHOUT TIME ZONE,
    updated_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_md_settle_cycle_org UNIQUE (org_unit_id)
);
CREATE INDEX IF NOT EXISTS idx_md_settle_cycle_org ON tb_master_dist_settlement_cycle_config (org_unit_id);
