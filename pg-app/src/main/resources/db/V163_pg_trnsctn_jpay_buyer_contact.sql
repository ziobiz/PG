-- JPAY URL/API 결제 — 구매자 이메일·성명·전화·마스킹 카드번호 (ChillPay 노티에 없던 필드)
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS customer_tel VARCHAR(50);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS card_pan_display VARCHAR(32);
