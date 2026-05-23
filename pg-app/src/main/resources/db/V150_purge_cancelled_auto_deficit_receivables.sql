-- 미수금관리 중복·취소 잔여분 물리 삭제 (V148 CANCELLED 처리 후에도 목록에 2338건 노출되던 문제)
-- + 슬롯·레거시 memo 기준 중복 잔존분 정리

-- 1) 연결된 환수처리 요청(대기) — 삭제 대상 미수금에 묶인 것만 취소
UPDATE tb_merchant_receivable_recovery_request req
SET status = 'CANCELLED'
WHERE req.status = 'PENDING'
  AND req.merchant_receivable_id IN (
      SELECT r.id
      FROM tb_merchant_receivable r
      WHERE r.reason_code = 'AUTO_SETTLEMENT_DEFICIT'
        AND UPPER(TRIM(r.status)) = 'CANCELLED'
  );

-- 2) 취소된 지급부족 자동미수 전부 삭제(목록 노이즈 제거)
DELETE FROM tb_merchant_receivable
WHERE reason_code = 'AUTO_SETTLEMENT_DEFICIT'
  AND UPPER(TRIM(status)) = 'CANCELLED';

-- 3) 슬롯 memo 중복 — 가맹·memo당 최소 id 1건만 유지
WITH slot_dup AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY lower(trim(merchant_id)), memo
               ORDER BY id ASC
           ) AS rn
    FROM tb_merchant_receivable
    WHERE reason_code = 'AUTO_SETTLEMENT_DEFICIT'
      AND memo LIKE 'AUTO_DEFICIT_SLOT:%'
      AND UPPER(TRIM(status)) NOT IN ('CANCELLED', 'WRITE_OFF')
)
DELETE FROM tb_merchant_receivable
WHERE id IN (SELECT id FROM slot_dup WHERE rn > 1);

-- 4) 레거시 AUTO_DEFICIT:{runId} — 가맹·금액·제목 기준 중복(실행 삭제 후 join 불가분)
WITH legacy_dup AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY lower(trim(merchant_id)), total_amount, COALESCE(title, ''), reason_code
               ORDER BY id ASC
           ) AS rn
    FROM tb_merchant_receivable
    WHERE reason_code = 'AUTO_SETTLEMENT_DEFICIT'
      AND memo LIKE 'AUTO_DEFICIT:%'
      AND memo NOT LIKE 'AUTO_DEFICIT_SLOT:%'
      AND UPPER(TRIM(status)) NOT IN ('CANCELLED', 'WRITE_OFF')
)
DELETE FROM tb_merchant_receivable
WHERE id IN (SELECT id FROM legacy_dup WHERE rn > 1);

-- 5) 유지 건 memo 슬롯 형식 통일(실행 행이 남아 있는 경우)
UPDATE tb_merchant_receivable r
SET memo = 'AUTO_DEFICIT_SLOT:' || TRIM(r.merchant_id) || '|'
         || COALESCE(sr.period_from::text, sr.calc_dt::text) || '|'
         || COALESCE(sr.period_to::text, sr.calc_dt::text) || '|'
         || sr.calc_dt::text
FROM tb_settlement_run sr
WHERE r.reason_code = 'AUTO_SETTLEMENT_DEFICIT'
  AND r.memo = 'AUTO_DEFICIT:' || sr.id::text
  AND UPPER(TRIM(r.status)) NOT IN ('CANCELLED', 'WRITE_OFF')
  AND r.memo NOT LIKE 'AUTO_DEFICIT_SLOT:%';
