CREATE TABLE IF NOT EXISTS tb_hq_notify_target (
    id BIGSERIAL PRIMARY KEY,
    target_code VARCHAR(50) NOT NULL UNIQUE,
    target_name VARCHAR(100) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    use_yn VARCHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP
);

