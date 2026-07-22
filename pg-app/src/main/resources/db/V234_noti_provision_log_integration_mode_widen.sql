-- 노티 생성 이력 연동방식: URL_HYBRID 저장을 위해 컬럼 확장
ALTER TABLE tb_noti_provision_log
    ALTER COLUMN integration_mode TYPE VARCHAR(16);

UPDATE tb_noti_provision_log
SET integration_mode = 'API'
WHERE integration_mode IS NULL OR TRIM(integration_mode) = '';
