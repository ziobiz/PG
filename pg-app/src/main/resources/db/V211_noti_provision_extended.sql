-- NOTI Provision 확장: 화폐별 전산 대상, DEALMAI 웹훅 파트너 목록
-- PostgreSQL (기본 datasource). MySQL 전용 문법(AUTO_INCREMENT 등) 사용하지 않음.
ALTER TABLE tb_hq_notify_env_config
    ADD COLUMN IF NOT EXISTS noti_provision_internal_target_jpy VARCHAR(120),
    ADD COLUMN IF NOT EXISTS noti_provision_internal_target_usd VARCHAR(120),
    ADD COLUMN IF NOT EXISTS noti_provision_default_dealmai_partner VARCHAR(64);

CREATE TABLE IF NOT EXISTS tb_hq_noti_webhook_partner (
    id             BIGSERIAL PRIMARY KEY,
    partner_code   VARCHAR(64)  NOT NULL,
    partner_label  VARCHAR(200),
    sort_order     INT          DEFAULT 0,
    use_yn         VARCHAR(1)   DEFAULT 'Y',
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hnwp_partner_code UNIQUE (partner_code)
);

COMMENT ON TABLE tb_hq_noti_webhook_partner IS '본사설정 노티웹훅구성 — DEALMAI Partner 목록';
