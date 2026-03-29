-- 차지백 구간 정책(본사): 월간 환불·강제환불(상태 30/31) 건수 구간별 건당 수수료
CREATE TABLE IF NOT EXISTS tb_chargeback_fee_policy (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    remark TEXT,
    currency_code VARCHAR(8) NOT NULL DEFAULT 'KRW',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_chargeback_fee_tier (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES tb_chargeback_fee_policy(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    count_min INT NOT NULL DEFAULT 0,
    count_max INT,
    fee_per_case NUMERIC(12, 0) NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cb_fee_tier_policy ON tb_chargeback_fee_tier(policy_id);

ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS chargeback_policy_id BIGINT
    REFERENCES tb_chargeback_fee_policy(id) ON DELETE SET NULL;
