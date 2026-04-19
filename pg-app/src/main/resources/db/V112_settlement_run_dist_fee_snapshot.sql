-- 가맹 정산 실행 확정 시(미수·환수 반영 후 최종 지급액 기준) 유통 단계별 % 분배 스냅샷.
-- 유통망정산내역 집계는 기존처럼 실행 행을 합산하되, 스냅샷이 있으면 해당 값을 사용해 요율 변경 후에도 당시 분배를 재현합니다.
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS dist_hq_fee_amt DECIMAL(21, 8);
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS dist_regional_fee_amt DECIMAL(21, 8);
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS dist_master_fee_amt DECIMAL(21, 8);
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS dist_branch_fee_amt DECIMAL(21, 8);
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS dist_agency_fee_amt DECIMAL(21, 8);
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS dist_sales_office_fee_amt DECIMAL(21, 8);

COMMENT ON COLUMN tb_settlement_run.dist_hq_fee_amt IS '유통 총본사 분배(정산 확정 지급액×요율, 전산 반올림)';
COMMENT ON COLUMN tb_settlement_run.dist_regional_fee_amt IS '유통 본사 분배';
COMMENT ON COLUMN tb_settlement_run.dist_master_fee_amt IS '유통 총판 분배';
COMMENT ON COLUMN tb_settlement_run.dist_branch_fee_amt IS '유통 지사 분배';
COMMENT ON COLUMN tb_settlement_run.dist_agency_fee_amt IS '유통 대리점 분배';
COMMENT ON COLUMN tb_settlement_run.dist_sales_office_fee_amt IS '유통 영업점 분배';
