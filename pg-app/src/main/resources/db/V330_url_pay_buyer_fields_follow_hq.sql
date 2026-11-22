-- 결제창 구매자입력: 주문번호 노출 FOLLOW_HQ + 본사 기본값
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS url_pay_order_no_show_default_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_hq_api_config.url_pay_order_no_show_default_yn IS
    '결제창 주문번호 행 노출 본사 기본 Y/N (가맹 FOLLOW_HQ 시 적용, 기본 N)';

ALTER TABLE tb_merchant_profile
    ALTER COLUMN url_pay_order_no_show_yn TYPE VARCHAR(16);

COMMENT ON COLUMN tb_merchant_profile.url_pay_order_no_show_yn IS
    '결제창 주문번호 행 노출 FOLLOW_HQ|Y|N (기본 FOLLOW_HQ, 백단 orderNo는 유지)';

-- 신규 기본만 FOLLOW_HQ. 기존 Y/N 값은 유지(가맹 명시 설정).
ALTER TABLE tb_merchant_profile
    ALTER COLUMN url_pay_order_no_show_yn SET DEFAULT 'FOLLOW_HQ';
