-- 조직 단위(tb_org_unit)별 화면 권한 오버라이드 + 단계 기본/개별 모드
-- PostgreSQL: 한 번에 NOT NULL + DEFAULT. (로컬 H2 dev는 application-dev ddl-auto로 반영; 엔티티는 H2 호환 위해 nullable 매핑)
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS page_permission_mode VARCHAR(20) NOT NULL DEFAULT 'LEVEL_DEFAULT';

CREATE TABLE IF NOT EXISTS tb_org_unit_page_permission (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT NOT NULL,
    page_url        VARCHAR(256) NOT NULL,
    menu_id         VARCHAR(32),
    permission      VARCHAR(16) NOT NULL,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_org_unit_page_perm UNIQUE (org_unit_id, page_url),
    CONSTRAINT fk_org_unit_page_perm_ou FOREIGN KEY (org_unit_id) REFERENCES tb_org_unit(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_org_unit_page_perm_ou ON tb_org_unit_page_permission(org_unit_id);
