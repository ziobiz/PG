-- 조직 단계별 태블릿 모드에서 노출할 메뉴(URL) — 본사설정 「운영모드관리」에서 편집
-- PostgreSQL (기본 datasource). MySQL 전용 문법(AUTO_INCREMENT 등) 사용하지 않음.
CREATE TABLE IF NOT EXISTS tb_org_tablet_menu (
    id         BIGSERIAL PRIMARY KEY,
    org_level  VARCHAR(32) NOT NULL,
    page_url   VARCHAR(256) NOT NULL,
    use_yn     VARCHAR(1) NOT NULL DEFAULT 'N',
    updated_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT uk_org_tablet_menu_level_url UNIQUE (org_level, page_url)
);
