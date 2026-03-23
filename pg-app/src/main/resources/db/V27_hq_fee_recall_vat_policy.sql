-- 본사 환경설정: 환수금 수수료 포함 여부, 정산 VAT 적용 여부
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS recall_include_fee_yn VARCHAR(1) DEFAULT 'N';

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS settlement_vat_apply_yn VARCHAR(1) DEFAULT 'Y';
