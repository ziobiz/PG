-- 본사 정산 자동 배치: Y/N 컬럼 → ACTIVE / INACTIVE / AUTO 모드
-- ACTIVE: 스케줄 tick 항상 본문 실행(① JVM AND).
-- INACTIVE: 본사 DB에서 배치 본문 비활성.
-- AUTO: 이번 tick 에 실행할 AUTO 가맹(주기·시각 조건)이 있을 때만 본문 실행.

ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS settlement_auto_batch_mode VARCHAR(16);

UPDATE tb_hq_ledger_sys_settings
SET settlement_auto_batch_mode = CASE
    WHEN TRIM(settlement_auto_batch_enabled_yn::text) = 'Y' THEN 'ACTIVE'
    ELSE 'INACTIVE'
END
WHERE settlement_auto_batch_mode IS NULL OR TRIM(settlement_auto_batch_mode) = '';

UPDATE tb_hq_ledger_sys_settings
SET settlement_auto_batch_mode = 'INACTIVE'
WHERE UPPER(TRIM(settlement_auto_batch_mode)) NOT IN ('ACTIVE', 'INACTIVE', 'AUTO');

ALTER TABLE tb_hq_ledger_sys_settings
    ALTER COLUMN settlement_auto_batch_mode SET DEFAULT 'INACTIVE';

ALTER TABLE tb_hq_ledger_sys_settings
    ALTER COLUMN settlement_auto_batch_mode SET NOT NULL;

COMMENT ON COLUMN tb_hq_ledger_sys_settings.settlement_auto_batch_mode IS '스케줄 tick: ACTIVE 항상, INACTIVE 끔, AUTO 이번 틱에 대상 AUTO 가맹이 있을 때만. RT 건별 정산과 무관.';

ALTER TABLE tb_hq_ledger_sys_settings
    DROP COLUMN IF EXISTS settlement_auto_batch_enabled_yn;
