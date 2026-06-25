-- JPAY 통합조회: 포털 Export 결과 캐시(DB) — 서버 재시작·로그아웃 후에도 목록 조회 가능
CREATE TABLE IF NOT EXISTS tb_jpay_portal_export_cache (
    cache_key          VARCHAR(32)  NOT NULL PRIMARY KEY DEFAULT 'DEFAULT',
    synced_at          TIMESTAMP,
    last_sync_message  TEXT,
    export_from        DATE,
    export_to          DATE,
    rows_json          TEXT         NOT NULL DEFAULT '[]',
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE tb_jpay_portal_export_cache IS 'JPAY 포털 Export 동기화 결과 — 통합조회(/calc/jpayTrList) 목록 캐시';
COMMENT ON COLUMN tb_jpay_portal_export_cache.rows_json IS 'enrichRows 결과 JSON 배열 — 메모리 캐시와 동일 구조';
