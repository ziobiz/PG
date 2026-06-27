-- JPAY 00:00 기본 동기화(2일) 마지막 수행 기준일
ALTER TABLE tb_jpay_portal_export_cache
    ADD COLUMN IF NOT EXISTS last_basic_sync_date DATE;

COMMENT ON COLUMN tb_jpay_portal_export_cache.last_basic_sync_date IS
    'JPAY 매일 00:00 기본 동기화(어제·오늘 2일) 마지막 수행 기준일(전산 타임존)';
