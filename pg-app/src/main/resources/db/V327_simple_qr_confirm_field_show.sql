-- 간편결제(QR URL) 확인 페이지 필드 노출 Y/N (가맹 API 연동 채널)
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS simple_qr_show_order_no_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS simple_qr_show_product_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS simple_qr_show_name_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS simple_qr_show_email_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    ADD COLUMN IF NOT EXISTS simple_qr_show_phone_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_merchant_profile.simple_qr_show_order_no_yn IS '간편결제 확인·QR 페이지 주문번호 노출 Y/N';
COMMENT ON COLUMN tb_merchant_profile.simple_qr_show_product_yn IS '간편결제 확인·QR 페이지 상품명 노출 Y/N';
COMMENT ON COLUMN tb_merchant_profile.simple_qr_show_name_yn IS '간편결제 확인·QR 페이지 성명 노출 Y/N';
COMMENT ON COLUMN tb_merchant_profile.simple_qr_show_email_yn IS '간편결제 확인·QR 페이지 이메일 노출 Y/N';
COMMENT ON COLUMN tb_merchant_profile.simple_qr_show_phone_yn IS '간편결제 확인·QR 페이지 전화번호 노출 Y/N';
