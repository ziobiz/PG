-- JPAY 스케줄 동기화 — 당일 스케줄 실행 횟수(1회=어제·오늘, 2회~=당일 판단용)
ALTER TABLE tb_jpay_portal_export_cache
    ADD COLUMN IF NOT EXISTS schedule_sync_count_date DATE,
    ADD COLUMN IF NOT EXISTS schedule_sync_count_today INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_jpay_portal_export_cache.schedule_sync_count_date IS 'schedule_sync_count_today 집계 기준일(전산 타임존)';
COMMENT ON COLUMN tb_jpay_portal_export_cache.schedule_sync_count_today IS 'schedule_sync_count_date 당일 스케줄(SCHEDULED) 동기화 완료 횟수';
