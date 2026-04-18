-- 본사 기본: 미수금 환수 모드(AUTO=차기 정산 FIFO, MANUAL=환수처리 후 차감). 신규 가맹 정산설정 초기값·정산관리설정 화면 저장 시 선택적 전체 동기화에 사용.
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS receivable_recovery_default_mode VARCHAR(16);

UPDATE tb_hq_ledger_sys_settings
SET receivable_recovery_default_mode = 'AUTO'
WHERE receivable_recovery_default_mode IS NULL OR TRIM(receivable_recovery_default_mode) = '';

UPDATE tb_hq_ledger_sys_settings
SET receivable_recovery_default_mode = 'AUTO'
WHERE UPPER(TRIM(receivable_recovery_default_mode)) NOT IN ('AUTO', 'MANUAL');

ALTER TABLE tb_hq_ledger_sys_settings
    ALTER COLUMN receivable_recovery_default_mode SET DEFAULT 'AUTO';

ALTER TABLE tb_hq_ledger_sys_settings
    ALTER COLUMN receivable_recovery_default_mode SET NOT NULL;

COMMENT ON COLUMN tb_hq_ledger_sys_settings.receivable_recovery_default_mode IS '미수금 환수 기본: AUTO 차기정산 FIFO, MANUAL 미수금관리 환수처리 후 차감.';
