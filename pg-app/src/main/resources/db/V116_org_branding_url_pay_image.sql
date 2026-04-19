-- URL 결제(checkout) 상단 전용 이미지 (총판·본사·총본사 브랜딩). 미설정 시 기존 로고(로그인 후 로고)로 폴백.
ALTER TABLE tb_org_branding ADD COLUMN IF NOT EXISTS url_pay_image_url VARCHAR(500);
COMMENT ON COLUMN tb_org_branding.url_pay_image_url IS 'URL결제 페이지 상단 이미지 URL (미입력 시 logo_image_url 사용)';
