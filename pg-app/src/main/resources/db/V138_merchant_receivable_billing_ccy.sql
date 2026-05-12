-- 미수금 행에 청구 통화(표시용). 챗봇 플랜·업그레이드 등.

ALTER TABLE tb_merchant_receivable
    ADD COLUMN IF NOT EXISTS billing_ccy VARCHAR(3);

COMMENT ON COLUMN tb_merchant_receivable.billing_ccy IS
    '청구 통화 ISO(표시·집계 보조). 금액 단위는 total_amount와 동일 스케일';
