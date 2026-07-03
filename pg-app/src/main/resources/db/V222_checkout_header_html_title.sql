-- 웹결제·URL 분할결제 — 기본(HTML) 모드 상단 표시명 (최대 20자)
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS web_payment_header_html_title VARCHAR(20);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS split_pay_header_html_title VARCHAR(20);

COMMENT ON COLUMN tb_merchant_profile.web_payment_header_html_title IS '웹결제 HTML 로고 모드 상단 표시명(20자)';
COMMENT ON COLUMN tb_merchant_profile.split_pay_header_html_title IS '분할결제 HTML 로고 모드 상단 표시명(20자)';
