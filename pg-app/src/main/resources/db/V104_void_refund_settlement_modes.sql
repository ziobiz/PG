-- 무효·수동무효·환불·강제환불 정산 방식 (GENERAL / REVENUE / HYBRID)
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN void_settlement_mode VARCHAR(16) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN manual_void_settlement_mode VARCHAR(16) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN refund_settlement_mode VARCHAR(16) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN force_refund_settlement_mode VARCHAR(16) NOT NULL DEFAULT 'GENERAL';

ALTER TABLE tb_commission_policy
    ADD COLUMN void_settlement_mode VARCHAR(16) NULL,
    ADD COLUMN manual_void_settlement_mode VARCHAR(16) NULL,
    ADD COLUMN refund_settlement_mode VARCHAR(16) NULL,
    ADD COLUMN force_refund_settlement_mode VARCHAR(16) NULL;
