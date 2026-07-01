-- 노티생성 이력 — NOTI merchantId
ALTER TABLE tb_noti_provision_log
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

UPDATE tb_noti_provision_log
SET merchant_id = comp_id
WHERE merchant_id IS NULL OR TRIM(merchant_id) = '';
