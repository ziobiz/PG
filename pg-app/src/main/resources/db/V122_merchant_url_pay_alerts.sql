-- URL·챗봇 인라인 결제(DirectCredit) 승인 시 LINE Notify·대표 이메일 알림
ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_alert_email_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_line_notify_token VARCHAR(256);

UPDATE tb_merchant_profile
SET url_pay_alert_email_yn = 'N'
WHERE url_pay_alert_email_yn IS NULL;
