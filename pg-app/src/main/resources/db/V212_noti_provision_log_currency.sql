-- 노티생성 이력 — 기준화폐(자동 슬롯 JPY j200 / USD j55 구간)
ALTER TABLE tb_noti_provision_log
    ADD COLUMN IF NOT EXISTS base_currency VARCHAR(8);
