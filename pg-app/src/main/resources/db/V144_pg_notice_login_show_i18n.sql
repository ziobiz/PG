-- 로그인 첫 화면 공지: 노출 여부 + AI 다국어 캐시(JSON)
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS show_on_login VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE pg_notice ADD COLUMN IF NOT EXISTS login_i18n_json TEXT;
