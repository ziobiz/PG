-- 고객 거래명세서: 환불·무효 메일 발송 시각 (승인 메일 receipt_mail_sent_at 과 별도)

ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS receipt_followup_mail_sent_at TIMESTAMP;

COMMENT ON COLUMN pg_trnsctn.receipt_followup_mail_sent_at IS '고객 거래명세서(환불·무효) 이메일 발송 시각';
