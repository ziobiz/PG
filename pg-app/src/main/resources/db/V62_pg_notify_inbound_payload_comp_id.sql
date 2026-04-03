-- URL 결제(1:N) 노티: 업체코드(compId) 분기용 원문 추적
ALTER TABLE tb_pg_notify_inbound ADD COLUMN IF NOT EXISTS payload_comp_id VARCHAR(64);
