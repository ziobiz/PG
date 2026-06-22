-- URL 분할결제 — 멀티(고객 선택 개월) 모드

ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_interval_multi_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_merchant_profile ADD COLUMN IF NOT EXISTS split_pay_multi_max_months INTEGER NOT NULL DEFAULT 6;

COMMENT ON COLUMN tb_merchant_profile.split_pay_interval_multi_yn IS '분할결제 멀티 모드(Y): 고객이 1~최대개월 중 기간 선택';
COMMENT ON COLUMN tb_merchant_profile.split_pay_multi_max_months IS '멀티 모드 최대 선택 개월(3·5·6·12)';
