-- 결제대행사 등록: 카드수수료 통합/개별(브랜드별, 0=미사용)

ALTER TABLE tb_pg_agency_cost_policy
    ADD COLUMN IF NOT EXISTS card_fee_mode VARCHAR(16) NOT NULL DEFAULT 'UNIFIED',
    ADD COLUMN IF NOT EXISTS fee_card_visa NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_card_master NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_card_jcb NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_card_amex NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_card_union NUMERIC(5, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_card_diners NUMERIC(5, 2) DEFAULT 0;

UPDATE tb_pg_agency_cost_policy
SET card_fee_mode = COALESCE(NULLIF(TRIM(card_fee_mode), ''), 'UNIFIED'),
    fee_card_visa = COALESCE(fee_card_visa, 0),
    fee_card_master = COALESCE(fee_card_master, 0),
    fee_card_jcb = COALESCE(fee_card_jcb, 0),
    fee_card_amex = COALESCE(fee_card_amex, 0),
    fee_card_union = COALESCE(fee_card_union, 0),
    fee_card_diners = COALESCE(fee_card_diners, 0)
WHERE TRUE;
