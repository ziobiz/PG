-- 수동무효(이메일무효): ChillPay 등에 보낼 요청 메일 수신처·제목·본문 템플릿 (전산설정관리)
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS email_void_to VARCHAR(255);
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS email_void_subject VARCHAR(500);
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS email_void_body_template TEXT;
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS email_void_company_name VARCHAR(200);
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS email_void_contact_name VARCHAR(200);
