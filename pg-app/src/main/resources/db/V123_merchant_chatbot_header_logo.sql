-- 공개 챗봇 결제 화면 상단 로고(URL). 비우면 상위 조직 브랜딩 로고로 폴백.
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS chatbot_header_logo_url VARCHAR(500);
