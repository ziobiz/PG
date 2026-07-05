-- V226: 멀티 PG 라우팅 방식(브랜드/통화/혼합) + 가맹 결제대행사 통화 범위
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS multi_pg_routing_mode VARCHAR(32) NOT NULL DEFAULT 'BRAND_AND_CURRENCY';

COMMENT ON COLUMN tb_hq_api_config.multi_pg_routing_mode IS
    'BRAND=카드브랜드만, CURRENCY=통화만, BRAND_AND_CURRENCY=브랜드+통화 혼합(행별 ALL 허용)';

ALTER TABLE tb_merchant_pg_binding
    ADD COLUMN IF NOT EXISTS currency_scope VARCHAR(8) NOT NULL DEFAULT 'ALL';

COMMENT ON COLUMN tb_merchant_pg_binding.currency_scope IS
    '멀티 PG 라우팅 통화 범위: ALL 또는 JPY/USD/KRW/THB/SGD/HKD/CNY 등 ISO 알파';
