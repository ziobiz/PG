-- 분할결제 계약취소: 가맹 FOLLOW_HQ 시 본사 기본을 사용(Y)으로 맞춤 (미사용이면 가맹 화면에서 취소 버튼 미노출)

UPDATE tb_hq_api_config
SET split_pay_contract_cancel_default_yn = 'Y'
WHERE COALESCE(NULLIF(TRIM(split_pay_contract_cancel_default_yn), ''), 'N') = 'N';

COMMENT ON COLUMN tb_hq_api_config.split_pay_contract_cancel_default_yn IS
    'URL 분할결제 계약취소 — 가맹 FOLLOW_HQ 시 기본 부여(Y/N). 기본 Y(가맹이 본인 계약 취소 가능)';
