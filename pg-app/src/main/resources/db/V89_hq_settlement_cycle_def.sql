-- 본사설정 > 정산관리설정: 정산주기 코드·표시명·설명·사용여부(가맹 정산주기 셀렉트·자동정산 해석과 연동)
CREATE TABLE IF NOT EXISTS tb_hq_settlement_cycle_def (
    id              BIGSERIAL PRIMARY KEY,
    cycle_code      VARCHAR(64)  NOT NULL,
    display_label   VARCHAR(128),
    description     TEXT,
    sort_order      INT          NOT NULL DEFAULT 0,
    active_yn       VARCHAR(1)   NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_hq_settlement_cycle_code UNIQUE (cycle_code)
);
CREATE INDEX IF NOT EXISTS idx_hq_settlement_cycle_def_sort ON tb_hq_settlement_cycle_def (sort_order, cycle_code);
