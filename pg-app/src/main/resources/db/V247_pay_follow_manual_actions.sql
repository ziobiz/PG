-- 결제 후속조치: 수동무효·수동환불 전역·단계별·가맹점 독립 스위치 (기존 auto_void/auto_refund 와 분리)

ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS manual_void_yn VARCHAR(1);

ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS manual_refund_yn VARCHAR(1);

UPDATE tb_hq_notify_env_config
SET manual_void_yn = auto_void_yn
WHERE manual_void_yn IS NULL;

UPDATE tb_hq_notify_env_config
SET manual_refund_yn = auto_refund_yn
WHERE manual_refund_yn IS NULL;

UPDATE tb_hq_notify_env_config
SET manual_void_yn = 'N'
WHERE manual_void_yn IS NULL;

UPDATE tb_hq_notify_env_config
SET manual_refund_yn = 'N'
WHERE manual_refund_yn IS NULL;

COMMENT ON COLUMN tb_hq_notify_env_config.manual_void_yn IS
    '수동무효(JPAY) 전역 사용 Y/N. 무효처리(auto_void)와 별도';

COMMENT ON COLUMN tb_hq_notify_env_config.manual_refund_yn IS
    '수동환불(JPAY) 전역 사용 Y/N. 환불처리(auto_refund)와 별도';

ALTER TABLE tb_org_level_pay_follow_cap
    ADD COLUMN IF NOT EXISTS manual_void_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_org_level_pay_follow_cap
    ADD COLUMN IF NOT EXISTS manual_refund_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_org_level_pay_follow_cap.manual_void_yn IS
    '조직 단계별 수동무효(JPAY) 허용. 무효처리 허용과 별도';

COMMENT ON COLUMN tb_org_level_pay_follow_cap.manual_refund_yn IS
    '조직 단계별 수동환불(JPAY) 허용. 환불처리 허용과 별도';

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS pay_follow_manual_void_yn VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS pay_follow_manual_refund_yn VARCHAR(1);

COMMENT ON COLUMN tb_merchant_profile.pay_follow_manual_void_yn IS
    '가맹 수동무효(JPAY) Y/N. NULL=상한 내 허용(호환)';

COMMENT ON COLUMN tb_merchant_profile.pay_follow_manual_refund_yn IS
    '가맹 수동환불(JPAY) Y/N. NULL=상한 내 허용(호환)';
