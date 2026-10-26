-- QR 지갑은 카드와 같은 가맹 결제대행사 행에 둔다. 비어 있으면 QR 없음(기존 카드 행).
ALTER TABLE tb_merchant_pg_binding
    ADD COLUMN IF NOT EXISTS qr_wallet_scope VARCHAR(64) NOT NULL DEFAULT '';

COMMENT ON COLUMN tb_merchant_pg_binding.qr_wallet_scope IS
    'QR 지갑 CSV. 빈 값=QR 없음. PAYPAY=PayPay. 카드브랜드 NONE 이면 이 행은 QR만 받는다.';
