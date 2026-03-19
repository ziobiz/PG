-- 본사 멀티 기준화폐 지원 (최대 3종, comma-separated 예: KRW,USD,JPY)
ALTER TABLE tb_merchant_profile ALTER COLUMN base_currency TYPE VARCHAR(30);
