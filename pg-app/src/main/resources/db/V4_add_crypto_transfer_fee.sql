-- ============================================================
-- 크립토 이체 수수료 컬럼 추가 (2026-02)
-- ============================================================

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS crypto_transfer_fee VARCHAR(50);
