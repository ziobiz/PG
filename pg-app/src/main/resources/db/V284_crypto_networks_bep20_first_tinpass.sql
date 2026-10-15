-- V284: TINPASS(6000000064) 허용 네트워크 순서를 BEP20 우선으로.
-- 원인: networks_csv 가 BASE,BEP20 이면 prepare 시 network 미지정 시 BASE를 고르고,
--       본사 수탁은 USDT/USDC×BEP20 만 사용Y → CUSTODY_MISSING.
-- 코드(수탁 있는 망 자동선택)와 병행. 운영 UI에서도 BEP20이 먼저 보이게 맞춤.

UPDATE tb_merchant_crypto_config c
SET networks_csv = 'BEP20,BASE'
FROM tb_org_unit ou
WHERE c.org_unit_id = ou.id
  AND ou.code = '6000000064'
  AND (
    c.networks_csv IS NULL
    OR TRIM(c.networks_csv) = ''
    OR UPPER(REPLACE(c.networks_csv, ' ', '')) = 'BASE,BEP20'
    OR UPPER(REPLACE(c.networks_csv, ' ', '')) LIKE 'BASE,%'
  );
