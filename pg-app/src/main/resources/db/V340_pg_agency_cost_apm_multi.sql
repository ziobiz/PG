-- 결제대행사 등록: 멀티(표시≠청구) + APM 카드 수수료(%)

ALTER TABLE tb_pg_agency_cost_policy
    ADD COLUMN IF NOT EXISTS multi_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    ADD COLUMN IF NOT EXISTS fee_apm_crypto NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_apm_paypay NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_apm_rakuten NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_apm_promptpay NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_apm_line NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_apm_kakao NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_apm_wechat NUMERIC(5, 2) DEFAULT 0;

UPDATE tb_pg_agency_cost_policy
SET multi_yn = COALESCE(NULLIF(TRIM(multi_yn), ''), 'N'),
    fee_apm_crypto = COALESCE(fee_apm_crypto, 0),
    fee_apm_paypay = COALESCE(fee_apm_paypay, 0),
    fee_apm_rakuten = COALESCE(fee_apm_rakuten, 0),
    fee_apm_promptpay = COALESCE(fee_apm_promptpay, 0),
    fee_apm_line = COALESCE(fee_apm_line, 0),
    fee_apm_kakao = COALESCE(fee_apm_kakao, 0),
    fee_apm_wechat = COALESCE(fee_apm_wechat, 0)
WHERE TRUE;
