-- 본사설정 확장: 기본정책(통화·3DS·차지백·비고), 도메인 URL, 서버관리 SSL 경로, 브랜딩 로그인 호스트

ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS currency_code VARCHAR(16);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS policy_remark TEXT;
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS fee_3ds_rate NUMERIC(5, 2);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS chargeback_fee_per_tx NUMERIC(12, 0);

UPDATE tb_commission_policy SET currency_code = 'KRW' WHERE currency_code IS NULL;

ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS public_admin_site_url VARCHAR(500);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS public_api_base_url VARCHAR(500);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_ssl_cert_path VARCHAR(500);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_ssl_le_domain VARCHAR(255);

ALTER TABLE tb_org_branding ADD COLUMN IF NOT EXISTS brand_host VARCHAR(255);
