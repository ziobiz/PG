-- V336: PAYSO 후속조치(자동무효·자동환불·강제환불) 활성화
-- Payso Web API Void(/payment/gateway/v2/payment/voidpayment) 연동. 정산 전 취소만 지원.
SET client_min_messages TO WARNING;

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
