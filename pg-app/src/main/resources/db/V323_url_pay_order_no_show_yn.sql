-- URL/리다이렉트 결제창 「주문번호」행 노출 — 가맹 설정, 기본 비활성(N). 백단 orderNo 생성·저장은 유지.

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS url_pay_order_no_show_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_merchant_profile.url_pay_order_no_show_yn IS '결제창 주문번호 행 노출 Y/N (기본 N, 백단 orderNo는 유지)';

UPDATE tb_merchant_profile
SET url_pay_order_no_show_yn = 'N'
WHERE url_pay_order_no_show_yn IS NULL OR TRIM(url_pay_order_no_show_yn) = '';
