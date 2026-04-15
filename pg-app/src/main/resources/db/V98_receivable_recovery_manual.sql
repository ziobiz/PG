-- 미수금 환수: 가맹별 자동(AUTO, 기본) / 수동(MANUAL, 「환수처리」 후 다음 정산 마감 시 차감)
ALTER TABLE tb_settlement_setting ADD COLUMN IF NOT EXISTS receivable_recovery_mode VARCHAR(16) NOT NULL DEFAULT 'AUTO';

-- 정산 실행 행: 해당 실행에서 미수금 FIFO로 차감된 합계(가맹점정산내역 표시)
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS receivable_applied_amt NUMERIC(21, 8);

-- 수동 환수 요청(가맹이 MANUAL일 때만 정산에서 적용)
CREATE TABLE IF NOT EXISTS tb_merchant_receivable_recovery_req (
    id                          BIGSERIAL PRIMARY KEY,
    merchant_receivable_id      BIGINT NOT NULL,
    merchant_id                 VARCHAR(50) NOT NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at                TIMESTAMP WITHOUT TIME ZONE,
    requested_by                VARCHAR(100),
    applied_settlement_run_id   BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mrr_req_one_pending_per_recv
    ON tb_merchant_receivable_recovery_req (merchant_receivable_id)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_mrr_req_merchant_status
    ON tb_merchant_receivable_recovery_req (merchant_id, status);
