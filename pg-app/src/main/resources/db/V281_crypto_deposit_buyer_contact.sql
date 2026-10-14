-- V281: 크립토 입금 거래 — 카드와 동일 구매자 연락처(이메일·성명·국가·전화·배송)

ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS buyer_phone VARCHAR(40);

ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS buyer_country_iso2 VARCHAR(2);

ALTER TABLE tb_crypto_deposit_txn
    ADD COLUMN IF NOT EXISTS buyer_shipping_json VARCHAR(1000);

COMMENT ON COLUMN tb_crypto_deposit_txn.buyer_phone IS '구매자 전화번호(국가번호 제외 로컬)';
COMMENT ON COLUMN tb_crypto_deposit_txn.buyer_country_iso2 IS '구매자 국가 ISO2';
COMMENT ON COLUMN tb_crypto_deposit_txn.buyer_shipping_json IS '배송주소 JSON(선택)';
