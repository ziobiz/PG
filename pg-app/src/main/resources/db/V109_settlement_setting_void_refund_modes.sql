-- 총판·가맹 tb_settlement_setting: 무효·환불 정산 방식 (NULL = 상위 기본 따름, 총판은 본사, 가맹은 총판·본사)
ALTER TABLE tb_settlement_setting
    ADD COLUMN void_settlement_mode VARCHAR(20) NULL,
    ADD COLUMN manual_void_settlement_mode VARCHAR(20) NULL,
    ADD COLUMN refund_settlement_mode VARCHAR(20) NULL,
    ADD COLUMN force_refund_settlement_mode VARCHAR(20) NULL,
    ADD COLUMN void_refund_settlement_override_yn VARCHAR(1) NOT NULL DEFAULT 'N';
