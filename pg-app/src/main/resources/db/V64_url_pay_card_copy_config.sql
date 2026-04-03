-- 결제구문설정: URL 결제 카드 안내 문구(PG별·다국어 JSON)
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS url_pay_card_copy_config_json TEXT;
