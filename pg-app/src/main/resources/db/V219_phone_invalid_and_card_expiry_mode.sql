-- 사전 리스크: 비정상 전화·이메일 + URL 결제 카드 유효기간 입력방식

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS filter_phone_invalid_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS filter_email_invalid_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_card_expiry_mode_default VARCHAR(16) NOT NULL DEFAULT 'DROPDOWN';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_card_expiry_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.url_pay_card_expiry_mode IS 'FOLLOW_HQ|DROPDOWN|TEXT|HYBRID|AI_B|AI_A';
