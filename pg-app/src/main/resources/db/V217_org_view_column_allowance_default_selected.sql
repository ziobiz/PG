-- 조직항목설정: 배포된 항목(allowed)과 최초 접속 기본 선택 항목 분리 + 전체 목록 순서
ALTER TABLE tb_org_view_column_allowance
    ADD COLUMN IF NOT EXISTS default_selected_keys_json TEXT;

ALTER TABLE tb_org_view_column_allowance
    ADD COLUMN IF NOT EXISTS column_order_keys_json TEXT;

COMMENT ON COLUMN tb_org_view_column_allowance.allowed_keys_json IS
    '배포된 항목(해당 조직 VIEW SETTING에 노출 가능한 열 키, 순서 유지)';
COMMENT ON COLUMN tb_org_view_column_allowance.default_selected_keys_json IS
    '선택된 항목(최초 접속 시 기본 ON, allowed_keys_json 부분집합)';
COMMENT ON COLUMN tb_org_view_column_allowance.column_order_keys_json IS
    '조직항목설정 UI 전체 행 순서(배포 여부와 무관)';
