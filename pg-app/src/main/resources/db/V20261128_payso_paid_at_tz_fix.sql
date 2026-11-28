-- PAYSO paid_at 시간대 오저장 보정
-- 1) orderDateTime 의 Z 를 UTC 로 읽어 방콕 변환(+7h)
-- 2) JVM(KST) LocalDateTime.now() 를 표준(TH) naive 로 저장(+2h)
-- created_at 과의 시차로 구분. 이미 정상인 건(차이 애매)은 건드리지 않음.

UPDATE pg_trnsctn t
SET paid_at = CASE
  WHEN t.paid_at - t.created_at >= INTERVAL '4 hours'
   AND t.paid_at - t.created_at <= INTERVAL '8 hours'
    THEN t.paid_at - INTERVAL '7 hours'
  WHEN ABS(EXTRACT(EPOCH FROM (t.paid_at - t.created_at))) <= 1800
    THEN t.paid_at - INTERVAL '2 hours'
  ELSE t.paid_at
END
WHERE UPPER(TRIM(COALESCE(t.van, ''))) LIKE 'PAYSO%'
  AND t.status IN ('10', '00')
  AND t.paid_at IS NOT NULL
  AND t.created_at IS NOT NULL;
