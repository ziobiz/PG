-- NOTI 유령 오승인(10) 정정 — 승인번호·PG TransactionId·카드 BIN 없이 status=10 만 반영된 건
-- (ICOPAY 아웃바운드 pg.payment.status 재유입·칠페이 노티매핑 오인 등)
-- 이미 정상 승인(승인번호·transaction_id·카드 BIN 보유) 건은 제외됩니다.
UPDATE pg_trnsctn
SET status = '21',
    paid_at = NULL,
    chill_payment_status = '21',
    outcome_reason = '불안전한 파라미터 정보 오류',
    outcome_reason_code = 'INCOMPLETE_PARAMS',
    outcome_reason_source = 'ICOPAY',
    outcome_reason_at = CURRENT_TIMESTAMP
WHERE status = '10'
  AND UPPER(TRIM(COALESCE(origin, ''))) = 'NOTI'
  AND (chill_transaction_id IS NULL OR BTRIM(chill_transaction_id) = '')
  AND (approval_no IS NULL OR BTRIM(approval_no) = '')
  AND (card_pan_display IS NULL OR BTRIM(card_pan_display) = '');
