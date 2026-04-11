-- 노티 수령 로그: 최초 수신(LIVE) vs 재전송(RETRY) 구분 (HTTP 헤더 기반, 미표시 시 UNKNOWN)
ALTER TABLE tb_pg_notify_inbound ADD COLUMN IF NOT EXISTS ingress_delivery_kind VARCHAR(16);

COMMENT ON COLUMN tb_pg_notify_inbound.ingress_delivery_kind IS 'LIVE=실시간 최초, RETRY=재전송, UNKNOWN=헤더 없음·미표시';
