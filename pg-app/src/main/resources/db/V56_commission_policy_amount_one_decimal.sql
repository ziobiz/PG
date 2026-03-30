-- 건당·고정 수수료: USD·THB 등 소수 첫째 자리 보존 (기존 정수 값은 그대로)
ALTER TABLE tb_commission_policy
  ALTER COLUMN per_tx_fee TYPE NUMERIC(12, 1) USING round(per_tx_fee::numeric, 1),
  ALTER COLUMN cancel_rate TYPE NUMERIC(12, 1) USING round(cancel_rate::numeric, 1),
  ALTER COLUMN usage_rate TYPE NUMERIC(12, 1) USING round(usage_rate::numeric, 1),
  ALTER COLUMN fail_fee TYPE NUMERIC(12, 1) USING round(fail_fee::numeric, 1),
  ALTER COLUMN refund_rate TYPE NUMERIC(12, 1) USING round(refund_rate::numeric, 1),
  ALTER COLUMN void_fee_per_tx TYPE NUMERIC(12, 1) USING round(void_fee_per_tx::numeric, 1),
  ALTER COLUMN manual_void_fee_per_tx TYPE NUMERIC(12, 1) USING round(manual_void_fee_per_tx::numeric, 1),
  ALTER COLUMN fee_settlement_per_tx TYPE NUMERIC(12, 1) USING round(fee_settlement_per_tx::numeric, 1),
  ALTER COLUMN chargeback_fee_per_tx TYPE NUMERIC(12, 1) USING round(chargeback_fee_per_tx::numeric, 1);
