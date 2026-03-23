-- 잔액/미수금관리 수동 차감 이력
CREATE TABLE IF NOT EXISTS tb_balance_deduction (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    memo VARCHAR(300),
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT now()
);
