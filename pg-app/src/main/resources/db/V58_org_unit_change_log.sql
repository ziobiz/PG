-- 업체(조직) 정보 필드별 변경 이력 (업체변경이력 화면)
CREATE TABLE IF NOT EXISTS tb_org_unit_change_log (
    id              BIGSERIAL PRIMARY KEY,
    org_unit_id     BIGINT       NOT NULL,
    comp_id         VARCHAR(50)  NOT NULL,
    comp_nm         VARCHAR(200),
    field_label     VARCHAR(200) NOT NULL,
    value_before    TEXT,
    value_after     TEXT,
    changed_by      VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_org_unit_chg_log_created ON tb_org_unit_change_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_org_unit_chg_log_comp_id ON tb_org_unit_change_log (comp_id);
CREATE INDEX IF NOT EXISTS idx_org_unit_chg_log_org_unit ON tb_org_unit_change_log (org_unit_id);
