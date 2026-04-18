-- 미수금 환수: 총판(MASTER_DIST) 기본 + 가맹(MERCHANT) 개별 오버라이드(Y일 때만 가맹 receivable_recovery_mode 우선)
ALTER TABLE tb_settlement_setting
    ADD COLUMN IF NOT EXISTS receivable_recovery_override_yn VARCHAR(1);

UPDATE tb_settlement_setting ss
SET receivable_recovery_override_yn = 'Y'
FROM tb_org_unit ou
WHERE ou.id = ss.org_unit_id
  AND ou.org_level = 'MERCHANT'
  AND (ss.receivable_recovery_override_yn IS NULL OR TRIM(ss.receivable_recovery_override_yn) = '');

UPDATE tb_settlement_setting ss
SET receivable_recovery_override_yn = 'N'
FROM tb_org_unit ou
WHERE ou.id = ss.org_unit_id
  AND ou.org_level = 'MASTER_DIST'
  AND (ss.receivable_recovery_override_yn IS NULL OR TRIM(ss.receivable_recovery_override_yn) = '');

UPDATE tb_settlement_setting
SET receivable_recovery_override_yn = 'N'
WHERE receivable_recovery_override_yn IS NULL OR TRIM(receivable_recovery_override_yn) = '';

UPDATE tb_settlement_setting
SET receivable_recovery_override_yn = 'N'
WHERE UPPER(TRIM(receivable_recovery_override_yn)) NOT IN ('Y', 'N');

ALTER TABLE tb_settlement_setting
    ALTER COLUMN receivable_recovery_override_yn SET DEFAULT 'N';

ALTER TABLE tb_settlement_setting
    ALTER COLUMN receivable_recovery_override_yn SET NOT NULL;

COMMENT ON COLUMN tb_settlement_setting.receivable_recovery_override_yn IS '미수금 환수: Y=가맹 receivable_recovery_mode 우선, N=상위 총판(또는 본사 기본) 따름.';
