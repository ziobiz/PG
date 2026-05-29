-- JPAY URL 결제창(jpay-pay.html) 입력 필드 가맹 오버라이드.
-- NULL/빈값이면 본사 기본값(tb_hq_api_config.jpay_checkout_field_mode)을 따른다.
-- FULL / CARD_ONLY / CARD_PREFILL. 운영(ddl-auto: validate) 시 1회 실행. 로컬 H2(dev)는 ddl-auto: update.
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS jpay_checkout_field_mode VARCHAR(20);
