-- 정산결과(배포/대기) 게이트: 가맹점정산내역·유통정산·확정리포트는 DISTRIBUTED 만 포함
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS settlement_publish_sts VARCHAR(20);

UPDATE tb_settlement_run
SET settlement_publish_sts = CASE
                                   WHEN COALESCE(TRIM(payout_hold_yn), 'N') = 'Y' THEN 'HOLD'
                                   ELSE 'DISTRIBUTED'
                                   END
WHERE settlement_publish_sts IS NULL;

ALTER TABLE tb_settlement_run ALTER COLUMN settlement_publish_sts SET DEFAULT 'PENDING';
ALTER TABLE tb_settlement_run ALTER COLUMN settlement_publish_sts SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_settlement_run_publish_calc ON tb_settlement_run (settlement_publish_sts, calc_dt);
