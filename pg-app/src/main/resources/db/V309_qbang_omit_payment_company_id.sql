-- V309: QBANG — paymentCompanyId 제거(큐뱅 회신: 세팅 불필요, PayPay=Alipay+ 중계)
SET client_min_messages TO WARNING;

UPDATE tb_pg_agency
SET credentials_extra_json = (COALESCE(credentials_extra_json::jsonb, '{}'::jsonb) - 'paymentCompanyId')
WHERE UPPER(TRIM(pg_cd)) LIKE 'QBANG%'
  AND credentials_extra_json IS NOT NULL
  AND credentials_extra_json::jsonb ? 'paymentCompanyId';
