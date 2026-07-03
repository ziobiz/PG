-- JPAY 사후 고위험·PY0124 자동 쿨다운 on/off (본사 리스크설정)

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS postsale_cooldown_jpay_highrisk_yn VARCHAR(1) NOT NULL DEFAULT 'Y';

ALTER TABLE tb_hq_risk_card_policy
    ADD COLUMN IF NOT EXISTS postsale_cooldown_jpay_py0124_yn VARCHAR(1) NOT NULL DEFAULT 'Y';
