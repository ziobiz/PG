-- PostgreSQL 등 영구 DB용. 운영(ddl-auto: validate) 시 1회 실행.

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_kb_welcome_hint TEXT;

COMMENT ON COLUMN tb_merchant_profile.chatbot_kb_welcome_hint IS '공개 챗봇 첫 진입 시 상단 버블 기본 안내(비우면 시스템 기본 문구)';
