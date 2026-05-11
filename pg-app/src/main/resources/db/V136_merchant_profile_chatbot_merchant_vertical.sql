-- 챗봇 기본설정 — 가맹점 업체성격(운영방식과 별개: 주문·예약 질문 흐름·LLM 안내)

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_merchant_vertical VARCHAR(40) NOT NULL DEFAULT 'GENERAL_SALE';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_merchant_vertical_notes TEXT;

COMMENT ON COLUMN tb_merchant_profile.chatbot_merchant_vertical IS
    '가맹점 업체성격: GENERAL_SALE, ECOMMERCE, CONSULTING, REAL_ESTATE, AUTO_SALES, SERVICE_TRADE, MASSAGE_GENERAL, COSMETIC, CLUB_ENTERTAINMENT, CLUB_MASSAGE, RESTAURANT, VIP_CLUB, OTHER';

COMMENT ON COLUMN tb_merchant_profile.chatbot_merchant_vertical_notes IS
    '본사·총판 전용: 업체성격 보조 메모(필수 질문 키워드 등). LLM 주문·예약 수집 시 반영';
