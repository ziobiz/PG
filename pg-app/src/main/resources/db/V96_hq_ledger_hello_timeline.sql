-- 헬로 타임라인: 전역 동기(분 단위 자동 비활성) vs 페이지별(기존 동작)
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS hello_timeline_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'N';
ALTER TABLE tb_hq_ledger_sys_settings ADD COLUMN IF NOT EXISTS hello_timeline_duration_min INTEGER NOT NULL DEFAULT 10;
