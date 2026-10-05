-- 거래명세서 Acquirer / Payment Switcher 노출 활성(Y)/비활성(N)
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS acquirer_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_pg_agency ADD COLUMN IF NOT EXISTS payment_switcher_use_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_pg_agency.acquirer_use_yn IS '거래명세서 카드매입사(Acquirer) 노출 Y=활성 N=비활성';
COMMENT ON COLUMN tb_pg_agency.payment_switcher_use_yn IS '거래명세서 결제중계사(Payment Switcher) 노출 Y=활성 N=비활성';

-- 기존에 명칭이 있으면 활성으로 이관(기존 노출 동작 유지)
UPDATE tb_pg_agency
SET acquirer_use_yn = 'Y'
WHERE acquirer_use_yn = 'N'
  AND acquirer_nm IS NOT NULL
  AND TRIM(acquirer_nm) <> '';

UPDATE tb_pg_agency
SET payment_switcher_use_yn = 'Y'
WHERE payment_switcher_use_yn = 'N'
  AND payment_switcher_nm IS NOT NULL
  AND TRIM(payment_switcher_nm) <> '';
