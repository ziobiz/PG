-- VIEW SETTING 본사 추가 항목 (화면별 커스텀 열 키·표시명)
CREATE TABLE IF NOT EXISTS tb_hq_view_custom_column (
    id              BIGSERIAL PRIMARY KEY,
    page_url        VARCHAR(256) NOT NULL,
    column_key      VARCHAR(80) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_hq_view_custom_col_url_key UNIQUE (page_url, column_key)
);

CREATE INDEX IF NOT EXISTS idx_hq_view_custom_col_page ON tb_hq_view_custom_column(page_url);
