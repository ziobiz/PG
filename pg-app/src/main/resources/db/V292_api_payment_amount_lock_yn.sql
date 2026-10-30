-- 본사 결제로직: API 결제 금액 고정 (기본 Y = 수정 불가)
ALTER TABLE tb_hq_api_config
  ADD COLUMN IF NOT EXISTS api_payment_amount_lock_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

UPDATE tb_hq_api_config
SET api_payment_amount_lock_yn = 'Y'
WHERE api_payment_amount_lock_yn IS NULL OR TRIM(api_payment_amount_lock_yn) = '';
