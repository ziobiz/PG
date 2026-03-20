-- PostgreSQL 등 영구 DB용 (H2 dev: ddl-auto 로 엔티티 반영)

CREATE TABLE IF NOT EXISTS tb_user_view_setting (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    page_url VARCHAR(200) NOT NULL,
    selected_keys_json TEXT NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_user_view_setting UNIQUE (username, page_url)
);
