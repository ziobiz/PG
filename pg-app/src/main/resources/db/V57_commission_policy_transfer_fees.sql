ALTER TABLE tb_commission_policy
    ADD COLUMN IF NOT EXISTS remittance_transfer_fee NUMERIC(12,1) DEFAULT 0;

ALTER TABLE tb_commission_policy
    ADD COLUMN IF NOT EXISTS usdt_transfer_fee_usd NUMERIC(12,1) DEFAULT 0;
