-- 결제대행사별 ICOPAY 후속조치 허용 + 기존 계열 기본값(현재 하드 규칙과 동일)
-- 노티 미들웨어(integ_noti_yn)는 거래 적재만 담당. 후속조치는 이 컬럼 AND 전산설정 AND 조직단계 AND 가맹 스위치.

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS pay_follow_auto_void_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS pay_follow_email_void_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS pay_follow_manual_void_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS pay_follow_auto_refund_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS pay_follow_manual_refund_yn VARCHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS pay_follow_force_refund_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_pg_agency
    ADD COLUMN IF NOT EXISTS pay_follow_same_day_refund_yn VARCHAR(1) NOT NULL DEFAULT 'N';

UPDATE tb_pg_agency
SET pay_follow_auto_void_yn = CASE WHEN UPPER(TRIM(pg_cd)) LIKE 'CHILLPAY%' THEN 'Y' ELSE 'N' END,
    pay_follow_email_void_yn = CASE WHEN UPPER(TRIM(pg_cd)) LIKE 'CHILLPAY%' THEN 'Y' ELSE 'N' END,
    pay_follow_manual_void_yn = CASE WHEN UPPER(TRIM(pg_cd)) LIKE 'JPAY%' THEN 'Y' ELSE 'N' END,
    pay_follow_auto_refund_yn = 'Y',
    pay_follow_manual_refund_yn = CASE WHEN UPPER(TRIM(pg_cd)) LIKE 'JPAY%' THEN 'Y' ELSE 'N' END,
    pay_follow_force_refund_yn = 'Y',
    pay_follow_same_day_refund_yn = CASE WHEN UPPER(TRIM(pg_cd)) LIKE 'ELEMENTPAY%' THEN 'Y' ELSE 'N' END;

COMMENT ON COLUMN tb_pg_agency.pay_follow_auto_void_yn IS
    'ICOPAY 무효처리 노출 허용 Y/N. 계열 API 가능(ChillPay)과 AND. 노티 적재와 별개';

COMMENT ON COLUMN tb_pg_agency.pay_follow_email_void_yn IS
    'ICOPAY 이메일 무효 노출 허용 Y/N. ChillPay만 실사용';

COMMENT ON COLUMN tb_pg_agency.pay_follow_manual_void_yn IS
    'ICOPAY 수동무효 노출 허용 Y/N. JPAY만 실사용';

COMMENT ON COLUMN tb_pg_agency.pay_follow_auto_refund_yn IS
    'ICOPAY 환불처리 노출 허용 Y/N';

COMMENT ON COLUMN tb_pg_agency.pay_follow_manual_refund_yn IS
    'ICOPAY 수동환불 노출 허용 Y/N. JPAY만 실사용';

COMMENT ON COLUMN tb_pg_agency.pay_follow_force_refund_yn IS
    'ICOPAY 강제환불 노출 허용 Y/N';

COMMENT ON COLUMN tb_pg_agency.pay_follow_same_day_refund_yn IS
    'ICOPAY 당일환불 창 허용 Y/N. ElementPay + 전역 당일환불과 AND';
