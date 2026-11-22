-- PAYSO 오성공·잔류 요청 정정 (가맹 6000000057)
-- FS1261122133429RFKVFG: 복귀 폴링 실패 후 웹훅이 저장 pay_no 만으로 10 처리 → PAYSO 포털 무거래
-- FS1261122133400FSP8IO: 요청(08) 잔류

UPDATE pg_trnsctn
SET status = '99',
    paid_at = NULL,
    outcome_reason = 'False paid corrected: buyer-return fail then unpaid webhook used stored pay_no',
    outcome_reason_code = 'PAYSO_FALSE_PAID_CORRECTED',
    outcome_reason_source = 'ICOPAY',
    outcome_reason_at = NOW()
WHERE trn_id = 'PS9c242ead08b549d282'
  AND merchant_id = '6000000057'
  AND status = '10';

UPDATE pg_trnsctn
SET status = '99',
    paid_at = NULL,
    outcome_reason = 'Pending expired — no PAYSO completion',
    outcome_reason_code = 'PAYSO_PENDING_EXPIRED',
    outcome_reason_source = 'ICOPAY',
    outcome_reason_at = NOW()
WHERE trn_id = 'PSa6f2195189a04db19b'
  AND merchant_id = '6000000057'
  AND status = '08';
