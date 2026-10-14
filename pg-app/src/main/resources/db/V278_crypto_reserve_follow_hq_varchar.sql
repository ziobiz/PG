-- V278: reserve_follow_hq CHAR → VARCHAR(1) (Hibernate validate)
SET client_min_messages TO WARNING;

ALTER TABLE tb_merchant_crypto_config
    ALTER COLUMN reserve_follow_hq TYPE VARCHAR(1)
    USING TRIM(BOTH FROM reserve_follow_hq::text);
