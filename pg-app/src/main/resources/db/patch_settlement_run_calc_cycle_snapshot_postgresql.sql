-- 정산 실행 행에 저장 시점 정산주기(가맹 설정 변경 후에도 과거 행 표시용)
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS calc_cycle_snapshot VARCHAR(64);
