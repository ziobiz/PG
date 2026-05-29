-- JPAY URL 결제창(jpay-pay.html) 전화 국가번호 드롭다운 — 본사 기본·가맹 오버라이드.
-- Y: 국가번호 선택 UI + 접속국가(또는 URL phoneDial) 우선 / N: 기존 단일 전화 입력.
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS jpay_phone_dial_code_yn VARCHAR(1) DEFAULT 'N';
UPDATE tb_hq_api_config SET jpay_phone_dial_code_yn = 'N' WHERE jpay_phone_dial_code_yn IS NULL;

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS jpay_phone_dial_code_yn VARCHAR(1);
-- NULL = 본사 기본 따름
