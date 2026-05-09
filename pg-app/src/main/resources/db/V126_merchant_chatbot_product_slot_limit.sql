-- 챗봇 카탈로그 상품 등록 한도(10~200) 및 월 이용료 미수금 청구 단위
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS chatbot_product_slot_limit INTEGER NULL;

COMMENT ON COLUMN tb_merchant_profile.chatbot_product_slot_limit IS '챗봇 상품 등록 가능 개수 티어(10,20,50,80,100,150,200). 월간 미수금 청구 및 등록 건수 제한에 사용.';

UPDATE tb_merchant_profile mp
SET chatbot_product_slot_limit = 10
WHERE COALESCE(TRIM(UPPER(mp.chatbot_payment_use_yn)), 'N') = 'Y'
  AND mp.chatbot_product_slot_limit IS NULL;
