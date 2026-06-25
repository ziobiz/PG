-- V186: 결제내역 처리사유 AI 번역 캐시
CREATE TABLE IF NOT EXISTS tb_outcome_reason_translation_cache (
    id BIGSERIAL PRIMARY KEY,
    cache_key VARCHAR(64) NOT NULL,
    source_text TEXT NOT NULL,
    target_locale VARCHAR(8) NOT NULL,
    translated_text TEXT NOT NULL,
    provider_used VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_outcome_reason_translation_cache_key UNIQUE (cache_key)
);

CREATE INDEX IF NOT EXISTS idx_outcome_reason_translation_cache_locale
    ON tb_outcome_reason_translation_cache (target_locale);
