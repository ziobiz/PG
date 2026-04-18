-- V104 가 운영 DB에 일부만 적용된 경우 등: tb_commission_policy 무효·환불·강제환불 정산 모드 컬럼 보강 (PostgreSQL IF NOT EXISTS)
-- Hibernate validate(tb_commission_policy.force_refund_settlement_mode 등) 실패 시 이 스크립트를 운영 DB에 실행.

ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS void_settlement_mode VARCHAR(16);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS manual_void_settlement_mode VARCHAR(16);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS refund_settlement_mode VARCHAR(16);
ALTER TABLE tb_commission_policy ADD COLUMN IF NOT EXISTS force_refund_settlement_mode VARCHAR(16);
