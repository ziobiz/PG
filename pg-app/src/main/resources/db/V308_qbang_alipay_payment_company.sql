-- V308: QBANG DEV — QR paymentCompanyId=Alipay (위챗 기본 QR 방지)
SET client_min_messages TO WARNING;

UPDATE tb_pg_agency
SET credentials_extra_json = COALESCE(credentials_extra_json::jsonb, '{}'::jsonb)
  || '{"paymentCompanyId":"Alipay"}'::jsonb
WHERE UPPER(TRIM(pg_cd)) LIKE 'QBANG%';
