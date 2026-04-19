-- 총판별 결제내역·통합내역 등 거래시간 1줄 표시 프리셋(KR/JP/USA/TH/SG/HK/CH). 2줄은 정산 크론 Zone(기존 settlement_cron_zone_id) 자동.
ALTER TABLE tb_master_dist_settlement_cycle_config
    ADD COLUMN IF NOT EXISTS txn_time_display_preset VARCHAR(16);
COMMENT ON COLUMN tb_master_dist_settlement_cycle_config.txn_time_display_preset IS
    '거래시간 1줄 표시: KR,JP,USA,TH,SG,HK,CH. NULL이면 JP로 간주.';
