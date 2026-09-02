-- 업체등록 시 VARCHAR(20) 초과로 저장 실패(오류가 노티 URL로 오인됨).
-- 은행명 직접입력·우편번호·주소/계좌 국가명·HTML표시명·PG코드 여유 확보.
ALTER TABLE tb_merchant_profile
    ALTER COLUMN zip_code TYPE VARCHAR(32),
    ALTER COLUMN bank_cd TYPE VARCHAR(100),
    ALTER COLUMN country_cd TYPE VARCHAR(64),
    ALTER COLUMN addr_country_cd TYPE VARCHAR(64),
    ALTER COLUMN web_payment_header_html_title TYPE VARCHAR(80),
    ALTER COLUMN split_pay_header_html_title TYPE VARCHAR(80);

ALTER TABLE tb_merchant_pg_binding
    ALTER COLUMN pg_cd TYPE VARCHAR(40);

COMMENT ON COLUMN tb_merchant_profile.bank_cd IS '은행코드 또는 직접입력 은행명';
COMMENT ON COLUMN tb_merchant_profile.addr_country_cd IS '주소 국가 ISO2 또는 기타 국가명';
COMMENT ON COLUMN tb_merchant_profile.country_cd IS '계좌 국가 ISO2 또는 기타 국가명';
