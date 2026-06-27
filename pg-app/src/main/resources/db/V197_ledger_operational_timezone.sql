-- 전산설정: 운영 시간대(그리드 1줄) — 표준 시간대(2줄·ICOPAY 벽시계)와 별도
ALTER TABLE tb_hq_ledger_sys_settings
    ADD COLUMN IF NOT EXISTS operational_timezone VARCHAR(64);

UPDATE tb_hq_ledger_sys_settings
SET operational_timezone = 'Asia/Tokyo'
WHERE operational_timezone IS NULL OR BTRIM(operational_timezone) = '';

COMMENT ON COLUMN tb_hq_ledger_sys_settings.operational_timezone IS '운영 시간대(IANA). 결제·정산 그리드 거래시간 1줄 표시. 미설정 시 Asia/Tokyo';
