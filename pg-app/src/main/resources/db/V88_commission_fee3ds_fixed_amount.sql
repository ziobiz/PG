-- 3DS 수수료: 승인금액 대비 %가 아니라 정책통화 기준 건당 고정액(tb_commission_policy.fee_3ds_rate 값은 레거시 컬럼명 유지)
ALTER TABLE tb_commission_policy
    ALTER COLUMN fee_3ds_rate TYPE NUMERIC(12, 1);

COMMENT ON COLUMN tb_commission_policy.fee_3ds_rate IS '3DS 등 추가 인증 건당 고정 수수료(정책 통화 단위, 소수 첫째 자리)';
