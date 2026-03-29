-- VIEW SETTING 노출 허용: 본사(REGIONAL) / 총판(MASTER_DIST) / 지사·대리점·영업점(BRANCH_GROUP) / 가맹점(MERCHANT) 구분
-- 기존 행은 본사용으로 두고, 동일 JSON을 총판·지사그룹·가맹점에 복제해 기존과 동일하게 전 조직에 적용되게 함.

ALTER TABLE tb_org_view_column_allowance ADD COLUMN IF NOT EXISTS viewer_scope VARCHAR(32) DEFAULT 'REGIONAL' NOT NULL;

ALTER TABLE tb_org_view_column_allowance DROP CONSTRAINT uk_org_view_col_allow;

INSERT INTO tb_org_view_column_allowance (regional_org_code, page_url, allowed_keys_json, updated_at, viewer_scope)
SELECT regional_org_code, page_url, allowed_keys_json, updated_at, 'MASTER_DIST' FROM tb_org_view_column_allowance WHERE viewer_scope = 'REGIONAL';

INSERT INTO tb_org_view_column_allowance (regional_org_code, page_url, allowed_keys_json, updated_at, viewer_scope)
SELECT regional_org_code, page_url, allowed_keys_json, updated_at, 'BRANCH_GROUP' FROM tb_org_view_column_allowance WHERE viewer_scope = 'REGIONAL';

INSERT INTO tb_org_view_column_allowance (regional_org_code, page_url, allowed_keys_json, updated_at, viewer_scope)
SELECT regional_org_code, page_url, allowed_keys_json, updated_at, 'MERCHANT' FROM tb_org_view_column_allowance WHERE viewer_scope = 'REGIONAL';

ALTER TABLE tb_org_view_column_allowance ADD CONSTRAINT uk_org_view_col_allow_scope UNIQUE (regional_org_code, page_url, viewer_scope);
