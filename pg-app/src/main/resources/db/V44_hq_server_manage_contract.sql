-- 서버관리: 호스팅 약정 디스크·트래픽(MB)·기간·트래픽 사용량(수동 입력)
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_contract_disk_mb INTEGER;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_contract_traffic_mb INTEGER;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_contract_start DATE;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_contract_end DATE;
ALTER TABLE tb_hq_api_config ADD COLUMN IF NOT EXISTS server_manage_traffic_used_mb INTEGER;
