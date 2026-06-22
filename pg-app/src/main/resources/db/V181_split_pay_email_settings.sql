-- 분할결제 회차 안내 메일 템플릿 (D-1 ~ D+3, 다국어) — PostgreSQL·MySQL 공통
ALTER TABLE tb_split_pay_contract
    ADD COLUMN IF NOT EXISTS customer_locale VARCHAR(8) NULL DEFAULT 'KOR';

ALTER TABLE tb_split_pay_installment
    ADD COLUMN IF NOT EXISTS mail_d3_sent TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS tb_split_pay_email_phase (
    phase VARCHAR(16) NOT NULL PRIMARY KEY,
    mail_from_address VARCHAR(255) NULL,
    mail_from_name VARCHAR(200) NULL,
    alert_recipient_emails TEXT NULL,
    test_recipient_email VARCHAR(255) NULL,
    subject_kor VARCHAR(500) NULL,
    body_kor TEXT NULL,
    subject_eng VARCHAR(500) NULL,
    body_eng TEXT NULL,
    subject_jpn VARCHAR(500) NULL,
    body_jpn TEXT NULL,
    subject_chn VARCHAR(500) NULL,
    body_chn TEXT NULL,
    subject_tha VARCHAR(500) NULL,
    body_tha TEXT NULL,
    updated_at TIMESTAMP NULL
);

INSERT INTO tb_split_pay_email_phase (phase, updated_at)
SELECT v.phase, NOW()
FROM (
    SELECT 'D_MINUS1' AS phase UNION ALL
    SELECT 'D0' UNION ALL
    SELECT 'D1' UNION ALL
    SELECT 'D2' UNION ALL
    SELECT 'D3'
) v
WHERE NOT EXISTS (SELECT 1 FROM tb_split_pay_email_phase p WHERE p.phase = v.phase);
