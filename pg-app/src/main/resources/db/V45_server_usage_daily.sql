-- 일별 트래픽(누적 바이트 델타 합)·일 메모리 피크(%) — NOTI 시스템 모니터와 유사 시계열
CREATE TABLE IF NOT EXISTS tb_server_usage_daily (
    usage_date DATE NOT NULL PRIMARY KEY,
    traffic_bytes BIGINT NOT NULL DEFAULT 0,
    memory_peak_pct DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tb_server_usage_state (
    id SMALLINT NOT NULL PRIMARY KEY,
    last_net_total_bytes BIGINT,
    updated_at TIMESTAMP
);

INSERT INTO tb_server_usage_state (id, last_net_total_bytes, updated_at)
VALUES (1, NULL, NULL)
ON CONFLICT (id) DO NOTHING;
