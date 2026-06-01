-- 비활성카드(블랙리스트) 등록·해지 감사 컬럼
-- 운영 PostgreSQL 1회. H2 dev는 ddl-auto update.

ALTER TABLE tb_hq_pay_card_blacklist ADD COLUMN IF NOT EXISTS registered_by VARCHAR(64);
ALTER TABLE tb_hq_pay_card_blacklist ADD COLUMN IF NOT EXISTS released_at TIMESTAMP;
ALTER TABLE tb_hq_pay_card_blacklist ADD COLUMN IF NOT EXISTS released_by VARCHAR(64);
