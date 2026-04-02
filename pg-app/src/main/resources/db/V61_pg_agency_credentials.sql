-- PG사 API 연동(tb_pg_agency): PG사별 자격 증명(MID·키·라우트 등)
ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS merchant_mid VARCHAR(100);

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS api_key VARCHAR(512);

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS md5_secret_key VARCHAR(255);

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS route_no INTEGER;

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS sandbox_yn VARCHAR(1) DEFAULT 'Y';

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS credentials_extra_json TEXT;

COMMENT ON COLUMN tb_pg_agency.merchant_mid IS 'PG별 MID/Merchant Code';
COMMENT ON COLUMN tb_pg_agency.api_key IS 'PG API Key 등';
COMMENT ON COLUMN tb_pg_agency.md5_secret_key IS 'CheckSum/서명용 시크릿(예 ChillPay MD5)';
COMMENT ON COLUMN tb_pg_agency.route_no IS 'PG별 Route No(있는 경우)';
COMMENT ON COLUMN tb_pg_agency.sandbox_yn IS 'Y/N 샌드박스';
COMMENT ON COLUMN tb_pg_agency.credentials_extra_json IS 'PG별 추가 키·설정 JSON';
