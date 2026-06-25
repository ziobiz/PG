-- 가맹점 리스크 정책 기본값: 미사용(DISABLED)
ALTER TABLE tb_merchant_profile
    ALTER COLUMN card_risk_policy_mode SET DEFAULT 'DISABLED';
