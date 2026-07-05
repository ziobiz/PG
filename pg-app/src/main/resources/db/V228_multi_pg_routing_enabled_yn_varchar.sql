-- V228: multi_pg_routing_enabled_yn CHAR(1)→VARCHAR(1) (Schema-validation bpchar 오류 복구)
ALTER TABLE tb_hq_api_config
    ALTER COLUMN multi_pg_routing_enabled_yn TYPE VARCHAR(1);
