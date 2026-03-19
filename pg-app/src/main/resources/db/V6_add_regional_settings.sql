-- ============================================================
-- 본사(REGIONAL) 전용 설정 (2026-03)
-- 업체 상세 정보, 정산정보, 출금/결제 제한, 기본 수수료, 카드사별 한도, 터미널 등
-- ============================================================

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS regional_settings TEXT;

COMMENT ON COLUMN tb_merchant_profile.regional_settings IS '본사(REGIONAL) 전용 JSON 설정';
