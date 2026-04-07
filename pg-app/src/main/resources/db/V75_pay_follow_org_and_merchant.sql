-- 결제 후속조치(자동무효·이메일무효·자동환불·강제환불): 조직 단계별 상한 + 가맹점별 세부

CREATE TABLE IF NOT EXISTS tb_org_level_pay_follow_cap (
    org_level VARCHAR(20) PRIMARY KEY,
    auto_void_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    email_void_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    auto_refund_yn VARCHAR(1) NOT NULL DEFAULT 'Y',
    force_refund_yn VARCHAR(1) NOT NULL DEFAULT 'Y'
);

INSERT INTO tb_org_level_pay_follow_cap (org_level, auto_void_yn, email_void_yn, auto_refund_yn, force_refund_yn) VALUES
    ('HEADQUARTERS', 'Y', 'Y', 'Y', 'Y'),
    ('REGIONAL', 'Y', 'Y', 'Y', 'Y'),
    ('MASTER_DIST', 'Y', 'Y', 'Y', 'Y'),
    ('BRANCH', 'Y', 'Y', 'Y', 'Y'),
    ('AGENCY', 'Y', 'Y', 'Y', 'Y'),
    ('SALES_OFFICE', 'Y', 'Y', 'Y', 'Y'),
    ('MERCHANT', 'Y', 'Y', 'Y', 'Y')
ON CONFLICT (org_level) DO NOTHING;

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS pay_follow_merchant_use_yn VARCHAR(1);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS pay_follow_auto_void_yn VARCHAR(1);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS pay_follow_email_void_yn VARCHAR(1);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS pay_follow_auto_refund_yn VARCHAR(1);
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS pay_follow_force_refund_yn VARCHAR(1);

COMMENT ON TABLE tb_org_level_pay_follow_cap IS '조직 단계별 결제 후속조치 API 허용 상한(본사권한설정)';
COMMENT ON COLUMN tb_merchant_profile.pay_follow_merchant_use_yn IS '가맹점 관리자 후속조치 사용 Y/N, NULL=기존호환(허용)';
COMMENT ON COLUMN tb_merchant_profile.pay_follow_auto_void_yn IS 'NULL=상한 내 전체 허용(호환)';
