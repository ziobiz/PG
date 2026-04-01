ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS api_broker_default_flow_type VARCHAR(20);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_default_flow_type VARCHAR(20);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_path_template VARCHAR(255);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS api_broker_inline_enabled_yn VARCHAR(1);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS api_broker_redirect_enabled_yn VARCHAR(1);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_inline_enabled_yn VARCHAR(1);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_redirect_enabled_yn VARCHAR(1);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS payment_provider_registry_json TEXT;
