-- V159가 CHAR(1)로 생성된 운영 DB → JPA validate(bpchar vs varchar) 오류 수정
-- 운영 PostgreSQL 1회 실행.

ALTER TABLE tb_hq_pay_card_blacklist
    ALTER COLUMN active_yn TYPE VARCHAR(1) USING TRIM(active_yn::text);

ALTER TABLE tb_hq_pay_card_block_prefix
    ALTER COLUMN active_yn TYPE VARCHAR(1) USING TRIM(active_yn::text);
