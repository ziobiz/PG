-- V337: 결제대행사별 후속조치 시간 경계(NULL=전산설정 전역 따름)
SET client_min_messages TO WARNING;

ALTER TABLE tb_pg_agency
  ADD COLUMN IF NOT EXISTS pf_void_start_min INTEGER,
  ADD COLUMN IF NOT EXISTS pf_void_end_min INTEGER,
  ADD COLUMN IF NOT EXISTS pf_refund_after_days INTEGER,
  ADD COLUMN IF NOT EXISTS pf_force_after_days INTEGER,
  ADD COLUMN IF NOT EXISTS pf_refund_window_start_min INTEGER;

COMMENT ON COLUMN tb_pg_agency.pf_void_start_min IS '후속 무효: 당일 시작(분). NULL=전역 auto_void_start_min';
COMMENT ON COLUMN tb_pg_agency.pf_void_end_min IS '후속 무효: 당일 마감(분). NULL=전역 auto_void_end_min';
COMMENT ON COLUMN tb_pg_agency.pf_refund_after_days IS '후속 환불: 정산전 일수. NULL=전역 auto_refund_after_days';
COMMENT ON COLUMN tb_pg_agency.pf_force_after_days IS '후속 강제환불: 정산후 일수. NULL=전역 force_refund_after_days';
COMMENT ON COLUMN tb_pg_agency.pf_refund_window_start_min IS '후속 환불: 익일 시작(분). NULL=전역 auto_refund_window_start_min';

-- PAYSO: 무효·환불·강제환불 허용(강제=수동 반영). 이메일·수동JPAY 무효는 N
UPDATE tb_pg_agency
SET
  pay_follow_auto_void_yn = 'Y',
  pay_follow_email_void_yn = 'N',
  pay_follow_manual_void_yn = 'N',
  pay_follow_auto_refund_yn = 'Y',
  pay_follow_manual_refund_yn = 'N',
  pay_follow_force_refund_yn = 'Y',
  pay_follow_same_day_refund_yn = COALESCE(NULLIF(TRIM(pay_follow_same_day_refund_yn), ''), 'N')
WHERE UPPER(REPLACE(TRIM(pg_cd), ' ', '_')) LIKE 'PAYSO%'
   OR UPPER(TRIM(pg_cd)) LIKE 'PAYSOLUTIONS%';

-- KSNET: 무효(당일)·환불·강제환불 — 모두 cancel API. 이메일무효 없음
UPDATE tb_pg_agency
SET
  pay_follow_auto_void_yn = 'Y',
  pay_follow_email_void_yn = 'N',
  pay_follow_manual_void_yn = 'N',
  pay_follow_auto_refund_yn = 'Y',
  pay_follow_manual_refund_yn = 'N',
  pay_follow_force_refund_yn = 'Y'
WHERE UPPER(REPLACE(TRIM(pg_cd), ' ', '_')) LIKE 'KSNET%'
   OR UPPER(TRIM(pg_cd)) LIKE 'KSPAY%';
