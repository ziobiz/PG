-- URL/RE 결제창 성명(리다이렉트용) 활성 — 본사 기본 + 가맹 FOLLOW_HQ|Y|N
-- 이메일·국가·전화·배송과 동일 패턴. 프리셋 잠금 대상이 아님(개별 제어).

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_buyer_name_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_hq_api_config.url_pay_buyer_name_use_default_yn IS '결제창 성명(리다이렉트·URL) 입력 본사 기본 Y/N';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_buyer_name_use_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.url_pay_buyer_name_use_yn IS '결제창 성명(리다이렉트·URL) — FOLLOW_HQ|Y|N';
