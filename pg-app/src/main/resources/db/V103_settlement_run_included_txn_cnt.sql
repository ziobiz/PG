-- 정산 실행 1건에 포함된 거래(pg_trnsctn) 건수. 신규 실행부터 저장. 구데이터는 NULL.
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS included_txn_cnt INTEGER;

COMMENT ON COLUMN tb_settlement_run.included_txn_cnt IS '정산 집계에 포함된 거래 건수(SettlementCalcService calcOne의 txList 크기)';
