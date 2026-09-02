-- 본사·총판 도메인 설정: 브라우저 <title> (설정 이름·브랜딩 siteName 과 별도)
ALTER TABLE tb_org_unit ADD COLUMN IF NOT EXISTS domain_page_title VARCHAR(200);
