ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS business_day_settings_json TEXT;

