-- V225: 멀티 결제대행사(카드브랜드·통화) 라우팅 본사 마스터 스위치
ALTER TABLE tb_hq_api_config
    ADD COLUMN IF NOT EXISTS multi_pg_routing_enabled_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

COMMENT ON COLUMN tb_hq_api_config.multi_pg_routing_enabled_yn IS
    'Y=카드브랜드·통화 기준 멀티 PG 라우팅 사용(가맹 결제대행사 card_brand_scope 적용). N=단일 운영 PG(기존).';
