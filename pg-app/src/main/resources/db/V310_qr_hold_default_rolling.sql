-- QR 보증금(롤링) 본사 기본: DEFAULT_QR → 10% / 180일
-- (카드 FIAT 보류율·CRYPTO 보증금과 분리. 가맹은 QR 카드에서 본사따름/직접입력)

UPDATE tb_commission_policy
SET rolling_pct = 10,
    rolling_days = 180,
    updated_at = NOW()
WHERE scope = 'DEFAULT_QR';

UPDATE tb_commission_policy
SET rolling_pct = COALESCE(rolling_pct, 10),
    rolling_days = COALESCE(NULLIF(rolling_days, 0), 180),
    updated_at = NOW()
WHERE UPPER(COALESCE(fee_channel, '')) = 'QR'
  AND scope <> 'DEFAULT_QR'
  AND (rolling_pct IS NULL OR rolling_days IS NULL OR rolling_days = 0);
