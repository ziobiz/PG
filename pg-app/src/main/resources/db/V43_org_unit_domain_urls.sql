-- 본사·총판 조직별 도메인(관리자 웹/API) 및 설정명
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS domain_setting_name VARCHAR(200);
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS org_domain_admin_url VARCHAR(500);
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS org_domain_api_url VARCHAR(500);
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS domain_urls_updated_at TIMESTAMP;
