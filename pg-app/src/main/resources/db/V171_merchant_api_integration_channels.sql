-- 가맹 API 연동 채널(인라인·리다이렉트·WordPress) — 가맹별 오픈 + 본사 WordPress 전역
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS api_broker_inline_use_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS api_broker_redirect_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS api_wordpress_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS api_wordpress_plugin_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
