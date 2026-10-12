-- V271: 크립토 보증금(담보) 비율 · 출금 큐 보증금 금액
SET client_min_messages TO WARNING;

ALTER TABLE tb_merchant_crypto_config
    ADD COLUMN IF NOT EXISTS reserve_pct NUMERIC(10, 4) NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_merchant_crypto_config.reserve_pct IS
    '보증금(담보) 비율 %. USD 입금금액 기준. 0=미적용.';

COMMENT ON COLUMN tb_merchant_crypto_config.hold_days IS
    '보증금·지연정산 보류일수. SCHEDULED 정산 시 출금 대기일.';

ALTER TABLE tb_crypto_payout_queue
    ADD COLUMN IF NOT EXISTS amount_reserve NUMERIC(24, 8) NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_crypto_payout_queue.amount_reserve IS
    '보증금(담보) 차감액. USD 액면.';
