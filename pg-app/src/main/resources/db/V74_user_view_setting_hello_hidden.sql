-- VIEW SETTING 헬로 패널(열 가이드) 숨김 여부 — 사용자·페이지별 저장

ALTER TABLE tb_user_view_setting
    ADD COLUMN IF NOT EXISTS hello_panel_hidden_yn VARCHAR(1) NOT NULL DEFAULT 'N';

COMMENT ON COLUMN tb_user_view_setting.hello_panel_hidden_yn IS 'Y: 헬로(VIEW SETTING 패널) 숨김 기본';
