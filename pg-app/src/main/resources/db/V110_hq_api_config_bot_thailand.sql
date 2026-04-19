-- PostgreSQL: 본사 결제로직설정 — 태국은행 BOT 일평균 환율(URL DISPLAY·FX 자동).
-- 운영은 ddl-auto validate 이므로 배포 후 1회 실행. 비우면 application.yml / 환경변수 BOT_THAILAND_* 사용.

ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS bot_thailand_api_key VARCHAR(512);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS bot_thailand_base_url VARCHAR(512);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS bot_thailand_daily_avg_path VARCHAR(255);
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS bot_thailand_api_key_header VARCHAR(64);

COMMENT ON COLUMN tb_hq_api_config.bot_thailand_api_key IS 'BOT Stat-ExchangeRate API 키(포털 Client ID). 비우면 BOT_THAILAND_API_KEY';
COMMENT ON COLUMN tb_hq_api_config.bot_thailand_base_url IS '예: https://gateway.api.bot.or.th/Stat-ExchangeRate/v2 또는 https://iapi.bot.or.th. 비우면 BOT_THAILAND_BASE_URL';
COMMENT ON COLUMN tb_hq_api_config.bot_thailand_daily_avg_path IS '예: /DAILY_AVG_EXG_RATE/ 또는 /Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/. 비우면 기본 경로';
COMMENT ON COLUMN tb_hq_api_config.bot_thailand_api_key_header IS 'Authorization 또는 api-key. 비우면 BOT_THAILAND_API_KEY_HEADER 기본 api-key';
