-- JPAY 동기화 — 당일 동기화 횟수(통합조회·조회통합·통합체크 요약 표시)
ALTER TABLE tb_jpay_portal_export_cache
    ADD COLUMN IF NOT EXISTS sync_count_date DATE,
    ADD COLUMN IF NOT EXISTS sync_count_today INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_jpay_portal_export_cache.sync_count_date IS 'sync_count_today 집계 기준일(전산 타임존)';
COMMENT ON COLUMN tb_jpay_portal_export_cache.sync_count_today IS 'sync_count_date 당일 누적 동기화 완료 횟수';
