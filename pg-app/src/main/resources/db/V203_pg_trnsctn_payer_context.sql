-- 결제개요(검수): 결제 시 단말기·지역·IP 컨텍스트
ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS payer_client_ip VARCHAR(64) NULL;

ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS payer_device_category VARCHAR(32) NULL;

ALTER TABLE pg_trnsctn
    ADD COLUMN IF NOT EXISTS payer_country_iso2 CHAR(2) NULL;

COMMENT ON COLUMN pg_trnsctn.payer_client_ip IS '결제 고객 IP';
COMMENT ON COLUMN pg_trnsctn.payer_device_category IS 'PC|MOBILE_IOS|MOBILE_ANDROID|MOBILE_OTHER|TABLET|UNKNOWN';
COMMENT ON COLUMN pg_trnsctn.payer_country_iso2 IS '결제 접속/청구 국가 ISO2';
