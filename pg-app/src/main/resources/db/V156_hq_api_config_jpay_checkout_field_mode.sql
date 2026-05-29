-- JPAY URL 결제창(jpay-pay.html) 입력 필드 본사 기본값.
-- FULL: 전체 입력(카드·성명·이메일·청구지), CARD_ONLY: 카드·성명만, CARD_PREFILL: 카드·성명 입력 + 가맹 청구정보 자동 전송.
-- 운영(application.yml ddl-auto: validate) 시 1회 실행. 로컬 H2(dev)는 ddl-auto: update 로 자동 반영.
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS jpay_checkout_field_mode VARCHAR(20) DEFAULT 'FULL';
UPDATE tb_hq_api_config SET jpay_checkout_field_mode = 'FULL' WHERE jpay_checkout_field_mode IS NULL;
