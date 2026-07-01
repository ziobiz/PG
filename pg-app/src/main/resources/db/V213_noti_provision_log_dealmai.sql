-- 노티생성 이력 — DEALMAI Partner 코드
ALTER TABLE tb_noti_provision_log
    ADD COLUMN IF NOT EXISTS dealmai_partner_code VARCHAR(64);
