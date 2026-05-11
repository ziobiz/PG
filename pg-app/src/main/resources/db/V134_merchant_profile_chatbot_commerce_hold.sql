-- 챗봇결제는 켜져 있으나 상위 조직이 상품·예약·결제(상업 기능)만 일시 중지할 때 사용. Y=보류(문의 채팅은 유지).

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_commerce_hold_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_merchant_profile.chatbot_commerce_hold_yn IS 'Y=운영 보류: 공개 챗봇 상품·주문·예약 차단, 문의 채팅 유지';
