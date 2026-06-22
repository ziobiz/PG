-- URL 분할결제 — API URL 결제방식 SPLIT_PAY 연동·월간격 개월

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_month_interval_months INTEGER NOT NULL DEFAULT 1;

UPDATE tb_merchant_profile
SET api_url_pay_checkout_mode = 'SPLIT_PAY'
WHERE split_pay_enabled_yn = 'Y'
  AND (api_url_pay_checkout_mode IS NULL OR api_url_pay_checkout_mode IN ('', 'STANDARD'));

COMMENT ON COLUMN tb_merchant_profile.split_pay_month_interval_months IS '분할결제 월간 간격 기본 개월 수(1회차 간격)';
