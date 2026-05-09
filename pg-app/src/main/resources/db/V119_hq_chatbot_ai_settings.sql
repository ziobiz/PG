-- 본사설정 AI (챗봇·상품 안내 등) — ziobiz/Stock php-web/pages/ai.php 리포트 API 키·모델·프로바이더 순위 호환 필드를 JSON(tb_hq_chatbot_ai_settings.config_json)에 저장
CREATE TABLE IF NOT EXISTS tb_hq_chatbot_ai_settings (
    id            BIGINT PRIMARY KEY,
    config_json   TEXT NOT NULL DEFAULT '{}',
    created_at    TIMESTAMP WITHOUT TIME ZONE,
    updated_at    TIMESTAMP WITHOUT TIME ZONE
);

INSERT INTO tb_hq_chatbot_ai_settings (id, config_json, created_at, updated_at)
SELECT 1, '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_hq_chatbot_ai_settings WHERE id = 1);
