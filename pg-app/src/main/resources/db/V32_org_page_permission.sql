-- 조직(OrgLevel)별 화면(URL) 접근 권한: NONE / OBSERVER / MODIFY / DELETE
CREATE TABLE IF NOT EXISTS tb_org_page_permission (
    id              BIGSERIAL PRIMARY KEY,
    org_level       VARCHAR(32) NOT NULL,
    page_url        VARCHAR(256) NOT NULL,
    menu_id         VARCHAR(32),
    permission      VARCHAR(16) NOT NULL,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_org_page_perm UNIQUE (org_level, page_url)
);
CREATE INDEX IF NOT EXISTS idx_org_page_perm_org ON tb_org_page_permission(org_level);
