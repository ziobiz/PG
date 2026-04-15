-- =============================================================================
-- V89까지만 적용된 PostgreSQL DB용 후속 패치 (한 파일로 실행)
--   - 정산 기간 컬럼(V92), 환수금·미수금 테이블(V93), 지급보류 적치(V94)
--   - 모두 IF NOT EXISTS / ADD COLUMN IF NOT EXISTS 이라 재실행해도 안전합니다.
-- 적용 예:
--   psql "postgresql://USER:PASS@HOST:5432/DBNAME" -f patch_after_V89_postgresql.sql
-- 또는 JAR에서:
--   unzip -p pg-app-0.0.1-SNAPSHOT.jar BOOT-INF/classes/db/patch_after_V89_postgresql.sql | psql ...
-- =============================================================================

-- V92: 정산 실행 행 — 집계 기간(가맹점정산내역·수수료내역과 동일 거래 창 재현용)
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS period_from DATE;
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS period_to DATE;
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS period_end_at TIMESTAMP WITHOUT TIME ZONE;

-- V93: 환수금(정산 후 환불 자동)·미수금(수동) — 다음 정산 지급액 FIFO 차감
CREATE TABLE IF NOT EXISTS tb_settlement_recovery (
    id                          BIGSERIAL PRIMARY KEY,
    merchant_id                 VARCHAR(50) NOT NULL,
    trn_id                      VARCHAR(20) NOT NULL,
    recall_amount               BIGINT NOT NULL,
    remaining_amount            BIGINT NOT NULL,
    applied_amount              BIGINT NOT NULL DEFAULT 0,
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason_code                 VARCHAR(40) NOT NULL,
    fee_included_yn             VARCHAR(1),
    vat_applied_yn              VARCHAR(1),
    last_applied_settlement_run_id BIGINT,
    memo                        VARCHAR(500),
    created_at                  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_settlement_recovery_trn_reason UNIQUE (trn_id, reason_code)
);
CREATE INDEX IF NOT EXISTS idx_settlement_recovery_merchant_status ON tb_settlement_recovery (merchant_id, status);

CREATE TABLE IF NOT EXISTS tb_merchant_receivable (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         VARCHAR(50) NOT NULL,
    title               VARCHAR(200),
    total_amount        BIGINT NOT NULL,
    remaining_amount    BIGINT NOT NULL,
    applied_amount      BIGINT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason_code         VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    memo                TEXT,
    created_by          VARCHAR(100),
    created_at          TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_merchant_receivable_merchant_status ON tb_merchant_receivable (merchant_id, status);

-- V94: 지급보류(Y) 가맹점 — 정산 실행 행을 정산보류내역에 적치(가맹점정산내역·유통망 집계 제외), 해제 시 반영
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS payout_hold_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS payout_hold_remark VARCHAR(800);
CREATE INDEX IF NOT EXISTS idx_settlement_run_payout_hold ON tb_settlement_run (payout_hold_yn, calc_dt);

-- 정산 실행 행 — 실행 시점 정산주기(가맹 설정 변경 후에도 과거 행 표시용)
ALTER TABLE tb_settlement_run ADD COLUMN IF NOT EXISTS calc_cycle_snapshot VARCHAR(64);
