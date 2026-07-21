-- 노티 생성 이력: 연동방식(API/URL) 저장
ALTER TABLE tb_noti_provision_log
    ADD COLUMN IF NOT EXISTS integration_mode VARCHAR(8);

UPDATE tb_noti_provision_log
SET integration_mode = 'API'
WHERE integration_mode IS NULL OR TRIM(integration_mode) = '';
