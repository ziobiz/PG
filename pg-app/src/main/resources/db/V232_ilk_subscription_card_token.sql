-- ILK 구독 MIT 재청구용 카드 시드 암호화 보관(에이전시 seed 로 암복호)
ALTER TABLE tb_merchant_ilk_subscription
    ADD COLUMN IF NOT EXISTS card_token_enc VARCHAR(512);
ALTER TABLE tb_merchant_ilk_subscription
    ADD COLUMN IF NOT EXISTS card_exp_month_enc VARCHAR(128);
ALTER TABLE tb_merchant_ilk_subscription
    ADD COLUMN IF NOT EXISTS card_exp_year_enc VARCHAR(128);

COMMENT ON COLUMN tb_merchant_ilk_subscription.card_token_enc IS
    'ILK MIT 재청구용 카드번호 AES(Base64). 운영 시드키로만 복호화.';
