-- 정산관리 중복 데이터 정리 + 재발 방지 유니크 인덱스
-- 대상: tb_settlement_run(가맹점정산·유통망·정산보류), tb_rolling_reserve(담보금), tb_merchant_receivable(미수금 슬롯)

-- 1) 달력형·주간 정산 실행 중복 (period_end_at IS NULL) — 슬롯별 최소 id 유지
WITH calendar_dup AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY lower(trim(merchant_id)), period_from, period_to, calc_dt
               ORDER BY id ASC
           ) AS rn
    FROM tb_settlement_run
    WHERE period_end_at IS NULL
)
DELETE FROM tb_settlement_run
WHERE id IN (SELECT id FROM calendar_dup WHERE rn > 1);

-- 2) 격자·담보해지 전용 실행 중복 (period_end_at IS NOT NULL)
WITH grid_dup AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY lower(trim(merchant_id)), calc_dt, period_end_at
               ORDER BY id ASC
           ) AS rn
    FROM tb_settlement_run
    WHERE period_end_at IS NOT NULL
)
DELETE FROM tb_settlement_run
WHERE id IN (SELECT id FROM grid_dup WHERE rn > 1);

-- 3) 담보금 HOLD 중복 (동일 trn_id)
WITH hold_dup AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY trn_id
               ORDER BY id ASC
           ) AS rn
    FROM tb_rolling_reserve
    WHERE UPPER(TRIM(status)) = 'HOLD'
)
DELETE FROM tb_rolling_reserve
WHERE id IN (SELECT id FROM hold_dup WHERE rn > 1);

-- 4) 환수금: DB 유니크(uk_settlement_recovery_trn_reason) 위반 잔존분 정리
WITH recovery_dup AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY trn_id, reason_code
               ORDER BY id ASC
           ) AS rn
    FROM tb_settlement_recovery
)
DELETE FROM tb_settlement_recovery
WHERE id IN (SELECT id FROM recovery_dup WHERE rn > 1);

-- 5) 재발 방지 — 달력형 정산 슬롯
CREATE UNIQUE INDEX IF NOT EXISTS uk_settlement_run_calendar_slot
    ON tb_settlement_run (lower(trim(merchant_id)), period_from, period_to, calc_dt)
    WHERE period_end_at IS NULL;

-- 6) 재발 방지 — 격자·period_end_at 슬롯
CREATE UNIQUE INDEX IF NOT EXISTS uk_settlement_run_grid_slot
    ON tb_settlement_run (lower(trim(merchant_id)), calc_dt, period_end_at)
    WHERE period_end_at IS NOT NULL;

-- 7) 재발 방지 — 담보 HOLD 1건/trn
CREATE UNIQUE INDEX IF NOT EXISTS uk_rolling_reserve_trn_hold
    ON tb_rolling_reserve (trn_id)
    WHERE UPPER(TRIM(status)) = 'HOLD';

-- 8) 재발 방지 — 지급부족 자동미수 슬롯 memo (V148 정규화 후)
CREATE UNIQUE INDEX IF NOT EXISTS uk_merchant_receivable_auto_deficit_slot
    ON tb_merchant_receivable (lower(trim(merchant_id)), memo)
    WHERE reason_code = 'AUTO_SETTLEMENT_DEFICIT'
      AND memo LIKE 'AUTO_DEFICIT_SLOT:%';
