-- JPAY UNPAID 건 오승인(10) 정정 — 승인번호 365675016778 (2026-06-25)
-- JPAY 포털 Unpaid·노티 미수신 → ICOPAY 임시 취소(20, UNPAID)
UPDATE pg_trnsctn
SET status = '20',
    paid_at = NULL,
    chill_payment_status = '08',
    outcome_reason = '결제 미완료(UNPAID, 노티 미수신, 임시 취소)',
    outcome_reason_code = 'UNPAID',
    outcome_reason_source = 'JPAY',
    outcome_reason_at = CURRENT_TIMESTAMP
WHERE chill_transaction_id = '365675016778'
  AND status = '10';
