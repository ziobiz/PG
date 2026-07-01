-- NOTI Provision 실행 이력 (운영관리 노티생성 목록)
-- PostgreSQL (기본 datasource). MySQL 전용 문법(AUTO_INCREMENT 등) 사용하지 않음.
CREATE TABLE IF NOT EXISTS tb_noti_provision_log (
    id                  BIGSERIAL PRIMARY KEY,
    org_unit_id         BIGINT       NOT NULL,
    comp_id             VARCHAR(50)  NOT NULL,
    comp_nm             VARCHAR(200),
    internal_target_id  VARCHAR(120),
    route_no            VARCHAR(32),
    slot_no             INT,
    jpay_notify_url     VARCHAR(2048),
    jpay_callback_url   VARCHAR(2048),
    created_flag        VARCHAR(1)   DEFAULT 'Y',
    provisioned_by      VARCHAR(64),
    provisioned_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_npl_comp_id ON tb_noti_provision_log (comp_id);
CREATE INDEX IF NOT EXISTS idx_npl_provisioned_at ON tb_noti_provision_log (provisioned_at DESC);

COMMENT ON TABLE tb_noti_provision_log IS 'NOTI Provision 실행 이력(운영관리 노티생성 목록)';
COMMENT ON COLUMN tb_noti_provision_log.created_flag IS 'Y=NOTI 신규, N=기존 동일';
