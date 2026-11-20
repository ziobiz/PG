-- API 결제창 성명 노출(가맹 API) — URL 성명(url_pay_buyer_name_use_yn)과 분리
-- 채널: URL·챗봇 = url_pay_*, entry=merchant_api = api_*

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS api_buyer_name_use_default_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_hq_api_config.api_buyer_name_use_default_yn IS '가맹 API 결제창 성명 노출 본사 기본 Y/N';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS api_buyer_name_use_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.api_buyer_name_use_yn IS '가맹 API 결제창 성명 — FOLLOW_HQ|Y|N';
