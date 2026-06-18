-- 접속팝업·첫화면(show_as_popup/show_on_login)은 총본사 작성 공지만 — 총판·본사 등 잘못 지정된 행 정리
UPDATE pg_notice n
SET show_as_popup = 'N'
FROM tb_org_unit o
WHERE n.org_unit_id = o.id
  AND o.org_level <> 'HEADQUARTERS'
  AND n.show_as_popup = 'Y';

UPDATE pg_notice n
SET show_on_login = 'N'
FROM tb_org_unit o
WHERE n.org_unit_id = o.id
  AND o.org_level <> 'HEADQUARTERS'
  AND n.show_on_login = 'Y';

UPDATE pg_notice SET show_as_popup = 'N' WHERE org_unit_id IS NULL AND show_as_popup = 'Y';
UPDATE pg_notice SET show_on_login = 'N' WHERE org_unit_id IS NULL AND show_on_login = 'Y';
