-- 담당자 권한그룹(MANAGER/OPERATOR/SETTLEMENT/TECH)별 메뉴 권한 — 조직 최종 권한(상한) 이내에서만 적용

CREATE TABLE IF NOT EXISTS tb_org_unit_assistant_page_permission (
    id BIGSERIAL PRIMARY KEY,
    org_unit_id BIGINT NOT NULL,
    assistant_role_type VARCHAR(32) NOT NULL,
    page_url VARCHAR(256) NOT NULL,
    menu_id VARCHAR(32),
    permission VARCHAR(16) NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_org_unit_assist_page UNIQUE (org_unit_id, assistant_role_type, page_url)
);

CREATE INDEX IF NOT EXISTS idx_org_unit_assist_page_org ON tb_org_unit_assistant_page_permission(org_unit_id);
