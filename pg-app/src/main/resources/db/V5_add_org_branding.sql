-- ============================================================
-- 본사/총판 브랜딩 설정 (2026-03)
-- 메인이미지, 로고이미지, 배경테마
-- ============================================================

CREATE TABLE IF NOT EXISTS tb_org_branding (
    id BIGSERIAL PRIMARY KEY,
    org_unit_id BIGINT NOT NULL UNIQUE,
    main_image_url VARCHAR(500),
    logo_image_url VARCHAR(500),
    theme VARCHAR(20) DEFAULT 'DEFAULT',
    updated_at TIMESTAMP,
    CONSTRAINT fk_org_branding_org_unit FOREIGN KEY (org_unit_id) REFERENCES tb_org_unit(id)
);

CREATE INDEX IF NOT EXISTS idx_org_branding_org_unit_id ON tb_org_branding(org_unit_id);
