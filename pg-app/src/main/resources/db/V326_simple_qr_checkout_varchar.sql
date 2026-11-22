-- Hibernate validate: yn 컬럼은 VARCHAR(1) (CHAR/bpchar 불일치 수정)
ALTER TABLE tb_hq_api_config
    ALTER COLUMN api_broker_simple_qr_enabled_yn TYPE VARCHAR(1);

ALTER TABLE tb_merchant_profile
    ALTER COLUMN simple_qr_checkout_use_yn TYPE VARCHAR(1);
