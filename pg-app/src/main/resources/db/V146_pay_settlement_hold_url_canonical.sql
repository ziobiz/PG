-- 정산보류내역: 구 메뉴 URL /settlement/paySettlementHoldList → /calc/paySettlementHoldList
-- (앱은 구 URL 접근 시 /calc 로 리다이렉트·권한 별칭 처리)

-- tb_org_page_permission
UPDATE tb_org_page_permission SET page_url = '/calc/paySettlementHoldList'
WHERE page_url = '/settlement/paySettlementHoldList'
  AND NOT EXISTS (
    SELECT 1 FROM tb_org_page_permission p2
    WHERE p2.org_level = tb_org_page_permission.org_level
      AND p2.page_url = '/calc/paySettlementHoldList'
  );
DELETE FROM tb_org_page_permission WHERE page_url = '/settlement/paySettlementHoldList';

-- tb_org_unit_page_permission
UPDATE tb_org_unit_page_permission SET page_url = '/calc/paySettlementHoldList'
WHERE page_url = '/settlement/paySettlementHoldList'
  AND NOT EXISTS (
    SELECT 1 FROM tb_org_unit_page_permission p2
    WHERE p2.org_unit_id = tb_org_unit_page_permission.org_unit_id
      AND p2.page_url = '/calc/paySettlementHoldList'
  );
DELETE FROM tb_org_unit_page_permission WHERE page_url = '/settlement/paySettlementHoldList';

-- tb_org_unit_assistant_page_permission
UPDATE tb_org_unit_assistant_page_permission SET page_url = '/calc/paySettlementHoldList'
WHERE page_url = '/settlement/paySettlementHoldList'
  AND NOT EXISTS (
    SELECT 1 FROM tb_org_unit_assistant_page_permission p2
    WHERE p2.org_unit_id = tb_org_unit_assistant_page_permission.org_unit_id
      AND p2.assistant_role_type = tb_org_unit_assistant_page_permission.assistant_role_type
      AND p2.page_url = '/calc/paySettlementHoldList'
  );
DELETE FROM tb_org_unit_assistant_page_permission WHERE page_url = '/settlement/paySettlementHoldList';

-- tb_org_tablet_menu
UPDATE tb_org_tablet_menu SET page_url = '/calc/paySettlementHoldList'
WHERE page_url = '/settlement/paySettlementHoldList'
  AND NOT EXISTS (
    SELECT 1 FROM tb_org_tablet_menu m2
    WHERE m2.org_level = tb_org_tablet_menu.org_level
      AND m2.page_url = '/calc/paySettlementHoldList'
  );
DELETE FROM tb_org_tablet_menu WHERE page_url = '/settlement/paySettlementHoldList';

-- tb_user_view_setting
UPDATE tb_user_view_setting SET page_url = '/calc/paySettlementHoldList'
WHERE page_url = '/settlement/paySettlementHoldList'
  AND NOT EXISTS (
    SELECT 1 FROM tb_user_view_setting u2
    WHERE u2.username = tb_user_view_setting.username
      AND u2.page_url = '/calc/paySettlementHoldList'
  );
DELETE FROM tb_user_view_setting WHERE page_url = '/settlement/paySettlementHoldList';

-- tb_org_view_column_allowance
UPDATE tb_org_view_column_allowance SET page_url = '/calc/paySettlementHoldList'
WHERE page_url = '/settlement/paySettlementHoldList'
  AND NOT EXISTS (
    SELECT 1 FROM tb_org_view_column_allowance a2
    WHERE a2.regional_org_code = tb_org_view_column_allowance.regional_org_code
      AND a2.viewer_scope = tb_org_view_column_allowance.viewer_scope
      AND a2.page_url = '/calc/paySettlementHoldList'
  );
DELETE FROM tb_org_view_column_allowance WHERE page_url = '/settlement/paySettlementHoldList';

-- tb_hq_view_custom_column
UPDATE tb_hq_view_custom_column SET page_url = '/calc/paySettlementHoldList'
WHERE page_url = '/settlement/paySettlementHoldList'
  AND NOT EXISTS (
    SELECT 1 FROM tb_hq_view_custom_column h2
    WHERE h2.page_url = '/calc/paySettlementHoldList'
      AND h2.column_key = tb_hq_view_custom_column.column_key
  );
DELETE FROM tb_hq_view_custom_column WHERE page_url = '/settlement/paySettlementHoldList';
