-- 한국은행 ECOS Open API 인증키 (URL DP FX 소스 ECOS)
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS bok_ecos_api_key VARCHAR(512);

COMMENT ON COLUMN tb_hq_api_config.bok_ecos_api_key IS
    '한국은행 ECOS Open API 인증키(StatisticSearch). 비우면 BOK_ECOS_API_KEY 환경변수·yml';
