-- 본사설정 사용자설정: 담당자(ASSISTANT) 역할별 메뉴 기본 권한(JSON). 개별 조직 담당자 오버라이드가 있으면 그쪽이 우선.

ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS assistant_role_default_matrix_json TEXT;
