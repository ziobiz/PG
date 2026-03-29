-- 서버관리 실시간 대시보드 자동 갱신 간격(초). NULL이면 application.yml 기본값 사용
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS server_manage_ui_refresh_sec INTEGER;
