-- ICOPAY 카드정책 사전차단(비활성카드·쿨다운)으로 결제내역에 잘못 남은 실패 건 정리
-- JPAY/ChillPay pay_index·승인번호·transaction_id 에 도달하지 않은 ICOPAY 선행 차단만 대상
-- (2026-06 이후 앱 로직: 동일 유형은 신규 적재하지 않음)
DELETE FROM pg_trnsctn t
WHERE t.status = '99'
  AND COALESCE(t.settled_yn, 'N') = 'N'
  AND t.paid_at IS NULL
  AND UPPER(TRIM(COALESCE(t.outcome_reason_source, ''))) = 'ICOPAY'
  AND (t.approval_no IS NULL OR BTRIM(t.approval_no) = '')
  AND (t.chill_transaction_id IS NULL OR BTRIM(t.chill_transaction_id) = '')
  AND (
    UPPER(TRIM(COALESCE(t.outcome_reason_code, ''))) IN (
      'INACTIVE_CARD',
      'BLACKLIST',
      'CARD_COOLDOWN',
      'CARD_COOLDOWN_TIER_1',
      'CARD_COOLDOWN_TIER_2',
      'CARD_COOLDOWN_TIER_3',
      'CARD_COOLDOWN_TIER_4'
    )
    OR (
      (t.outcome_reason_code IS NULL OR BTRIM(t.outcome_reason_code) = '')
      AND t.outcome_reason IS NOT NULL
      AND (
        t.outcome_reason LIKE '%고위험 거래로 인해 거부%'
        OR t.outcome_reason LIKE '%high-risk policy%'
        OR t.outcome_reason LIKE '%高リスク取引%'
        OR t.outcome_reason LIKE '%高风险交易%'
        OR t.outcome_reason LIKE '%ความเสี่ยงสูง%'
        OR t.outcome_reason LIKE '%잠시 사용할 수 없습니다%'
        OR t.outcome_reason LIKE '%temporarily unavailable%'
        OR t.outcome_reason LIKE '%一時的にご利用いただけません%'
        OR t.outcome_reason LIKE '%暂时无法使用%'
        OR t.outcome_reason LIKE '%차 결제 실패 경고%'
        OR t.outcome_reason LIKE '%payment failure warning%'
        OR t.outcome_reason LIKE '%決済失敗警告%'
        OR t.outcome_reason LIKE '%支付失败警告%'
      )
    )
  );
