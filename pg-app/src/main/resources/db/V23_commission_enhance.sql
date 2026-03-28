-- 유통 수수료: 조직별 건당수수료, 영업점 비율, 적용시작일
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS hq_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS regional_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS master_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS branch_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS agency_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS sales_office_per_tx_fee DECIMAL(12, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS sales_office_rate DECIMAL(5, 2);
ALTER TABLE tb_distribution_fee_config ADD COLUMN IF NOT EXISTS apply_start_date DATE;

-- 수수료 변경 이력: 스냅샷 JSON, 변경자
-- PostgreSQL: CLOB 없음 → TEXT (JPA columnDefinition = TEXT 와 동일)
ALTER TABLE tb_commission_history ADD COLUMN IF NOT EXISTS snapshot_json TEXT;
ALTER TABLE tb_commission_history ADD COLUMN IF NOT EXISTS changed_by VARCHAR(100);
