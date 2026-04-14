-- 정산·담보·환수·미수 금액: 전산설정 수수료·정산(기준통화) 소수 규칙과 동일 스케일로 저장
-- 운영 DB에 수동 적용 시 이 파일 전체를 한 번 실행합니다.

ALTER TABLE tb_settlement_run
  ALTER COLUMN approve_amt TYPE NUMERIC(21,8) USING approve_amt::numeric,
  ALTER COLUMN cancel_amt TYPE NUMERIC(21,8) USING cancel_amt::numeric,
  ALTER COLUMN total_fee TYPE NUMERIC(21,8) USING total_fee::numeric,
  ALTER COLUMN rolling_reserve_amt TYPE NUMERIC(21,8) USING rolling_reserve_amt::numeric,
  ALTER COLUMN pay_amt TYPE NUMERIC(21,8) USING pay_amt::numeric;

ALTER TABLE tb_rolling_reserve
  ALTER COLUMN reserve_amt TYPE NUMERIC(21,8) USING reserve_amt::numeric;

ALTER TABLE tb_settlement_recovery
  ALTER COLUMN recall_amount TYPE NUMERIC(21,8) USING recall_amount::numeric,
  ALTER COLUMN remaining_amount TYPE NUMERIC(21,8) USING remaining_amount::numeric,
  ALTER COLUMN applied_amount TYPE NUMERIC(21,8) USING applied_amount::numeric;

ALTER TABLE tb_merchant_receivable
  ALTER COLUMN total_amount TYPE NUMERIC(21,8) USING total_amount::numeric,
  ALTER COLUMN remaining_amount TYPE NUMERIC(21,8) USING remaining_amount::numeric,
  ALTER COLUMN applied_amount TYPE NUMERIC(21,8) USING applied_amount::numeric;
