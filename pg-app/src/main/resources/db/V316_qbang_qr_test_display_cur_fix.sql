-- 기존 QBANG QR 테스트: 실결제=입력금액인데 display_cur_type 만 THB(가맹 기준화폐)로 남은 건 → KRW 로 정정
-- (이미 FX 환전된 건: display_amt ≠ amt_krw 이면 display THB/JPY 유지)
UPDATE pg_trnsctn
SET display_cur_type = 'KRW'
WHERE UPPER(COALESCE(van, '')) LIKE 'QBANG%'
  AND UPPER(COALESCE(payment_channel, '')) = 'QR'
  AND UPPER(COALESCE(cur_type, '')) = 'KRW'
  AND UPPER(COALESCE(display_cur_type, '')) = 'THB'
  AND display_amt IS NOT NULL
  AND amt_krw IS NOT NULL
  AND display_amt = amt_krw
  AND created_at >= TIMESTAMP '2026-11-05';
