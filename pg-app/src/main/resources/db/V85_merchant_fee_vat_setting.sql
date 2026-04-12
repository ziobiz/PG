-- 가맹점 정산방법: 수수료 부가세(VAT) 사용 여부 및 세율(%)
ALTER TABLE tb_settlement_setting
    ADD COLUMN IF NOT EXISTS fee_vat_apply_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_settlement_setting
    ADD COLUMN IF NOT EXISTS fee_vat_rate_pct NUMERIC(7, 4) NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_settlement_setting.fee_vat_apply_yn IS '수수료 부가세 적용 Y/N';
COMMENT ON COLUMN tb_settlement_setting.fee_vat_rate_pct IS '수수료 부가세율(%) — fee_vat_apply_yn=Y일 때만 적용';
