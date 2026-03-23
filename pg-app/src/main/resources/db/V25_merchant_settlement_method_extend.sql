-- 가맹점 정산방법 확장: 정산구분(calc_proc_type), 정산최소금액(calc_min_amt), 이체시간(transfer_exec_time)
-- transfer_type 컬럼 의미 변경: 이체및송금구분(MANUAL / AUTO / NONE). 기존 FUMBANKING 값은 calc_proc_type 으로 이관.

ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS calc_proc_type VARCHAR(20);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS calc_min_amt NUMERIC(18,0);
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS transfer_exec_time TIME;

-- 레거시 transfer_type(정산+이체 혼합) → 정산구분
UPDATE tb_settlement_setting SET calc_proc_type = CASE UPPER(COALESCE(transfer_type, ''))
    WHEN 'FUMBANKING' THEN 'FUMBANKING'
    WHEN 'AUTO' THEN 'AUTO'
    ELSE 'MANUAL'
END;

-- 이체및송금구분: 펌뱅킹 정산이면 이체는 자동 연동, 자동 정산이면 이체도 자동, 그 외 수동
UPDATE tb_settlement_setting SET transfer_type = CASE UPPER(COALESCE(calc_proc_type, 'MANUAL'))
    WHEN 'FUMBANKING' THEN 'AUTO'
    WHEN 'AUTO' THEN 'AUTO'
    ELSE 'MANUAL'
END;
