-- 리스크 필터링: 카드·이메일·IP 속도제한 창/횟수 분리
-- 기본값(운영 권장): 카드 10분/3회, 이메일 30분/5회, IP 15분/10회

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS velocity_card_window_minutes INT NOT NULL DEFAULT 10;

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS velocity_card_max_attempts INT NOT NULL DEFAULT 3;

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS velocity_email_window_minutes INT NOT NULL DEFAULT 30;

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS velocity_email_max_attempts INT NOT NULL DEFAULT 5;

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS velocity_ip_window_minutes INT NOT NULL DEFAULT 15;

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS velocity_ip_max_attempts INT NOT NULL DEFAULT 10;

-- 기존 통합값 → 카드 채널로 이관(이미 운영 중이면 카드에 반영)
UPDATE tb_hq_risk_card_policy
SET velocity_card_window_minutes = GREATEST(1, COALESCE(velocity_window_minutes, 10)),
    velocity_card_max_attempts = GREATEST(1, COALESCE(velocity_max_attempts, 3))
WHERE id = 1;

COMMENT ON COLUMN tb_hq_risk_card_policy.velocity_card_window_minutes IS '동일 카드 속도제한 창(분) 기본 10';
COMMENT ON COLUMN tb_hq_risk_card_policy.velocity_card_max_attempts IS '동일 카드 속도제한 횟수 기본 3';
COMMENT ON COLUMN tb_hq_risk_card_policy.velocity_email_window_minutes IS '동일 이메일 속도제한 창(분) 기본 30';
COMMENT ON COLUMN tb_hq_risk_card_policy.velocity_email_max_attempts IS '동일 이메일 속도제한 횟수 기본 5';
COMMENT ON COLUMN tb_hq_risk_card_policy.velocity_ip_window_minutes IS '동일 IP 속도제한 창(분) 기본 15';
COMMENT ON COLUMN tb_hq_risk_card_policy.velocity_ip_max_attempts IS '동일 IP 속도제한 횟수 기본 10';
