-- 공지 노출 유형 확장: 로그인 후 팝업(show_post_login_popup), 메인공지(show_on_main)
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS show_post_login_popup CHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS show_on_main CHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN pg_notice.show_as_popup IS 'Y: 로그인 페이지 접속팝업(총본사, 동시 1건)';
COMMENT ON COLUMN pg_notice.show_on_login IS 'Y: 로그인 페이지 첫화면(총본사, 동시 1건)';
COMMENT ON COLUMN pg_notice.show_post_login_popup IS 'Y: 로그인 완료 후 팝업(동시 1건, 배포대상별 노출)';
COMMENT ON COLUMN pg_notice.show_on_main IS 'Y: 메인 대시보드 공지(동시 1건, 배포대상별 노출)';
