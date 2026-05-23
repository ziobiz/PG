-- 정산 지급부족(AUTO_SETTLEMENT_DEFICIT) 미수금 중복 정리
-- 원인: 동일 정산 슬롯(가맹·period_from·period_to·calc_dt)마다 정산 실행이 반복되며
--       memo=AUTO_DEFICIT:{run_id} 건만 달라 중복 등록됨.
-- 슬롯별 최소 id 1건만 유지하고 나머지 PENDING·PARTIAL 건은 CANCELLED 처리.

WITH recv_run AS (
    SELECT
        r.id AS recv_id,
        r.merchant_id,
        sr.period_from,
        sr.period_to,
        sr.calc_dt,
        ROW_NUMBER() OVER (
            PARTITION BY r.merchant_id, sr.period_from, sr.period_to, sr.calc_dt
            ORDER BY r.id ASC
        ) AS rn
    FROM tb_merchant_receivable r
    INNER JOIN tb_settlement_run sr
        ON r.memo = 'AUTO_DEFICIT:' || sr.id::text
    WHERE r.reason_code = 'AUTO_SETTLEMENT_DEFICIT'
      AND UPPER(TRIM(r.status)) IN ('PENDING', 'PARTIAL')
)
UPDATE tb_merchant_receivable r
SET status = 'CANCELLED',
    remaining_amount = 0
FROM recv_run d
WHERE r.id = d.recv_id
  AND d.rn > 1;

-- 유지 건 memo를 슬롯 키로 통일(신규 코드와 동일 형식)
UPDATE tb_merchant_receivable r
SET memo = 'AUTO_DEFICIT_SLOT:' || TRIM(r.merchant_id) || '|'
         || COALESCE(sr.period_from::text, sr.calc_dt::text) || '|'
         || COALESCE(sr.period_to::text, sr.calc_dt::text) || '|'
         || sr.calc_dt::text
FROM tb_settlement_run sr
WHERE r.reason_code = 'AUTO_SETTLEMENT_DEFICIT'
  AND r.memo = 'AUTO_DEFICIT:' || sr.id::text
  AND UPPER(TRIM(r.status)) IN ('PENDING', 'PARTIAL', 'CLOSED', 'WRITE_OFF')
  AND r.memo NOT LIKE 'AUTO_DEFICIT_SLOT:%';
