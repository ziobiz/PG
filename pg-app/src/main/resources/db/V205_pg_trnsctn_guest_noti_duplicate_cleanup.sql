-- 동일 가맹·주문번호에 NOTI·guest 유령 행과 URL/API·실고객 행이 공존하는 중복 제거
-- (ICOPAY 노티 미들웨어 선행 적재 + URL/JPAY sale 후속 적재 등)
DELETE FROM pg_trnsctn g
WHERE UPPER(TRIM(COALESCE(g.origin, ''))) = 'NOTI'
  AND LOWER(TRIM(COALESCE(g.customer_id, ''))) = 'guest'
  AND g.order_no IS NOT NULL
  AND BTRIM(g.order_no) <> ''
  AND g.merchant_id IS NOT NULL
  AND BTRIM(g.merchant_id) <> ''
  AND EXISTS (
    SELECT 1
    FROM pg_trnsctn k
    WHERE k.merchant_id = g.merchant_id
      AND k.order_no = g.order_no
      AND k.trn_id <> g.trn_id
      AND NOT (
        UPPER(TRIM(COALESCE(k.origin, ''))) = 'NOTI'
        AND LOWER(TRIM(COALESCE(k.customer_id, ''))) = 'guest'
      )
  );
