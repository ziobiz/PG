-- 가맹 정산주기 예약(pending) + 변경 이력(감사)
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pending_calc_cycle VARCHAR(64);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS pending_calc_cycle_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS tb_settlement_calc_cycle_audit (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NOT NULL,
    merchant_code   VARCHAR(64) NOT NULL,
    from_cycle      VARCHAR(64),
    to_cycle        VARCHAR(64) NOT NULL,
    transition_mode VARCHAR(32) NOT NULL,
    actor_username  VARCHAR(128),
    remark          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_scca_org_created ON tb_settlement_calc_cycle_audit (org_unit_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_scca_merchant_created ON tb_settlement_calc_cycle_audit (merchant_code, created_at DESC);
