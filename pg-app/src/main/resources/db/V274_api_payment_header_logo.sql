-- V274: 가맹 API/embed 전용 결제창 상단 로고 (웹결제 로고와 분리)
-- 기본 DISABLED — API 연동 시에만 활성 시 노출

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS api_payment_header_logo_mode VARCHAR(16) NOT NULL DEFAULT 'DISABLED';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS api_payment_header_logo_url VARCHAR(500);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS api_payment_header_html_title VARCHAR(80);

COMMENT ON COLUMN tb_merchant_profile.api_payment_header_logo_mode IS
    'API/embed 결제창 상단 로고: DISABLED|FOLLOW_HQ|DEFAULT|HTML|ACTIVE. FOLLOW_HQ=웹결제 로고 따름';

COMMENT ON COLUMN tb_merchant_profile.api_payment_header_logo_url IS
    'API 로고 URL — mode=ACTIVE 시 가맹 업로드';

COMMENT ON COLUMN tb_merchant_profile.api_payment_header_html_title IS
    'API 로고 HTML 모드 표시명';
