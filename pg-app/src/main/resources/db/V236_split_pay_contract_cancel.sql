-- 분할결제 계약 취소 권한(본사정책·가맹) + 계약 취소 감사 컬럼

ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS split_pay_contract_cancel_default_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS split_pay_contract_cancel_org_op_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_hq_api_config.split_pay_contract_cancel_default_yn IS
    'URL 분할결제 계약취소 — 가맹 FOLLOW_HQ 시 기본 부여(Y/N). 기본 N';
COMMENT ON COLUMN tb_hq_api_config.split_pay_contract_cancel_org_op_yn IS
    'URL 분할결제 계약취소 — 본사(REGIONAL)·총판(MASTER_DIST) 운영 권한(Y/N). 기본 N. 총본사는 항상 가능';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS split_pay_contract_cancel_yn VARCHAR(16) NOT NULL DEFAULT 'FOLLOW_HQ';

COMMENT ON COLUMN tb_merchant_profile.split_pay_contract_cancel_yn IS
    '분할 계약취소 권한: FOLLOW_HQ | Y | N. FOLLOW_HQ면 본사 기본값';

ALTER TABLE tb_split_pay_contract
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);
ALTER TABLE tb_split_pay_contract
    ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(100);

COMMENT ON COLUMN tb_split_pay_contract.cancel_reason IS '운영 계약취소 사유';
COMMENT ON COLUMN tb_split_pay_contract.cancelled_by IS '계약취소 처리자(로그인 ID)';
