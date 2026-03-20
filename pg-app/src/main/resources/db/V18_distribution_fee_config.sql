CREATE TABLE IF NOT EXISTS tb_distribution_fee_config (
    id BIGSERIAL PRIMARY KEY,
    comp_id VARCHAR(32) NOT NULL UNIQUE,
    hq_rate NUMERIC(5,2) DEFAULT 0,
    regional_rate NUMERIC(5,2) DEFAULT 0,
    master_rate NUMERIC(5,2) DEFAULT 0,
    branch_rate NUMERIC(5,2) DEFAULT 0,
    agency_rate NUMERIC(5,2) DEFAULT 0,
    updated_at TIMESTAMP
);

