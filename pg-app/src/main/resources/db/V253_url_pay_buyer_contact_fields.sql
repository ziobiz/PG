-- 결제창 구매자 입력(이메일·국가코드·전화번호) — 전 PG 공통. 본사 기본 + 가맹 FOLLOW_HQ/Y/N
-- 기존 JPAY 1·2·3형(jpay_checkout_field_mode)에서 마이그레이션 후, UI는 공통 토글로 통일.

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_buyer_email_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_buyer_country_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_buyer_phone_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_hq_api_config.url_pay_buyer_email_use_default_yn IS '결제창 이메일 입력 본사 기본 Y/N';
COMMENT ON COLUMN tb_hq_api_config.url_pay_buyer_country_use_default_yn IS '결제창 국가코드 입력 본사 기본 Y/N';
COMMENT ON COLUMN tb_hq_api_config.url_pay_buyer_phone_use_default_yn IS '결제창 전화번호 입력 본사 기본 Y/N';

-- 본사: 기존 JPAY 3형(CARD_PREFILL)이면 연락처 기본 비활성
UPDATE tb_hq_api_config
SET url_pay_buyer_email_use_default_yn = 'N',
    url_pay_buyer_country_use_default_yn = 'N',
    url_pay_buyer_phone_use_default_yn = 'N'
WHERE UPPER(TRIM(COALESCE(jpay_checkout_field_mode, ''))) = 'CARD_PREFILL';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_buyer_email_use_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_buyer_country_use_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_buyer_phone_use_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.url_pay_buyer_email_use_yn IS '결제창 이메일 — FOLLOW_HQ|Y|N';
COMMENT ON COLUMN tb_merchant_profile.url_pay_buyer_country_use_yn IS '결제창 국가코드 — FOLLOW_HQ|Y|N';
COMMENT ON COLUMN tb_merchant_profile.url_pay_buyer_phone_use_yn IS '결제창 전화번호 — FOLLOW_HQ|Y|N';

-- 가맹: JPAY 3형 개별 설정이면 연락처 비활성(본사 따름이 아닌 경우만)
UPDATE tb_merchant_profile
SET url_pay_buyer_email_use_yn = 'N',
    url_pay_buyer_country_use_yn = 'N',
    url_pay_buyer_phone_use_yn = 'N'
WHERE UPPER(TRIM(COALESCE(jpay_checkout_field_mode, ''))) = 'CARD_PREFILL';
