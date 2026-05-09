-- 가맹 챗봇 상품 등록 전용 관리자( 사용자 1명 ) + 공개 페이지용 세션 토큰

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_admin_user_id BIGINT;

COMMENT ON COLUMN tb_merchant_profile.chatbot_admin_user_id IS '챗봇에서 상품 CRUD 허용 tb_user.id, 가맹당 1명';

CREATE TABLE IF NOT EXISTS tb_chatbot_admin_session (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chatbot_admin_session_expires ON tb_chatbot_admin_session (expires_at);
CREATE INDEX IF NOT EXISTS idx_chatbot_admin_session_user ON tb_chatbot_admin_session (user_id);
