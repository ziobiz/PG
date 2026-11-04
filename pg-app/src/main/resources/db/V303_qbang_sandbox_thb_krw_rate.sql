-- V303: QBANG DEV 샌드박스 THB→KRW 폴백 환율 (BOT 불가 서버용)
SET client_min_messages TO WARNING;

UPDATE tb_pg_agency
SET credentials_extra_json = '{"posId":"00000000-2026-1103-0004-006700912345","agencyCode":"OTL","env":"DEV","partner":"온더라인","sandboxThbToKrw":40}'
WHERE UPPER(TRIM(pg_cd)) = 'QBANG SANDBOX';
