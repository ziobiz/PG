-- V305: 크립토 대표수탁 ReplaceMe 시드 비활성 — 구매자 입금에 임시 주소 노출 금지
-- 실제 ERC20 등 수탁은 본사정책 → 결제·URL → 크립토설정 → 대표수탁지갑에서 등록.

UPDATE tb_crypto_custody_address
SET use_yn = 'N',
    primary_yn = 'N',
    label = CASE
      WHEN COALESCE(label, '') = '' THEN 'DISABLED placeholder (ReplaceMe)'
      WHEN label ILIKE '%replace%' THEN label
      ELSE label || ' [DISABLED placeholder]'
    END
WHERE use_yn = 'Y'
  AND (
    UPPER(TRIM(address)) LIKE '%REPLACEME%'
    OR UPPER(TRIM(address)) LIKE '0XICOPAY%'
    OR UPPER(TRIM(address)) LIKE 'TICOPAYCUSTODY%'
  );
