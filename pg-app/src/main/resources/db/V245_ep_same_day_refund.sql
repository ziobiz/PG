-- URL 결제(ElementPay) 당일환불: 전역 스위치 + 조직 단계별 허용(자동환불과 별도)

ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS ep_same_day_refund_yn VARCHAR(1) DEFAULT 'N';

UPDATE tb_hq_notify_env_config
SET ep_same_day_refund_yn = 'N'
WHERE ep_same_day_refund_yn IS NULL;

COMMENT ON COLUMN tb_hq_notify_env_config.ep_same_day_refund_yn IS
    'URL 결제 당일환불 전역 사용 Y/N. 환불처리(initRefund)와 동일. 태국 결제일 당일만. 단계별 same_day_refund_yn 과 AND';

ALTER TABLE tb_org_level_pay_follow_cap
    ADD COLUMN IF NOT EXISTS same_day_refund_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_org_level_pay_follow_cap.same_day_refund_yn IS
    '조직 단계별 URL 결제 당일환불 허용. 자동환불 허용과 별도. 기본 N';
