-- 신규 가맹 기본: API 리다이렉트 prepare 허용 ON (기존 행 값은 유지)
ALTER TABLE tb_merchant_profile
  ALTER COLUMN api_broker_redirect_use_yn SET DEFAULT 'Y';
