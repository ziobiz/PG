-- 조직별 관리자 웹 태블릿 UI(헤더 전환·운영모드 메뉴) 사용 여부
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS tablet_feature_use_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
