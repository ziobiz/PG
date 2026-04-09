-- URL 결제: 통화직접결제(CHECKOUT_CURRENCY) vs 표시통화→실결제 THB(DISPLAY_FX_THB)
ALTER TABLE tb_merchant_pg_binding
    ADD COLUMN IF NOT EXISTS url_pay_pricing_mode VARCHAR(32) NOT NULL DEFAULT 'CHECKOUT_CURRENCY';
COMMENT ON COLUMN tb_merchant_pg_binding.url_pay_pricing_mode IS 'CHECKOUT_CURRENCY | DISPLAY_FX_THB';

-- 본사: BOT 일평균 환율·마진·갱신주기 등 JSON (tb_hq_api_config)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_display_fx_json TEXT;
COMMENT ON COLUMN tb_hq_api_config.url_pay_display_fx_json IS 'URL 표시통화(THB정산) 설정 JSON';
