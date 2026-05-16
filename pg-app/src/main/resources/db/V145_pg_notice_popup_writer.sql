-- 공지: 로그인 팝업 노출 + 작성자 표시명
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS show_as_popup VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS writer_nm VARCHAR(100);
