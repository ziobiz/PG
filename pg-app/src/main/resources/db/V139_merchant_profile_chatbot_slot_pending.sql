-- 챗봇 플랜 다운그레이드: 당월은 기존 슬롯 유지, 다음 달 1일(서울 달력 월)부터 적용할 목표 슬롯

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_product_slot_limit_pending INTEGER;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_product_slot_pending_apply_ym VARCHAR(7);

COMMENT ON COLUMN tb_merchant_profile.chatbot_product_slot_limit_pending IS
    '다음 적용 월부터 반영할 챗봇 상품등록 플랜(건). 당월 chatbot_product_slot_limit 은 유지.';
COMMENT ON COLUMN tb_merchant_profile.chatbot_product_slot_pending_apply_ym IS
    'pending 적용 시작 달(YYYY-MM, Asia/Seoul). 해당 월 도달 시 메인 슬롯으로 이관 후 비움.';
