-- 본사 리스크설정(카드 실패 쿨다운·자동 비활성 트리거) + 가맹점별 정책

CREATE TABLE IF NOT EXISTS tb_hq_risk_card_policy (
    id                          BIGINT PRIMARY KEY DEFAULT 1,
    enabled_yn                  VARCHAR(1)  NOT NULL DEFAULT 'Y',
    tier1_hours                 INTEGER     NOT NULL DEFAULT 0,
    tier1_min                   INTEGER     NOT NULL DEFAULT 5,
    tier2_hours                 INTEGER     NOT NULL DEFAULT 0,
    tier2_min                   INTEGER     NOT NULL DEFAULT 10,
    tier3_hours                 INTEGER     NOT NULL DEFAULT 1,
    tier3_min                   INTEGER     NOT NULL DEFAULT 0,
    tier4_hours                 INTEGER     NOT NULL DEFAULT 0,
    tier4_min                   INTEGER     NOT NULL DEFAULT 0,
    auto_blacklist_trigger_tier INTEGER     NOT NULL DEFAULT 4,
    updated_at                  TIMESTAMP
);

INSERT INTO tb_hq_risk_card_policy (id, updated_at)
SELECT 1, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_hq_risk_card_policy WHERE id = 1);

-- 기존 전산설정 값 이관(있을 때)
UPDATE tb_hq_risk_card_policy h
SET enabled_yn = COALESCE(l.card_fail_cooldown_enabled_yn, h.enabled_yn),
    tier1_min = COALESCE(l.card_fail_cooldown_tier1_min, h.tier1_min),
    tier2_min = COALESCE(l.card_fail_cooldown_tier2_min, h.tier2_min),
    tier3_min = COALESCE(l.card_fail_cooldown_tier3_min, h.tier3_min),
    updated_at = CURRENT_TIMESTAMP
FROM tb_hq_ledger_sys_settings l
WHERE h.id = 1 AND l.id IS NOT NULL;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_policy_mode VARCHAR(16) NOT NULL DEFAULT 'DISABLED';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_tier1_hours INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_tier1_min INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_tier2_hours INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_tier2_min INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_tier3_hours INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_tier3_min INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_tier4_hours INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_tier4_min INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_auto_blacklist_tier INTEGER;

COMMENT ON COLUMN tb_merchant_profile.card_risk_policy_mode IS 'FOLLOW_HQ|CUSTOM|DISABLED';

ALTER TABLE tb_pay_card_fail_cooldown
    ADD COLUMN IF NOT EXISTS org_unit_id BIGINT;

DROP INDEX IF EXISTS ux_pay_card_fail_cooldown_pg_hash;

CREATE UNIQUE INDEX IF NOT EXISTS ux_pay_card_fail_cooldown_pg_hash_org
    ON tb_pay_card_fail_cooldown (pg_vendor, pan_hash, COALESCE(org_unit_id, 0));

ALTER TABLE tb_hq_pay_card_blacklist
    ADD COLUMN IF NOT EXISTS registered_org_unit_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_pay_card_blacklist_reg_org
    ON tb_hq_pay_card_blacklist (registered_org_unit_id, active_yn);
