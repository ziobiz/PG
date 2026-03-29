-- 차지백 구간 정책: 기준통화(표시·집계 단위 안내용)
ALTER TABLE tb_chargeback_fee_policy ADD COLUMN IF NOT EXISTS currency_code VARCHAR(8) NOT NULL DEFAULT 'KRW';
