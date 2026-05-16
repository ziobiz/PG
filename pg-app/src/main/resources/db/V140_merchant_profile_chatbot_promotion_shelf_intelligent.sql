-- 챗봇-pay 상단 프로모션: 인텔리전트(히든/프로모션/다이나믹/하이브리드) + 순환 간격(초, 30초 단위)

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_promotion_shelf_mode VARCHAR(16) NOT NULL DEFAULT 'PROMOTION';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_promotion_rotate_seconds INTEGER NOT NULL DEFAULT 30;

COMMENT ON COLUMN tb_merchant_profile.chatbot_promotion_shelf_mode IS
    '챗봇-pay 상단 프로모션: HIDDEN(미노출), PROMOTION(전체 그리드), DYNAMIC(3칸 순환), HYBRID(좌1고정+2칸순환).';
COMMENT ON COLUMN tb_merchant_profile.chatbot_promotion_rotate_seconds IS
    'DYNAMIC·HYBRID 순환 주기(초). 30초 단위, 기본 30.';
