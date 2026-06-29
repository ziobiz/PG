-- URL·JPAY 공개 결제창 배송 주소 입력 — 가맹별 Y/N (기본 미사용)
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_shipping_address_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_merchant_profile.url_pay_shipping_address_use_yn IS 'URL 결제창 배송 주소 입력 — Y=표시·필수(FULL·1형), N=미표시(기본)';
