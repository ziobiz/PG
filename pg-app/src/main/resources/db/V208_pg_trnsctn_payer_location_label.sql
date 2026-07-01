-- 결제개요 위치 — JPAY 스타일 영문 라벨 (예: Japan-Chiba Prefecture). 목록 조회 시 GeoIP 재조회 없음.
ALTER TABLE pg_trnsctn ADD COLUMN IF NOT EXISTS payer_location_label VARCHAR(256);
