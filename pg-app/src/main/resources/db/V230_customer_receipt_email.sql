-- 고객 거래명세서 이메일: PG 매입사/중계사, 정책, 결제창 언어

ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS acquirer_nm VARCHAR(200);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS acquirer_tel VARCHAR(50);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS acquirer_email VARCHAR(255);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS payment_switcher_nm VARCHAR(200);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS payment_switcher_tel VARCHAR(50);
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS payment_switcher_email VARCHAR(255);

ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS receipt_email_default_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS receipt_email_enabled_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS receipt_email_follow_hq_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS receipt_email_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS checkout_lang VARCHAR(8);
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS receipt_mail_sent_at TIMESTAMP;

COMMENT ON COLUMN tb_pg_agency.acquirer_nm IS '거래명세서 — 카드매입사(Acquirer) 명칭';
COMMENT ON COLUMN tb_pg_agency.payment_switcher_nm IS '거래명세서 — 결제중계사(Payment Switcher) 명칭';
COMMENT ON COLUMN tb_hq_ledger_sys_settings.receipt_email_default_yn IS '본사 기본: 고객 거래명세서 이메일 Y/N';
COMMENT ON COLUMN tb_settlement_setting.receipt_email_enabled_yn IS '총판(MASTER_DIST) 하위 가맹 기본: 고객 거래명세서 이메일 Y/N';
COMMENT ON COLUMN tb_merchant_profile.receipt_email_follow_hq_yn IS 'Y=상위(총판·본사) 정책 따름, N=receipt_email_use_yn 직접';
COMMENT ON COLUMN pg_trnsctn.checkout_lang IS '결제창 언어 KOR/ENG/JPN/CHN/THA';
