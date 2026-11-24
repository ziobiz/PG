-- PG 아웃바운드 구매자 이메일증명: 본사 기본 활성(Y), 가맹 본사설정따름(FOLLOW_HQ)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS pg_buyer_email_attest_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_hq_api_config.pg_buyer_email_attest_yn IS
    '이메일증명 본사 기본 Y=실제 구매자 이메일을 PG로 전송 / N=noreply@icopay.co.kr 강제 치환';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS pg_buyer_email_attest_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.pg_buyer_email_attest_yn IS
    '이메일증명 FOLLOW_HQ|Y|N — FOLLOW_HQ면 본사 pg_buyer_email_attest_yn, Y/N이면 가맹 우선';
