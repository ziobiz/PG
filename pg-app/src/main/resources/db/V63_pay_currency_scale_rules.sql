-- 결제통화로직설정: URL 결제 입력 금액 → PG(ChillPay) 전송 금액 배수 (×100 / ÷100 / 동일)
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS pay_currency_scale_rules_json TEXT;
