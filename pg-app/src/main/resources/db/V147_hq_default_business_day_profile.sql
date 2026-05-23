-- 총본사 메인 영업일 달력·정산 기본에 쓰는 영업일 설정 프로필 ID (tb_hq_api_config.business_day_settings_json 내 id)
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS hq_default_business_day_profile_id VARCHAR(64);
