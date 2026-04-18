-- 총판별 정산 스케줄(격자·T0 등) 시각 기준 Zone — 영업일 프로필(본사 영업일설정)과 별도
ALTER TABLE tb_master_dist_settlement_cycle_config
    ADD COLUMN IF NOT EXISTS settlement_cron_zone_id VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul';

COMMENT ON COLUMN tb_master_dist_settlement_cycle_config.settlement_cron_zone_id IS '정산 크론·일중 격자·T0 당일 기준 IANA ZoneId (예: Asia/Bangkok, Asia/Tokyo). 영업일 프로필과 독립.';
