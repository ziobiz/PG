-- 리스크 자동등록 추적기간(본사·가맹) + 카드별 비성공 이벤트 이력

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS track_period_mode VARCHAR(8) NOT NULL DEFAULT 'NONE';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS track_period_value INTEGER NOT NULL DEFAULT 0;

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_track_period_policy VARCHAR(16);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_track_period_mode VARCHAR(8);

ALTER TABLE tb_merchant_profile
    ADD COLUMN IF NOT EXISTS card_risk_track_period_value INTEGER;

COMMENT ON COLUMN tb_merchant_profile.card_risk_track_period_policy IS 'NONE|FOLLOW_HQ|CUSTOM — 가맹점 추적기간 기간정책(미사용=평생, 본사정책 따름, 별도정책)';

COMMENT ON COLUMN tb_hq_risk_card_policy.track_period_mode IS 'NONE|DAY|MONTH|YEAR — 미사용 시 기간 제한 없음(성공 시까지 누적)';
COMMENT ON COLUMN tb_hq_risk_card_policy.track_period_value IS '추적기간 값(일·월·년). NONE이면 무시';

CREATE TABLE IF NOT EXISTS tb_pay_card_fail_risk_event (
    id           BIGSERIAL PRIMARY KEY,
    pg_vendor    VARCHAR(16) NOT NULL,
    pan_hash     VARCHAR(64) NOT NULL,
    org_unit_id  BIGINT,
    outcome_code VARCHAR(32),
    occurred_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pay_card_fail_risk_event_lookup
    ON tb_pay_card_fail_risk_event (pg_vendor, pan_hash, COALESCE(org_unit_id, 0), occurred_at DESC);
