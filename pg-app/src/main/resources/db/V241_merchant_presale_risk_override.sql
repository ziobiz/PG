-- 가맹점별 리스크 필터링(사전필터) 오버라이드
-- FOLLOW_HQ | DISABLED | CUSTOM — 기본 FOLLOW_HQ(본사 전역 사전필터 그대로)

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_mode VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_buyer_mismatch_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_holder_name_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_phone_invalid_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_email_invalid_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_velocity_card_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_velocity_email_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_velocity_ip_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_vel_card_win_min INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_vel_card_max INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_vel_email_win_min INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_vel_email_max INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_vel_ip_win_min INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_presale_vel_ip_max INTEGER;
